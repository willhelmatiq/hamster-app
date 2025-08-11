package com.hamsterhub.tracker.engine;

import com.hamsterhub.tracker.config.TrackerProperties;
import com.hamsterhub.tracker.service.AlertService;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
class InactivityMonitor {

    private final TrackerState state;
    private final AlertService alerts;
    private final TrackerProperties props;
    private final Disposable task;
    private final ConcurrentHashMap.KeySetView<String, Boolean> hamsterAlerted =
            ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap.KeySetView<String, Boolean> sensorAlerted  =
            ConcurrentHashMap.newKeySet();

    InactivityMonitor(TrackerState state, AlertService alerts, TrackerProperties props) {
        this.state = state;
        this.alerts = alerts;
        this.props = props;

        this.task = Flux.interval(Duration.ofMinutes(1))
                .onBackpressureDrop()
                .subscribe(t -> check());
    }

    void check() {
        long now = System.currentTimeMillis();

        // Хомяки (> hamsterInactivityMs)
        Map<String, TrackerState.DayStats> today = state.getStatsForDate(LocalDate.now(TrackerState.ZONE));
        today.forEach((hamId, stats) -> {
            boolean inactive = now - stats.lastActiveEpochMs() > props.hamsterInactivityMs();
            if (inactive) {
                if (hamsterAlerted.add(hamId)) alerts.sendAlert("Hamster %s inactive".formatted(hamId));
            } else {
                hamsterAlerted.remove(hamId);
            }
        });

        // Сенсоры (> sensorInactivityMs)
        state.sensorsLastSeen().forEach((sensorId, last) -> {
            boolean inactive = now - last > props.sensorInactivityMs();
            if (inactive) {
                if (sensorAlerted.add(sensorId)) alerts.sendAlert("Sensor %s inactive".formatted(sensorId));
            } else {
                sensorAlerted.remove(sensorId);
            }
        });
    }
}
