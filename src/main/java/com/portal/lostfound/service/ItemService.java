package com.portal.lostfound.service;

import com.portal.lostfound.dto.ItemRequest;
import com.portal.lostfound.model.Item;
import com.portal.lostfound.model.User;
import com.portal.lostfound.repository.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ItemService {

    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    private final ItemRepository itemRepository;
    private final AuthService authService;
    private final MatchingService matchingService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Autowired
    public ItemService(ItemRepository itemRepository,
                       AuthService authService,
                       MatchingService matchingService) {
        this.itemRepository  = itemRepository;
        this.authService     = authService;
        this.matchingService = matchingService;
    }

    // ── Create ─────────────────────────────────────────────────

    @Transactional
    public Item createItem(ItemRequest request, List<MultipartFile> images, String userEmail) {
        User user = authService.getCurrentUser(userEmail);

        Item item = new Item();
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategory(request.getCategory());
        item.setSubCategory(request.getSubCategory());
        item.setType(request.getType());
        item.setLocationName(request.getLocationName());
        item.setLocationLat(request.getLocationLat());
        item.setLocationLng(request.getLocationLng());
        item.setDateOccurred(request.getDateOccurred());
        item.setColor(request.getColor());
        item.setBrand(request.getBrand());
        item.setModel(request.getModel());
        item.setUniqueIdentifiers(request.getUniqueIdentifiers());
        item.setRewardAmount(request.getRewardAmount());
        item.setPostedBy(user);
        item.setStatus(Item.ItemStatus.ACTIVE);

        if (images != null && !images.isEmpty()) {
            item.setImageUrls(saveImages(images));
        }

        Item saved = itemRepository.save(item);
        logger.info("Item created [id={}, type={}] by {}", saved.getId(), saved.getType(), userEmail);

        // Trigger async matching after save
        matchingService.findAndNotifyMatches(saved);

        return saved;
    }

    // ── Read ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Item> getItems(Item.ItemType type, String category,
                               int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sort));

        if (type != null && category != null) {
            return itemRepository.findByTypeAndStatusAndCategory(
                    type, Item.ItemStatus.ACTIVE, category, pageable);
        }
        if (type != null) {
            return itemRepository.findByTypeAndStatus(type, Item.ItemStatus.ACTIVE, pageable);
        }
        return itemRepository.findByStatus(Item.ItemStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public Item getItemById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Item> searchItems(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return itemRepository.searchItems(query, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Item> getUserItems(String userEmail, int page, int size) {
        User user = authService.getCurrentUser(userEmail);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return itemRepository.findByPostedById(user.getId(), pageable);
    }

    // ── Update ─────────────────────────────────────────────────

    @Transactional
    public Item updateItem(Long id, ItemRequest request,
                           List<MultipartFile> images, String userEmail) {
        Item item = getItemById(id);
        validateOwnership(item, userEmail);

        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategory(request.getCategory());
        item.setSubCategory(request.getSubCategory());
        item.setLocationName(request.getLocationName());
        item.setLocationLat(request.getLocationLat());
        item.setLocationLng(request.getLocationLng());
        item.setDateOccurred(request.getDateOccurred());
        item.setColor(request.getColor());
        item.setBrand(request.getBrand());
        item.setModel(request.getModel());
        item.setUniqueIdentifiers(request.getUniqueIdentifiers());
        item.setRewardAmount(request.getRewardAmount());

        if (images != null && !images.isEmpty()) {
            item.setImageUrls(saveImages(images));
        }

        return itemRepository.save(item);
    }

    @Transactional
    public Item updateStatus(Long id, Item.ItemStatus status, String userEmail) {
        Item item = getItemById(id);
        validateOwnership(item, userEmail);
        item.setStatus(status);
        return itemRepository.save(item);
    }

    // ── Delete ─────────────────────────────────────────────────

    @Transactional
    public void deleteItem(Long id, String userEmail) {
        Item item = getItemById(id);
        validateOwnership(item, userEmail);
        itemRepository.delete(item);
        logger.info("Item deleted [id={}] by {}", id, userEmail);
    }

    // ── Upload images separately ───────────────────────────────

    @Transactional
    public Item uploadImages(Long id, List<MultipartFile> images, String userEmail) {
        Item item = getItemById(id);
        validateOwnership(item, userEmail);
        if (images != null && !images.isEmpty()) {
            item.setImageUrls(saveImages(images));
        }
        return itemRepository.save(item);
    }

    // ── Admin helpers ──────────────────────────────────────────

    @Transactional
    public Item adminUpdateStatus(Long id, Item.ItemStatus status) {
        Item item = getItemById(id);
        item.setStatus(status);
        return itemRepository.save(item);
    }

    // ── Stats ──────────────────────────────────────────────────

    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalLost",  itemRepository.countByTypeAndActive(Item.ItemType.LOST));
        stats.put("totalFound", itemRepository.countByTypeAndActive(Item.ItemType.FOUND));
        stats.put("total",      itemRepository.count());
        return stats;
    }

    // ── Private helpers ────────────────────────────────────────

    private void validateOwnership(Item item, String userEmail) {
        if (!item.getPostedBy().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("You do not own this item");
        }
    }

    private String saveImages(List<MultipartFile> files) {
        Path dir = Paths.get(uploadDir);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }

        List<String> saved = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            try {
                String name = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path target = dir.resolve(name);
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                saved.add(name);
            } catch (IOException e) {
                logger.error("Failed to save image: {}", e.getMessage());
            }
        }
        return String.join(",", saved);
    }
}
