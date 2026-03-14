package com.portal.lostfound.dto;

import com.portal.lostfound.model.Item;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class ItemRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    private String subCategory;

    @NotNull(message = "Item type (LOST or FOUND) is required")
    private Item.ItemType type;

    private String locationName;
    private Double locationLat;
    private Double locationLng;

    @NotNull(message = "Date is required")
    private LocalDate dateOccurred;

    private String color;
    private String brand;
    private String model;
    private String uniqueIdentifiers;
    private Double rewardAmount;

    // ── Constructors ───────────────────────────────────────────
    public ItemRequest() {}

    // ── Getters & Setters ──────────────────────────────────────
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public Item.ItemType getType() { return type; }
    public void setType(Item.ItemType type) { this.type = type; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public Double getLocationLat() { return locationLat; }
    public void setLocationLat(Double locationLat) { this.locationLat = locationLat; }

    public Double getLocationLng() { return locationLng; }
    public void setLocationLng(Double locationLng) { this.locationLng = locationLng; }

    public LocalDate getDateOccurred() { return dateOccurred; }
    public void setDateOccurred(LocalDate dateOccurred) { this.dateOccurred = dateOccurred; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getUniqueIdentifiers() { return uniqueIdentifiers; }
    public void setUniqueIdentifiers(String uniqueIdentifiers) { this.uniqueIdentifiers = uniqueIdentifiers; }

    public Double getRewardAmount() { return rewardAmount; }
    public void setRewardAmount(Double rewardAmount) { this.rewardAmount = rewardAmount; }
}
