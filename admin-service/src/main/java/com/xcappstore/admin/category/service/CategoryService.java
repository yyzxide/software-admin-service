package com.xcappstore.admin.category.service;

import com.xcappstore.admin.category.dto.CategoryCreateRequest;
import com.xcappstore.admin.category.dto.CategoryQueryRequest;
import com.xcappstore.admin.category.dto.CategoryResponse;
import com.xcappstore.admin.category.dto.CategoryUpdateRequest;
import java.util.List;

public interface CategoryService {
    Long create(CategoryCreateRequest request);

    CategoryResponse detail(Long id);

    List<CategoryResponse> list(CategoryQueryRequest request);

    List<CategoryResponse> tree(Integer status);

    void update(Long id, CategoryUpdateRequest request);

    CategoryResponse toggleStatus(Long id);

    void delete(Long id);
}
