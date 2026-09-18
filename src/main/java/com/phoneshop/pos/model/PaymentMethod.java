package com.phoneshop.pos.model;

public enum PaymentMethod {
    CASH("Cash"),
    CARD("Card / POS Machine"),
    BANK_TRANSFER("Bank Transfer / QR"),
    SPLIT("Split Payment");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PaymentMethod fromString(String str) {
        if (str == null) return CASH;
        try {
            return PaymentMethod.valueOf(str.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CASH;
        }
    }
}
