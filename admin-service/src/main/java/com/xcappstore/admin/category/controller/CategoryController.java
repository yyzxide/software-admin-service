package com.xcappstore.admin.category.controller;

import com.xcappstore.admin.auth.rbac.RequirePermission;
import com.xcappstore.admin.category.dto.CategoryCreateRequest;
import com.xcappstore.admin.category.dto.CategoryQueryRequest;
import com.xcappstore.admin.category.dto.CategoryResponse;
import com.xcappstore.admin.category.dto.CategoryUpdateRequest;
import com.xcappstore.admin.category.service.CategoryService;
import com.xcappstore.admin.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequirePermission("category:view")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list(@Valid CategoryQueryRequest request) {
        return ApiResponse.success(categoryService.list(request));
    }

    @GetMapping("/tree")
    public ApiResponse<List<CategoryResponse>> tree(
        @RequestParam(required = false) @Min(0) @Max(1) Integer status
    ) {
        return ApiResponse.success(categoryService.tree(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(categoryService.detail(id));
    }

    @PostMapping
    @RequirePermission("category:manage")
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody CategoryCreateRequest request) {
        Long categoryId = categoryService.create(request);
        return ApiResponse.success(Map.of("category_id", categoryId));
    }

    @PutMapping("/{id}")
    @RequirePermission("category:manage")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request) {
        categoryService.update(id, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/toggle-status")
    @RequirePermission("category:manage")
    public ApiResponse<CategoryResponse> toggleStatus(@PathVariable Long id) {
        return ApiResponse.success(categoryService.toggleStatus(id));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("category:manage")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success(null);
    }
}
