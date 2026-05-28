package com.xcappstore.admin.category.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xcappstore.admin.category.dto.CategoryCreateRequest;
import com.xcappstore.admin.category.dto.CategoryResponse;
import com.xcappstore.admin.category.entity.CategoryEntity;
import com.xcappstore.admin.category.mapper.CategoryMapper;
import com.xcappstore.admin.category.service.impl.CategoryServiceImpl;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryServiceImplTest {
    private FakeCategoryMapper categoryMapper;
    private FakeCategoryCacheService categoryCacheService;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryMapper = new FakeCategoryMapper();
        categoryCacheService = new FakeCategoryCacheService();
        categoryService = new CategoryServiceImpl(categoryMapper, categoryCacheService);
    }

    @Test
    void createsRootCategory() {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName(" 办公增强 ");
        request.setSortOrder(10);

        categoryMapper.nextId = 100L;

        Long categoryId = categoryService.create(request);

        assertEquals(100L, categoryId);
        assertEquals("办公增强", categoryMapper.inserted.getName());
        assertEquals(1, categoryMapper.inserted.getStatus());
        assertEquals(1, categoryCacheService.invalidateCount);
    }

    @Test
    void rejectsDuplicateCategoryName() {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("办公软件");
        categoryMapper.categories.put(1L, category(1L, "办公软件"));

        BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.create(request));

        assertEquals(ErrorCode.CATEGORY_NAME_EXISTS, ex.getCode());
    }

    @Test
    void rejectsThirdLevelCategory() {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("子分类");
        request.setParentId(2L);

        CategoryEntity parent = category(2L, "二级分类");
        parent.setParentId(1L);
        categoryMapper.categories.put(2L, parent);

        BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.create(request));

        assertEquals(ErrorCode.CATEGORY_DEPTH_EXCEEDED, ex.getCode());
    }

    @Test
    void rejectsDeleteWhenCategoryHasApps() {
        categoryMapper.categories.put(1L, category(1L, "办公软件"));
        categoryMapper.appCounts.put(1L, 1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.delete(1L));

        assertEquals(ErrorCode.CATEGORY_HAS_APPS, ex.getCode());
    }

    @Test
    void returnsCachedTreeWhenPresent() {
        CategoryResponse cached = new CategoryResponse();
        cached.setId(1L);
        cached.setName("办公软件");
        categoryCacheService.cachedTree = List.of(cached);

        List<CategoryResponse> tree = categoryService.tree(1);

        assertEquals(1, tree.size());
        assertEquals("办公软件", tree.get(0).getName());
    }

    private CategoryEntity category(Long id, String name) {
        CategoryEntity entity = new CategoryEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setSortOrder(1);
        entity.setStatus(1);
        entity.setIsBuiltin(0);
        return entity;
    }

    private static final class FakeCategoryMapper implements CategoryMapper {
        private final Map<Long, CategoryEntity> categories = new HashMap<>();
        private final Map<Long, Long> appCounts = new HashMap<>();
        private Long nextId = 1L;
        private CategoryEntity inserted;

        @Override
        public int insert(CategoryEntity category) {
            category.setId(nextId);
            inserted = category;
            categories.put(category.getId(), category);
            return 1;
        }

        @Override
        public CategoryEntity selectById(Long id) {
            return categories.get(id);
        }

        @Override
        public List<CategoryEntity> selectList(Long parentId, Integer status) {
            return categories.values().stream()
                .filter(category -> parentId == null ? category.getParentId() == null : parentId.equals(category.getParentId()))
                .filter(category -> status == null || status.equals(category.getStatus()))
                .toList();
        }

        @Override
        public List<CategoryEntity> selectAll(Integer status) {
            return categories.values().stream()
                .filter(category -> status == null || status.equals(category.getStatus()))
                .toList();
        }

        @Override
        public long countByName(String name, Long excludeId) {
            return categories.values().stream()
                .filter(category -> name.equals(category.getName()))
                .filter(category -> excludeId == null || !excludeId.equals(category.getId()))
                .count();
        }

        @Override
        public long countChildren(Long parentId) {
            return categories.values().stream()
                .filter(category -> parentId.equals(category.getParentId()))
                .count();
        }

        @Override
        public long countApps(Long categoryId) {
            return appCounts.getOrDefault(categoryId, 0L);
        }

        @Override
        public int update(CategoryEntity category) {
            categories.put(category.getId(), category);
            return 1;
        }

        @Override
        public int softDelete(Long id) {
            categories.remove(id);
            return 1;
        }
    }

    private static final class FakeCategoryCacheService extends CategoryCacheService {
        private List<CategoryResponse> cachedTree = new ArrayList<>();
        private int invalidateCount;

        private FakeCategoryCacheService() {
            super(null, null);
        }

        @Override
        public Optional<List<CategoryResponse>> getTree(Integer status) {
            return cachedTree.isEmpty() ? Optional.empty() : Optional.of(cachedTree);
        }

        @Override
        public void putTree(Integer status, List<CategoryResponse> tree) {
            cachedTree = tree;
        }

        @Override
        public void invalidate() {
            invalidateCount++;
        }
    }
}
