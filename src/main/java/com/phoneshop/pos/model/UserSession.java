package com.phoneshop.pos.model;

import java.time.Duration;
import java.time.LocalDateTime;

public class UserSession {
    private int id;
    private int userId;
    private String username;
    private String fullName;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private LocalDateTime lastActiveTime;
    private String status; // "ACTIVE", "LOGGED_OUT"
    private double shiftSalesTotal;
    private int shiftOrdersCount;

    public UserSession() {}

    public UserSession(int id, int userId, String username, String fullName, LocalDateTime loginTime,
                       LocalDateTime logoutTime, LocalDateTime lastActiveTime, String status,
                       double shiftSalesTotal, int shiftOrdersCount) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.loginTime = loginTime;
        this.logoutTime = logoutTime;
        this.lastActiveTime = lastActiveTime;
        this.status = status;
        this.shiftSalesTotal = shiftSalesTotal;
        this.shiftOrdersCount = shiftOrdersCount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }

    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(LocalDateTime lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getShiftSalesTotal() {
        return shiftSalesTotal;
    }

    public void setShiftSalesTotal(double shiftSalesTotal) {
        this.shiftSalesTotal = shiftSalesTotal;
    }

    public int getShiftOrdersCount() {
        return shiftOrdersCount;
    }

    public void setShiftOrdersCount(int shiftOrdersCount) {
        this.shiftOrdersCount = shiftOrdersCount;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public String getDurationFormatted() {
        if (loginTime == null) return "0m";
        LocalDateTime end = logoutTime != null ? logoutTime : LocalDateTime.now();
        Duration duration = Duration.between(loginTime, end);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }
}
