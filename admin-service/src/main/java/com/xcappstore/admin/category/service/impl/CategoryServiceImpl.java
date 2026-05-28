package com.xcappstore.admin.category.service.impl;

import com.xcappstore.admin.category.dto.CategoryCreateRequest;
import com.xcappstore.admin.category.dto.CategoryQueryRequest;
import com.xcappstore.admin.category.dto.CategoryResponse;
import com.xcappstore.admin.category.dto.CategoryUpdateRequest;
import com.xcappstore.admin.category.entity.CategoryEntity;
import com.xcappstore.admin.category.mapper.CategoryMapper;
import com.xcappstore.admin.category.service.CategoryCacheService;
import com.xcappstore.admin.category.service.CategoryService;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CategoryServiceImpl implements CategoryService {
    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;

    private final CategoryMapper categoryMapper;
    private final CategoryCacheService categoryCacheService;

    public CategoryServiceImpl(CategoryMapper categoryMapper, CategoryCacheService categoryCacheService) {
        this.categoryMapper = categoryMapper;
        this.categoryCacheService = categoryCacheService;
    }

    @Override
    @Transactional
    public Long create(CategoryCreateRequest request) {
        String name = normalizeName(request.getName());
        Long parentId = normalizeParentId(request.getParentId());
        validateParent(parentId, null);
        ensureNameAvailable(name, null);

        CategoryEntity category = new CategoryEntity();
        category.setName(name);
        category.setDescription(normalizeText(request.getDescription()));
        category.setIcon(normalizeText(request.getIcon()));
        category.setParentId(parentId);
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        category.setStatus(request.getStatus() == null ? STATUS_ENABLED : request.getStatus());
        category.setIsBuiltin(0);
        LocalDateTime now = LocalDateTime.now();
        category.setCreatedAt(now);
        category.setUpdatedAt(now);

        categoryMapper.insert(category);
        categoryCacheService.invalidate();
        return category.getId();
    }

    @Override
    public CategoryResponse detail(Long id) {
        return toResponse(requireCategory(id));
    }

    @Override
    public List<CategoryResponse> list(CategoryQueryRequest request) {
        Long parentId = normalizeParentId(request.getParentId());
        return categoryMapper.selectList(parentId, request.getStatus())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public List<CategoryResponse> tree(Integer status) {
        return categoryCacheService.getTree(status).orElseGet(() -> {
            List<CategoryResponse> tree = buildTree(categoryMapper.selectAll(status));
            categoryCacheService.putTree(status, tree);
            return tree;
        });
    }

    @Override
    @Transactional
    public void update(Long id, CategoryUpdateRequest request) {
        CategoryEntity category = requireCategory(id);

        if (StringUtils.hasText(request.getName())) {
            String name = normalizeName(request.getName());
            ensureNameAvailable(name, id);
            category.setName(name);
        }
        if (request.getParentId() != null) {
            Long parentId = normalizeParentId(request.getParentId());
            validateParent(parentId, id);
            category.setParentId(parentId);
        }
        if (request.getDescription() != null) {
            category.setDescription(normalizeText(request.getDescription()));
        }
        if (request.getIcon() != null) {
            category.setIcon(normalizeText(request.getIcon()));
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.update(category);

        if (request.getStatus() != null) {
            cascadeStatus(id, request.getStatus());
        }
        categoryCacheService.invalidate();
    }

    @Override
    @Transactional
    public CategoryResponse toggleStatus(Long id) {
        CategoryEntity category = requireCategory(id);
        int newStatus = STATUS_ENABLED == category.getStatus() ? STATUS_DISABLED : STATUS_ENABLED;
        category.setStatus(newStatus);
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.update(category);
        cascadeStatus(id, newStatus);
        categoryCacheService.invalidate();
        return toResponse(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireCategory(id);
        if (categoryMapper.countChildren(id) > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_CHILDREN, "分类下存在子分类");
        }
        if (categoryMapper.countApps(id) > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_APPS, "分类下存在软件");
        }
        categoryMapper.softDelete(id);
        categoryCacheService.invalidate();
    }

    private void cascadeStatus(Long parentId, Integer status) {
        List<CategoryEntity> children = categoryMapper.selectList(parentId, null);
        for (CategoryEntity child : children) {
            child.setStatus(status);
            child.setUpdatedAt(LocalDateTime.now());
            categoryMapper.update(child);
            cascadeStatus(child.getId(), status);
        }
    }

    private CategoryEntity requireCategory(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "参数格式错误");
        }
        CategoryEntity category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null) {
            return;
        }
        if (selfId != null && parentId.equals(selfId)) {
            throw new BusinessException(ErrorCode.CATEGORY_DEPTH_EXCEEDED, "分类层级不能超过两级");
        }
        CategoryEntity parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "父分类不存在");
        }
        if (parent.getParentId() != null && parent.getParentId() > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_DEPTH_EXCEEDED, "分类层级不能超过两级");
        }
    }

    private void ensureNameAvailable(String name, Long excludeId) {
        if (categoryMapper.countByName(name, excludeId) > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS, "分类名称已存在");
        }
    }

    private String normalizeName(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "分类名称不能为空");
        }
        String name = value.trim();
        if (name.length() > 50) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "分类名称不能超过50个字符");
        }
        return name;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId <= 0 ? null : parentId;
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private List<CategoryResponse> buildTree(List<CategoryEntity> categories) {
        Map<Long, CategoryResponse> categoryMap = new LinkedHashMap<>();
        for (CategoryEntity category : categories) {
            CategoryResponse response = toResponse(category);
            response.setChildren(new ArrayList<>());
            categoryMap.put(response.getId(), response);
        }

        List<CategoryResponse> roots = new ArrayList<>();
        for (CategoryEntity category : categories) {
            CategoryResponse response = categoryMap.get(category.getId());
            Long parentId = category.getParentId();
            if (parentId == null || parentId <= 0) {
                roots.add(response);
                continue;
            }
            CategoryResponse parent = categoryMap.get(parentId);
            if (parent != null) {
                parent.getChildren().add(response);
            }
        }
        return roots;
    }

    private CategoryResponse toResponse(CategoryEntity category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setIcon(category.getIcon());
        response.setParentId(category.getParentId());
        response.setSortOrder(category.getSortOrder());
        response.setStatus(category.getStatus());
        response.setIsBuiltin(category.getIsBuiltin());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
        return response;
    }
}
