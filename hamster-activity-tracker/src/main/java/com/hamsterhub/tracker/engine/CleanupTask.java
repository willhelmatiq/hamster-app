package com.hamsterhub.tracker.engine;

import com.hamsterhub.tracker.config.TrackerProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class CleanupTask {
    private final TrackerState state;
    private final TrackerProperties props;

    CleanupTask(TrackerState state, TrackerProperties props) {
        this.state = state;
        this.props = props;
    }

    @Scheduled(fixedRateString = "${tracker.cleanup-interval-ms}")
    public void cleanup() {
        long retainMs = props.cleanupIntervalMs();
    }
}
