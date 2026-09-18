package com.phoneshop.pos.model;

public class SaleItem {
    private int id;
    private int saleId;
    private int productId;
    private String productName;
    private String brand;
    private String category;
    private String imei;
    private double unitCost;
    private double unitPrice;
    private int quantity;
    private double discountAmount;
    private double subtotal;
    private String warrantyPeriod;

    public SaleItem() {
        this.quantity = 1;
        this.discountAmount = 0.0;
    }

    public SaleItem(Product product, int quantity) {
        this.productId = product.getId();
        this.productName = product.getFullDisplayName();
        this.brand = product.getBrand();
        this.category = product.getCategory();
        this.imei = product.getImei();
        this.unitCost = product.getCostPrice();
        this.unitPrice = product.getSellingPrice();
        this.quantity = quantity;
        this.discountAmount = 0.0;
        this.warrantyPeriod = product.getWarrantyPeriod();
        this.recalculateSubtotal();
    }

    public void recalculateSubtotal() {
        this.subtotal = (this.unitPrice * this.quantity) - this.discountAmount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
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

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public double getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(double unitCost) {
        this.unitCost = unitCost;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        recalculateSubtotal();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        recalculateSubtotal();
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
        recalculateSubtotal();
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public String getWarrantyPeriod() {
        return warrantyPeriod;
    }

    public void setWarrantyPeriod(String warrantyPeriod) {
        this.warrantyPeriod = warrantyPeriod;
    }

    public double getProfit() {
        return subtotal - (unitCost * quantity);
    }
}
