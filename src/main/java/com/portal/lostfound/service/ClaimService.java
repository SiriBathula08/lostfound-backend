package com.portal.lostfound.service;

import com.portal.lostfound.dto.ClaimRequest;
import com.portal.lostfound.model.Claim;
import com.portal.lostfound.model.Item;
import com.portal.lostfound.model.User;
import com.portal.lostfound.repository.ClaimRepository;
import com.portal.lostfound.util.MatchingAlgorithm;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ClaimService {

    private static final Logger logger = LoggerFactory.getLogger(ClaimService.class);

    private final ClaimRepository claimRepository;
    private final ItemService itemService;
    private final AuthService authService;
    private final MatchingAlgorithm matchingAlgorithm;
    private final NotificationService notificationService;

    @Autowired
    public ClaimService(ClaimRepository claimRepository,
                        ItemService itemService,
                        AuthService authService,
                        MatchingAlgorithm matchingAlgorithm,
                        NotificationService notificationService) {
        this.claimRepository     = claimRepository;
        this.itemService         = itemService;
        this.authService         = authService;
        this.matchingAlgorithm   = matchingAlgorithm;
        this.notificationService = notificationService;
    }

    // ── Submit ─────────────────────────────────────────────────

    @Transactional
    public Claim submitClaim(ClaimRequest request,
                             List<MultipartFile> proofImages,
                             String claimantEmail) {
        User claimant = authService.getCurrentUser(claimantEmail);
        Item item     = itemService.getItemById(request.getItemId());

        if (item.getPostedBy().getEmail().equals(claimantEmail)) {
            throw new IllegalStateException("You cannot claim your own item");
        }
        if (claimRepository.existsByItemIdAndClaimantId(item.getId(), claimant.getId())) {
            throw new IllegalStateException(
                    "You have already submitted a claim for this item");
        }
        if (item.getStatus() != Item.ItemStatus.ACTIVE) {
            throw new IllegalStateException("This item is no longer available for claims");
        }

        Claim claim = new Claim();
        claim.setItem(item);
        claim.setClaimant(claimant);
        claim.setDescription(request.getDescription());
        claim.setStatus(Claim.ClaimStatus.PENDING);

        double score = computeDescriptionMatchScore(request.getDescription(), item);
        claim.setMatchScore(score);

        if (proofImages != null && !proofImages.isEmpty()) {
            claim.setProofImages(saveProofImages(proofImages));
        }

        Claim saved = claimRepository.save(claim);

        notificationService.notifyClaimSubmitted(
                item.getPostedBy().getEmail(),
                item.getPostedBy().getFullName(),
                item.getTitle(),
                claimant.getFullName()
        );

        logger.info("Claim submitted [id={}] by {} on item [id={}]",
                saved.getId(), claimantEmail, item.getId());
        return saved;
    }

    // ── Read ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Claim> getClaimsForItem(Long itemId, String requesterEmail) {
        Item item = itemService.getItemById(itemId);
        if (!item.getPostedBy().getEmail().equals(requesterEmail)) {
            throw new AccessDeniedException(
                    "You do not have permission to view these claims");
        }
        return claimRepository.findByItemId(itemId);
    }

    @Transactional(readOnly = true)
    public Page<Claim> getMyClaims(String email, int page, int size) {
        User user = authService.getCurrentUser(email);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return claimRepository.findByClaimantId(user.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Claim getClaimById(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found: " + id));
    }

    // ── Review (item owner action) ─────────────────────────────

    @Transactional
    public Claim reviewClaim(Long claimId, boolean approve,
                             String adminNotes, String reviewerEmail) {
        Claim claim = getClaimById(claimId);
        Item  item  = claim.getItem();

        if (!item.getPostedBy().getEmail().equals(reviewerEmail)) {
            throw new AccessDeniedException("Only the item owner can review this claim");
        }
        if (claim.getStatus() != Claim.ClaimStatus.PENDING) {
            throw new IllegalStateException("Claim is no longer pending");
        }

        User reviewer = authService.getCurrentUser(reviewerEmail);
        claim.setStatus(approve ? Claim.ClaimStatus.APPROVED : Claim.ClaimStatus.REJECTED);
        claim.setAdminNotes(adminNotes);
        claim.setReviewedBy(reviewer.getId());
        claim.setReviewedAt(LocalDateTime.now());

        if (approve) {
            item.setStatus(Item.ItemStatus.CLAIMED);
            // Auto-reject all other pending claims for this item
            List<Claim> others = claimRepository.findByItemId(item.getId());
            for (Claim other : others) {
                if (!other.getId().equals(claimId)
                        && other.getStatus() == Claim.ClaimStatus.PENDING) {
                    other.setStatus(Claim.ClaimStatus.REJECTED);
                    other.setAdminNotes("Another claim was approved for this item.");
                    claimRepository.save(other);
                }
            }
        }

        Claim updated = claimRepository.save(claim);

        notificationService.notifyClaimDecision(
                claim.getClaimant().getEmail(),
                claim.getClaimant().getFullName(),
                item.getTitle(),
                approve
        );

        logger.info("Claim [id={}] {} by {}", claimId, claim.getStatus(), reviewerEmail);
        return updated;
    }

    // ── Withdraw ───────────────────────────────────────────────

    @Transactional
    public Claim withdrawClaim(Long claimId, String email) {
        Claim claim = getClaimById(claimId);
        if (!claim.getClaimant().getEmail().equals(email)) {
            throw new AccessDeniedException("You cannot withdraw someone else's claim");
        }
        if (claim.getStatus() != Claim.ClaimStatus.PENDING) {
            throw new IllegalStateException("Only pending claims can be withdrawn");
        }
        claim.setStatus(Claim.ClaimStatus.WITHDRAWN);
        return claimRepository.save(claim);
    }

    // ── Admin ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Claim> getAllClaims(Claim.ClaimStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null) {
            return claimRepository.findByStatus(status, pageable);
        }
        return claimRepository.findAll(pageable);
    }

    // ── Private helpers ────────────────────────────────────────

    private double computeDescriptionMatchScore(String claimDesc, Item item) {
        Item synthetic = new Item();
        synthetic.setTitle(claimDesc);
        synthetic.setDescription(claimDesc);
        synthetic.setCategory(item.getCategory());
        synthetic.setDateOccurred(item.getDateOccurred());
        synthetic.setLocationLat(item.getLocationLat());
        synthetic.setLocationLng(item.getLocationLng());

        Item lost  = (item.getType() == Item.ItemType.LOST)  ? item : synthetic;
        Item found = (item.getType() == Item.ItemType.FOUND) ? item : synthetic;
        return matchingAlgorithm.calculateMatchScore(lost, found);
    }

    private String saveProofImages(List<MultipartFile> files) {
        Path dir = Paths.get("uploads/claims/");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create proof-image directory", e);
        }

        List<String> saved = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            try {
                String name = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Files.copy(file.getInputStream(), dir.resolve(name),
                        StandardCopyOption.REPLACE_EXISTING);
                saved.add("claims/" + name);
            } catch (IOException e) {
                logger.error("Failed to save proof image: {}", e.getMessage());
            }
        }
        return String.join(",", saved);
    }
}
