package com.xcappstore.admin.software.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SoftwareUpdateRequest {
    @NotBlank(message = "软件名称不能为空")
    @Size(max = 128, message = "软件名称不能超过128个字符")
    private String name;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @Size(max = 512, message = "图标地址不能超过512个字符")
    private String iconUrl;

    @NotBlank(message = "软件摘要不能为空")
    @Size(max = 512, message = "软件摘要不能超过512个字符")
    private String summary;

    @NotBlank(message = "软件描述不能为空")
    private String description;

    private String supportedOsTypes;
    private String supportedArchs;
    private String screenshots;
    private String tagIds;
    private Integer isOfficial;
    private Integer isFeatured;
    private Integer sortWeight;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategory_id(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public void setIcon_url(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSupportedOsTypes() {
        return supportedOsTypes;
    }

    public void setSupportedOsTypes(String supportedOsTypes) {
        this.supportedOsTypes = supportedOsTypes;
    }

    public void setSupported_os_types(String supportedOsTypes) {
        this.supportedOsTypes = supportedOsTypes;
    }

    public String getSupportedArchs() {
        return supportedArchs;
    }

    public void setSupportedArchs(String supportedArchs) {
        this.supportedArchs = supportedArchs;
    }

    public void setSupported_archs(String supportedArchs) {
        this.supportedArchs = supportedArchs;
    }

    public String getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(String screenshots) {
        this.screenshots = screenshots;
    }

    public String getTagIds() {
        return tagIds;
    }

    public void setTagIds(String tagIds) {
        this.tagIds = tagIds;
    }

    public void setTag_ids(String tagIds) {
        this.tagIds = tagIds;
    }

    public Integer getIsOfficial() {
        return isOfficial;
    }

    public void setIsOfficial(Integer isOfficial) {
        this.isOfficial = isOfficial;
    }

    public void setIs_official(Integer isOfficial) {
        this.isOfficial = isOfficial;
    }

    public Integer getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Integer isFeatured) {
        this.isFeatured = isFeatured;
    }

    public void setIs_featured(Integer isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Integer getSortWeight() {
        return sortWeight;
    }

    public void setSortWeight(Integer sortWeight) {
        this.sortWeight = sortWeight;
    }

    public void setSort_weight(Integer sortWeight) {
        this.sortWeight = sortWeight;
    }
}
