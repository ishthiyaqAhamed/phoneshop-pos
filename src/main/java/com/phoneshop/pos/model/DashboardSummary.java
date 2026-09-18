package com.phoneshop.pos.model;

import java.util.HashMap;
import java.util.Map;

public class DashboardSummary {
    private double todayRevenue;
    private double todayProfit;
    private int todayOrdersCount;
    private double monthRevenue;
    private double monthProfit;
    private int monthOrdersCount;
    private int totalProductsCount;
    private int lowStockCount;
    private int outOfStockCount;
    private double inventoryTotalValue;
    private double inventoryTotalCost;
    private Map<String, Integer> topSellingProducts = new HashMap<>();

    public double getTodayRevenue() {
        return todayRevenue;
    }

    public void setTodayRevenue(double todayRevenue) {
        this.todayRevenue = todayRevenue;
    }

    public double getTodayProfit() {
        return todayProfit;
    }

    public void setTodayProfit(double todayProfit) {
        this.todayProfit = todayProfit;
    }

    public int getTodayOrdersCount() {
        return todayOrdersCount;
    }

    public void setTodayOrdersCount(int todayOrdersCount) {
        this.todayOrdersCount = todayOrdersCount;
    }

    public double getMonthRevenue() {
        return monthRevenue;
    }

    public void setMonthRevenue(double monthRevenue) {
        this.monthRevenue = monthRevenue;
    }

    public double getMonthProfit() {
        return monthProfit;
    }

    public void setMonthProfit(double monthProfit) {
        this.monthProfit = monthProfit;
    }

    public int getMonthOrdersCount() {
        return monthOrdersCount;
    }

    public void setMonthOrdersCount(int monthOrdersCount) {
        this.monthOrdersCount = monthOrdersCount;
    }

    public int getTotalProductsCount() {
        return totalProductsCount;
    }

    public void setTotalProductsCount(int totalProductsCount) {
        this.totalProductsCount = totalProductsCount;
    }

    public int getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(int lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public int getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(int outOfStockCount) {
        this.outOfStockCount = outOfStockCount;
    }

    public double getInventoryTotalValue() {
        return inventoryTotalValue;
    }

    public void setInventoryTotalValue(double inventoryTotalValue) {
        this.inventoryTotalValue = inventoryTotalValue;
    }

    public double getInventoryTotalCost() {
        return inventoryTotalCost;
    }

    public void setInventoryTotalCost(double inventoryTotalCost) {
        this.inventoryTotalCost = inventoryTotalCost;
    }

    public Map<String, Integer> getTopSellingProducts() {
        return topSellingProducts;
    }

    public void setTopSellingProducts(Map<String, Integer> topSellingProducts) {
        this.topSellingProducts = topSellingProducts;
    }
}
