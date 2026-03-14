package com.portal.lostfound.controller;

import com.portal.lostfound.dto.ApiResponse;
import com.portal.lostfound.model.Claim;
import com.portal.lostfound.model.Item;
import com.portal.lostfound.model.User;
import com.portal.lostfound.repository.UserRepository;
import com.portal.lostfound.service.ClaimService;
import com.portal.lostfound.service.ItemService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final ItemService itemService;
    private final ClaimService claimService;

    @Autowired
    public AdminController(UserRepository userRepository,
                           ItemService itemService,
                           ClaimService claimService) {
        this.userRepository = userRepository;
        this.itemService    = itemService;
        this.claimService   = claimService;
    }

    // ── Dashboard ──────────────────────────────────────────────

    /** GET /api/admin/stats */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        Map<String, Long> itemStats = itemService.getStats();
        long pendingClaims = claimService
                .getAllClaims(Claim.ClaimStatus.PENDING, 0, 1)
                .getTotalElements();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers",      userRepository.count());
        stats.put("activeUsers",     userRepository.countActiveUsers());
        stats.put("totalLostItems",  itemStats.get("totalLost"));
        stats.put("totalFoundItems", itemStats.get("totalFound"));
        stats.put("totalItems",      itemStats.get("total"));
        stats.put("pendingClaims",   pendingClaims);

        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    // ── User management ────────────────────────────────────────

    /** GET /api/admin/users */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<User>>> getAllUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<User> users = userRepository.findAll(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    /** PATCH /api/admin/users/{id}/toggle */
    @PatchMapping("/users/{id}/toggle")
    public ResponseEntity<ApiResponse<String>> toggleUserActive(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        user.setActive(!user.isActive());
        userRepository.save(user);
        String status = user.isActive() ? "activated" : "deactivated";
        return ResponseEntity.ok(ApiResponse.ok("User account " + status));
    }

    /** PATCH /api/admin/users/{id}/role */
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<String>> changeRole(
            @PathVariable Long id,
            @RequestParam User.Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        user.setRole(role);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Role updated to " + role));
    }

    // ── Item management ────────────────────────────────────────

    /** GET /api/admin/items */
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<Page<Item>>> getAllItems(
            @RequestParam(required = false) Item.ItemType type,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                itemService.getItems(type, null, page, size, "createdAt")));
    }

    /** PATCH /api/admin/items/{id}/status */
    @PatchMapping("/items/{id}/status")
    public ResponseEntity<ApiResponse<Item>> updateItemStatus(
            @PathVariable Long id,
            @RequestParam Item.ItemStatus status) {
        Item updated = itemService.adminUpdateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Item status updated", updated));
    }

    // ── Claim management ───────────────────────────────────────

    /** GET /api/admin/claims */
    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<Page<Claim>>> getAllClaims(
            @RequestParam(required = false) Claim.ClaimStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                claimService.getAllClaims(status, page, size)));
    }
}
