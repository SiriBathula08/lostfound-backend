package com.portal.lostfound.controller;

import com.portal.lostfound.dto.ApiResponse;
import com.portal.lostfound.dto.ItemRequest;
import com.portal.lostfound.model.Item;
import com.portal.lostfound.service.ItemService;
import com.portal.lostfound.service.MatchingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;
    private final MatchingService matchingService;

    @Autowired
    public ItemController(ItemService itemService, MatchingService matchingService) {
        this.itemService     = itemService;
        this.matchingService = matchingService;
    }

    // ── Public endpoints ───────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Item>>> getItems(
            @RequestParam(required = false) Item.ItemType type,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "10")        int size,
            @RequestParam(defaultValue = "createdAt") String sort) {
        return ResponseEntity.ok(ApiResponse.ok(
                itemService.getItems(type, category, page, size, sort)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<Item>>> searchItems(
            @RequestParam String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(itemService.searchItems(q, page, size)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(itemService.getStats()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Item>> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(itemService.getItemById(id)));
    }

    // ── Authenticated endpoints ────────────────────────────────

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<Page<Item>>> myItems(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                itemService.getUserItems(userDetails.getUsername(), page, size)));
    }

    @GetMapping("/{id}/matches")
    public ResponseEntity<ApiResponse<List<MatchingService.MatchResult>>> getMatches(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit) {
        Item item = itemService.getItemById(id);
        return ResponseEntity.ok(ApiResponse.ok(matchingService.getTopMatches(item, limit)));
    }

    /**
     * POST /api/items — plain JSON body, no multipart needed.
     * Send: Content-Type: application/json + Authorization: Bearer <token>
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Item>> createItem(
            @Valid @RequestBody ItemRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Item created = itemService.createItem(request, null, userDetails.getUsername());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Item posted successfully", created));
    }

    /**
     * POST /api/items/{id}/images — upload images separately after creating item.
     */
    @PostMapping("/{id}/images")
    public ResponseEntity<ApiResponse<Item>> uploadImages(
            @PathVariable Long id,
            @RequestParam("images") List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {
        Item updated = itemService.uploadImages(id, images, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Images uploaded", updated));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Item>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody ItemRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Item updated = itemService.updateItem(id, request, null, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Item updated", updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Item>> updateStatus(
            @PathVariable Long id,
            @RequestParam Item.ItemStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {
        Item updated = itemService.updateStatus(id, status, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Status updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        itemService.deleteItem(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Item deleted", null));
    }
}
