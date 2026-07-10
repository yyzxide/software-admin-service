package com.xcappstore.admin.software.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.common.TransactionActions;
import com.xcappstore.admin.software.dto.SoftwareResponse;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SoftwareCacheService {
    private static final Logger log = LoggerFactory.getLogger(SoftwareCacheService.class);
    private static final String DETAIL_KEY_PREFIX = "admin:software:detail:";
    private static final Duration DETAIL_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SoftwareCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<SoftwareResponse> getDetail(Long id) {
        try {
            String cached = redisTemplate.opsForValue().get(detailKey(id));
            if (!StringUtils.hasText(cached)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(cached, SoftwareResponse.class));
        } catch (Exception ex) {
            log.warn("Read software detail cache failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public void putDetail(Long id, SoftwareResponse response) {
        try {
            redisTemplate.opsForValue().set(detailKey(id), objectMapper.writeValueAsString(response), DETAIL_TTL);
        } catch (Exception ex) {
            log.warn("Write software detail cache failed: {}", ex.getMessage());
        }
    }

    public void invalidateDetail(Long id) {
        TransactionActions.afterCommit(() -> {
            try {
                redisTemplate.delete(detailKey(id));
            } catch (Exception ex) {
                log.warn("Invalidate software detail cache failed: {}", ex.getMessage());
            }
        });
    }

    private String detailKey(Long id) {
        return DETAIL_KEY_PREFIX + id;
    }
}
