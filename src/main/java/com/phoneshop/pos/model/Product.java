package com.phoneshop.pos.model;

import java.time.LocalDateTime;

public class Product {
    private int id;
    private String barcode;
    private String imei;
    private String name;
    private String brand;
    private String category;
    private String storageRam;
    private String color;
    private String condition;
    private double costPrice;
    private double sellingPrice;
    private int stockQuantity;
    private int minStockLevel;
    private String warrantyPeriod;
    private String description;
    private LocalDateTime updatedAt;

    public Product() {
        this.condition = "BRAND_NEW";
        this.warrantyPeriod = "1 Year Warranty";
        this.minStockLevel = 3;
    }

    public Product(int id, String barcode, String imei, String name, String brand, String category,
                   String storageRam, String color, String condition, double costPrice,
                   double sellingPrice, int stockQuantity, int minStockLevel, String warrantyPeriod,
                   String description, LocalDateTime updatedAt) {
        this.id = id;
        this.barcode = barcode;
        this.imei = imei;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.storageRam = storageRam;
        this.color = color;
        this.condition = condition;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.stockQuantity = stockQuantity;
        this.minStockLevel = minStockLevel;
        this.warrantyPeriod = warrantyPeriod;
        this.description = description;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStorageRam() {
        return storageRam;
    }

    public void setStorageRam(String storageRam) {
        this.storageRam = storageRam;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(double costPrice) {
        this.costPrice = costPrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public int getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(int minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    public String getWarrantyPeriod() {
        return warrantyPeriod;
    }

    public void setWarrantyPeriod(String warrantyPeriod) {
        this.warrantyPeriod = warrantyPeriod;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isLowStock() {
        return stockQuantity <= minStockLevel && stockQuantity > 0;
    }

    public boolean isOutOfStock() {
        return stockQuantity <= 0;
    }

    public String getFullDisplayName() {
        StringBuilder sb = new StringBuilder(name != null ? name : "");
        if (storageRam != null && !storageRam.isBlank()) {
            sb.append(" (").append(storageRam);
            if (color != null && !color.isBlank()) {
                sb.append(", ").append(color);
            }
            sb.append(")");
        } else if (color != null && !color.isBlank()) {
            sb.append(" (").append(color).append(")");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        String display = getFullDisplayName();
        if (barcode != null && !barcode.isBlank()) {
            return display + " [" + barcode + "]";
        }
        return display;
    }
}
