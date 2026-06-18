package com.xcappstore.admin.software.service;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PackageUploadCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(PackageUploadCleanupScheduler.class);

    private final PackageUploadSessionService uploadSessionService;
    private final boolean enabled;
    private final long expireHours;
    private final int batchSize;

    public PackageUploadCleanupScheduler(
        PackageUploadSessionService uploadSessionService,
        @Value("${admin.upload.cleanup.enabled:true}") boolean enabled,
        @Value("${admin.upload.cleanup.expire-hours:24}") long expireHours,
        @Value("${admin.upload.cleanup.batch-size:100}") int batchSize
    ) {
        this.uploadSessionService = uploadSessionService;
        this.enabled = enabled;
        this.expireHours = expireHours;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${admin.upload.cleanup.fixed-delay-ms:3600000}")
    public void cleanupExpiredUploadingSessions() {
        if (!enabled) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusHours(Math.max(1, expireHours));
        int cleaned = uploadSessionService.cleanupExpiredUploadingSessions(cutoff, batchSize);
        if (cleaned > 0) {
            log.info("Cleaned {} expired package upload sessions before {}", cleaned, cutoff);
        }
    }
}
