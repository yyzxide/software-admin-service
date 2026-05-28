package com.xcappstore.admin.tag.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.tag.dto.TagCreateRequest;
import com.xcappstore.admin.tag.dto.TagQueryRequest;
import com.xcappstore.admin.tag.dto.TagResponse;
import com.xcappstore.admin.tag.entity.TagEntity;
import com.xcappstore.admin.tag.mapper.TagMapper;
import com.xcappstore.admin.tag.service.impl.TagServiceImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TagServiceImplTest {
    private FakeTagMapper tagMapper;
    private FakeTagCacheService tagCacheService;
    private TagServiceImpl tagService;

    @BeforeEach
    void setUp() {
        tagMapper = new FakeTagMapper();
        tagCacheService = new FakeTagCacheService();
        tagService = new TagServiceImpl(tagMapper, tagCacheService);
    }

    @Test
    void createsTagWithTrimmedName() {
        TagCreateRequest request = new TagCreateRequest();
        request.setName("  国产化  ");
        request.setIsHot(1);
        tagMapper.nextId = 10L;

        Long tagId = tagService.create(request);

        assertEquals(10L, tagId);
        assertEquals("国产化", tagMapper.inserted.getName());
        assertEquals(1, tagMapper.inserted.getIsHot());
        assertEquals(1, tagCacheService.invalidateCount);
    }

    @Test
    void rejectsDuplicateTagName() {
        TagCreateRequest request = new TagCreateRequest();
        request.setName("办公");
        tagMapper.tags.put(1L, tag(1L, "办公", 0));

        BusinessException ex = assertThrows(BusinessException.class, () -> tagService.create(request));

        assertEquals(ErrorCode.TAG_NAME_EXISTS, ex.getCode());
    }

    @Test
    void returnsCachedListWhenKeywordIsBlank() {
        TagResponse cached = new TagResponse();
        cached.setId(1L);
        cached.setName("热门");
        tagCacheService.cachedTags = List.of(cached);

        TagQueryRequest request = new TagQueryRequest();
        request.setIsHot(1);
        List<TagResponse> tags = tagService.list(request);

        assertEquals(1, tags.size());
        assertEquals("热门", tags.get(0).getName());
    }

    @Test
    void searchesDatabaseWhenKeywordExists() {
        tagMapper.tags.put(1L, tag(1L, "安全", 1));
        tagMapper.tags.put(2L, tag(2L, "办公", 0));

        TagQueryRequest request = new TagQueryRequest();
        request.setKeyword("安全");
        List<TagResponse> tags = tagService.list(request);

        assertEquals(1, tags.size());
        assertEquals("安全", tags.get(0).getName());
    }

    @Test
    void rejectsDeleteWhenTagHasApps() {
        tagMapper.tags.put(1L, tag(1L, "办公", 0));
        tagMapper.appCounts.put(1L, 2L);

        BusinessException ex = assertThrows(BusinessException.class, () -> tagService.delete(1L));

        assertEquals(ErrorCode.TAG_HAS_APPS, ex.getCode());
    }

    private TagEntity tag(Long id, String name, Integer isHot) {
        TagEntity tag = new TagEntity();
        tag.setId(id);
        tag.setName(name);
        tag.setIsHot(isHot);
        tag.setIsBuiltin(0);
        tag.setAppCount(0L);
        return tag;
    }

    private static final class FakeTagMapper implements TagMapper {
        private final Map<Long, TagEntity> tags = new HashMap<>();
        private final Map<Long, Long> appCounts = new HashMap<>();
        private Long nextId = 1L;
        private TagEntity inserted;

        @Override
        public int insert(TagEntity tag) {
            tag.setId(nextId);
            inserted = tag;
            tags.put(tag.getId(), tag);
            return 1;
        }

        @Override
        public TagEntity selectById(Long id) {
            return tags.get(id);
        }

        @Override
        public List<TagEntity> selectList(Integer isHot, String keyword) {
            return tags.values().stream()
                .filter(tag -> isHot == null || isHot.equals(tag.getIsHot()))
                .filter(tag -> keyword == null || tag.getName().contains(keyword)
                    || (tag.getDescription() != null && tag.getDescription().contains(keyword)))
                .toList();
        }

        @Override
        public long countByName(String name, Long excludeId) {
            return tags.values().stream()
                .filter(tag -> name.equals(tag.getName()))
                .filter(tag -> excludeId == null || !excludeId.equals(tag.getId()))
                .count();
        }

        @Override
        public long countApps(Long tagId) {
            return appCounts.getOrDefault(tagId, 0L);
        }

        @Override
        public int update(TagEntity tag) {
            tags.put(tag.getId(), tag);
            return 1;
        }

        @Override
        public int deleteById(Long id) {
            tags.remove(id);
            return 1;
        }
    }

    private static final class FakeTagCacheService extends TagCacheService {
        private List<TagResponse> cachedTags = new ArrayList<>();
        private int invalidateCount;

        private FakeTagCacheService() {
            super(null, null);
        }

        @Override
        public Optional<List<TagResponse>> getList(Integer isHot) {
            return cachedTags.isEmpty() ? Optional.empty() : Optional.of(cachedTags);
        }

        @Override
        public void putList(Integer isHot, List<TagResponse> tags) {
            cachedTags = tags;
        }

        @Override
        public void invalidate() {
            invalidateCount++;
        }
    }
}
