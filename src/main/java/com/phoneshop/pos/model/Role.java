package com.phoneshop.pos.model;

public enum Role {
    ADMIN("Administrator"),
    CASHIER("Cashier");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Role fromString(String roleStr) {
        if (roleStr == null) return CASHIER;
        try {
            return Role.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CASHIER;
        }
    }
}
