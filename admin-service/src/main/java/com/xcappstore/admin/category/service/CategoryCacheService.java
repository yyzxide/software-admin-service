package com.xcappstore.admin.category.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.category.dto.CategoryResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CategoryCacheService {
    private static final Logger log = LoggerFactory.getLogger(CategoryCacheService.class);
    private static final String TREE_ALL_KEY = "admin:category:tree:all";
    private static final String TREE_STATUS_KEY_PREFIX = "admin:category:tree:status:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CategoryCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<List<CategoryResponse>> getTree(Integer status) {
        String key = treeKey(status);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(cached)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(cached, new TypeReference<>() {}));
        } catch (Exception ex) {
            log.warn("Read category tree cache failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public void putTree(Integer status, List<CategoryResponse> tree) {
        String key = treeKey(status);
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(tree), CACHE_TTL);
        } catch (Exception ex) {
            log.warn("Write category tree cache failed: {}", ex.getMessage());
        }
    }

    public void invalidate() {
        try {
            redisTemplate.delete(TREE_ALL_KEY);
            redisTemplate.delete(TREE_STATUS_KEY_PREFIX + "0");
            redisTemplate.delete(TREE_STATUS_KEY_PREFIX + "1");
        } catch (Exception ex) {
            log.warn("Invalidate category cache failed: {}", ex.getMessage());
        }
    }

    private String treeKey(Integer status) {
        return status == null ? TREE_ALL_KEY : TREE_STATUS_KEY_PREFIX + status;
    }
}
