package com.hamsterhub.tracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tracker")
public record TrackerProperties(
        int activeThreshold,
        long cleanupIntervalMs,
        long deduplicationWindowMs,
        String exportCron,
        int exportDaysBack,
        long hamsterInactivityMs,
        long sensorInactivityMs,
        int workers
) {
}
