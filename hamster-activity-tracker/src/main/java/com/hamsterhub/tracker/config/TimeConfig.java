package com.hamsterhub.tracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class TimeConfig {
    @Bean
    ZoneId appZone(TrackerProperties props) {
        return props.zoneId() != null && !props.zoneId().isBlank()
                ? ZoneId.of(props.zoneId())
                : ZoneId.systemDefault();
    }
}