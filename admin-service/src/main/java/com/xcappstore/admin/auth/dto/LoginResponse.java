package com.xcappstore.admin.auth.dto;

public class LoginResponse {
    private final String accessToken;
    private final String tokenType;
    private final Long expiresIn;
    private final Long expiresAt;
    private final AdminUserResponse user;

    public LoginResponse(String accessToken, String tokenType, Long expiresIn, Long expiresAt, AdminUserResponse user) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public AdminUserResponse getUser() {
        return user;
    }
}
