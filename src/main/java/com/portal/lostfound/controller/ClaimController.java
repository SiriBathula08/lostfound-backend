package com.portal.lostfound.controller;

import com.portal.lostfound.dto.ApiResponse;
import com.portal.lostfound.dto.ClaimRequest;
import com.portal.lostfound.model.Claim;
import com.portal.lostfound.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;

    @Autowired
    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    /**
     * POST /api/claims
     * Send: Content-Type: application/json + Authorization: Bearer <token>
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Claim>> submitClaim(
            @Valid @RequestBody ClaimRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Claim claim = claimService.submitClaim(request, null, userDetails.getUsername());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Claim submitted successfully", claim));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<Page<Claim>>> myClaims(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                claimService.getMyClaims(userDetails.getUsername(), page, size)));
    }

    @GetMapping("/item/{itemId}")
    public ResponseEntity<ApiResponse<List<Claim>>> claimsForItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                claimService.getClaimsForItem(itemId, userDetails.getUsername())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Claim>> getClaim(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(claimService.getClaimById(id)));
    }

    @PatchMapping("/{id}/review")
    public ResponseEntity<ApiResponse<Claim>> reviewClaim(
            @PathVariable Long id,
            @RequestParam boolean approve,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserDetails userDetails) {
        Claim updated = claimService.reviewClaim(id, approve, notes, userDetails.getUsername());
        String message = approve ? "Claim approved" : "Claim rejected";
        return ResponseEntity.ok(ApiResponse.ok(message, updated));
    }

    @PatchMapping("/{id}/withdraw")
    public ResponseEntity<ApiResponse<Claim>> withdrawClaim(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Claim updated = claimService.withdrawClaim(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Claim withdrawn", updated));
    }
}
