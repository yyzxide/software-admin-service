package com.xcappstore.admin.auth.rbac.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminUserManageResponse {
    private Long id;
    private String username;
    private String displayName;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AdminRoleResponse> roles = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<AdminRoleResponse> getRoles() {
        return roles;
    }

    public void setRoles(List<AdminRoleResponse> roles) {
        this.roles = roles == null ? new ArrayList<>() : roles;
    }
}
