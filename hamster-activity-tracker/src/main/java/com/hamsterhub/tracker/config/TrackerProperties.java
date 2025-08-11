package com.hamsterhub.tracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tracker")
public record TrackerProperties(
        int workers,
        long hamsterInactivityMs,
        long sensorInactivityMs
) {
}
