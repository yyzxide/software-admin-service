package com.xcappstore.admin.tag.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.common.TransactionActions;
import com.xcappstore.admin.tag.dto.TagResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TagCacheService {
    private static final Logger log = LoggerFactory.getLogger(TagCacheService.class);
    private static final String LIST_ALL_KEY = "admin:tag:list:all";
    private static final String LIST_HOT_KEY = "admin:tag:list:hot";
    private static final String LIST_NORMAL_KEY = "admin:tag:list:normal";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public TagCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<List<TagResponse>> getList(Integer isHot) {
        String key = listKey(isHot);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(cached)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(cached, new TypeReference<>() {}));
        } catch (Exception ex) {
            log.warn("Read tag list cache failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public void putList(Integer isHot, List<TagResponse> tags) {
        String key = listKey(isHot);
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(tags), CACHE_TTL);
        } catch (Exception ex) {
            log.warn("Write tag list cache failed: {}", ex.getMessage());
        }
    }

    public void invalidate() {
        TransactionActions.afterCommit(() -> {
            try {
                redisTemplate.delete(LIST_ALL_KEY);
                redisTemplate.delete(LIST_HOT_KEY);
                redisTemplate.delete(LIST_NORMAL_KEY);
            } catch (Exception ex) {
                log.warn("Invalidate tag cache failed: {}", ex.getMessage());
            }
        });
    }

    private String listKey(Integer isHot) {
        if (isHot == null) {
            return LIST_ALL_KEY;
        }
        return isHot == 1 ? LIST_HOT_KEY : LIST_NORMAL_KEY;
    }
}
