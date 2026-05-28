package com.xcappstore.admin.auth;

public class AdminPrincipal {
    private final Long userId;
    private final String username;
    private final String userType;
    private final Long expiresAt;

    public AdminPrincipal(Long userId, String username, String userType, Long expiresAt) {
        this.userId = userId;
        this.username = username;
        this.userType = userType;
        this.expiresAt = expiresAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getUserType() {
        return userType;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }
}
