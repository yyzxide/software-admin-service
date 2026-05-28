package com.xcappstore.admin.tag.service.impl;

import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.tag.dto.TagCreateRequest;
import com.xcappstore.admin.tag.dto.TagQueryRequest;
import com.xcappstore.admin.tag.dto.TagResponse;
import com.xcappstore.admin.tag.dto.TagUpdateRequest;
import com.xcappstore.admin.tag.entity.TagEntity;
import com.xcappstore.admin.tag.mapper.TagMapper;
import com.xcappstore.admin.tag.service.TagCacheService;
import com.xcappstore.admin.tag.service.TagService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TagServiceImpl implements TagService {
    private static final int NORMAL_TAG = 0;
    private static final int HOT_TAG = 1;

    private final TagMapper tagMapper;
    private final TagCacheService tagCacheService;

    public TagServiceImpl(TagMapper tagMapper, TagCacheService tagCacheService) {
        this.tagMapper = tagMapper;
        this.tagCacheService = tagCacheService;
    }

    @Override
    @Transactional
    public Long create(TagCreateRequest request) {
        String name = normalizeName(request.getName());
        ensureNameAvailable(name, null);

        TagEntity tag = new TagEntity();
        tag.setName(name);
        tag.setDescription(normalizeText(request.getDescription()));
        tag.setIsHot(request.getIsHot() == null ? NORMAL_TAG : request.getIsHot());
        tag.setIsBuiltin(0);
        tag.setCreatedAt(LocalDateTime.now());

        tagMapper.insert(tag);
        tagCacheService.invalidate();
        return tag.getId();
    }

    @Override
    public TagResponse detail(Long id) {
        return toResponse(requireTag(id));
    }

    @Override
    public List<TagResponse> list(TagQueryRequest request) {
        String keyword = normalizeKeyword(request.getKeyword());
        if (!StringUtils.hasText(keyword)) {
            return tagCacheService.getList(request.getIsHot()).orElseGet(() -> {
                List<TagResponse> tags = listFromDatabase(request.getIsHot(), null);
                tagCacheService.putList(request.getIsHot(), tags);
                return tags;
            });
        }
        return listFromDatabase(request.getIsHot(), keyword);
    }

    @Override
    @Transactional
    public void update(Long id, TagUpdateRequest request) {
        TagEntity tag = requireTag(id);
        if (StringUtils.hasText(request.getName())) {
            String name = normalizeName(request.getName());
            ensureNameAvailable(name, id);
            tag.setName(name);
        }
        if (request.getDescription() != null) {
            tag.setDescription(normalizeText(request.getDescription()));
        }
        if (request.getIsHot() != null) {
            tag.setIsHot(request.getIsHot());
        }
        tagMapper.update(tag);
        tagCacheService.invalidate();
    }

    @Override
    @Transactional
    public TagResponse toggleHot(Long id) {
        TagEntity tag = requireTag(id);
        tag.setIsHot(HOT_TAG == tag.getIsHot() ? NORMAL_TAG : HOT_TAG);
        tagMapper.update(tag);
        tagCacheService.invalidate();
        return toResponse(tag);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireTag(id);
        if (tagMapper.countApps(id) > 0) {
            throw new BusinessException(ErrorCode.TAG_HAS_APPS, "标签已被软件使用");
        }
        tagMapper.deleteById(id);
        tagCacheService.invalidate();
    }

    private List<TagResponse> listFromDatabase(Integer isHot, String keyword) {
        return tagMapper.selectList(isHot, keyword)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private TagEntity requireTag(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "参数格式错误");
        }
        TagEntity tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND, "标签不存在");
        }
        return tag;
    }

    private void ensureNameAvailable(String name, Long excludeId) {
        if (tagMapper.countByName(name, excludeId) > 0) {
            throw new BusinessException(ErrorCode.TAG_NAME_EXISTS, "标签名称已存在");
        }
    }

    private String normalizeName(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "标签名称不能为空");
        }
        String name = value.trim();
        if (name.length() > 50) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "标签名称不能超过50个字符");
        }
        return name;
    }

    private String normalizeKeyword(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private TagResponse toResponse(TagEntity tag) {
        TagResponse response = new TagResponse();
        response.setId(tag.getId());
        response.setName(tag.getName());
        response.setDescription(tag.getDescription());
        response.setIsHot(tag.getIsHot());
        response.setIsBuiltin(tag.getIsBuiltin());
        response.setAppCount(tag.getAppCount() == null ? 0L : tag.getAppCount());
        response.setCreatedAt(tag.getCreatedAt());
        return response;
    }
}
