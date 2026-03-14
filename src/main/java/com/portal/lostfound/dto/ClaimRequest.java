package com.portal.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ClaimRequest {

    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotBlank(message = "Description / proof is required")
    private String description;

    // ── Constructors ───────────────────────────────────────────
    public ClaimRequest() {}

    public ClaimRequest(Long itemId, String description) {
        this.itemId = itemId;
        this.description = description;
    }

    // ── Getters & Setters ──────────────────────────────────────
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
