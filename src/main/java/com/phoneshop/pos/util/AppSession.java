package com.phoneshop.pos.util;

import com.phoneshop.pos.model.Role;
import com.phoneshop.pos.model.User;

public class AppSession {
    private static AppSession instance;
    private User currentUser;
    private int currentSessionId = -1;

    private AppSession() {}

    public static synchronized AppSession getInstance() {
        if (instance == null) {
            instance = new AppSession();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser, int sessionId) {
        this.currentUser = currentUser;
        this.currentSessionId = sessionId;
    }

    public int getCurrentSessionId() {
        return currentSessionId;
    }

    public void clear() {
        this.currentUser = null;
        this.currentSessionId = -1;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }

    public boolean isCashier() {
        return currentUser != null && currentUser.getRole() == Role.CASHIER;
    }
}
