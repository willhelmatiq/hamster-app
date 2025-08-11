package com.hamsterhub.tracker;

import com.hamsterhub.tracker.config.TrackerProperties;
import com.hamsterhub.tracker.model.EventWrapper;
import hamsterhub.common.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.Objects;

@Component
public class EventProcessor {
    private static final Logger log = LoggerFactory.getLogger(EventProcessor.class);

    private final Disposable subscription;
    private final TrackerState state;
    private final TrackerProperties props;

    public EventProcessor(EventBus bus, TrackerState state, TrackerProperties props) {
        this.state = state;
        this.props = props;

        this.subscription = bus.flux()
                .parallel(Math.max(1, props.workers()))
                .runOn(Schedulers.boundedElastic()) // можно Schedulers.parallel()
                .subscribe(this::handle, e -> log.error("Event stream error", e));
    }

    private void handle(EventWrapper env) {
        HamsterEvent event = env.event();
        String sensorId = env.sensorId();

        if (sensorId != null && !sensorId.isBlank()) {
            state.touchSensor(sensorId, env.receivedAt());
        }

        try {
            switch (event) {
                case HamsterEnter e -> handleEnter(e);
                case HamsterExit e -> handleExit(e);
                case WheelSpin e -> handleSpin(e);
                case SensorFailure e -> handleFailure(e, sensorId);
            }
        } catch (Exception ex) {
            log.error("Failed to process {}: {}", event, ex.toString(), ex);
        }
    }

    private void handleEnter(HamsterEnter e) {
        Objects.requireNonNull(e.hamsterId());
        Objects.requireNonNull(e.wheelId());
        state.occupyWheel(e.wheelId(), e.hamsterId());
        long now = System.currentTimeMillis();
        state.touchHamster(e.hamsterId(), now);
        state.statsFor(LocalDate.now(TrackerState.ZONE), e.hamsterId()).addRounds(0, now);
    }

    private void handleExit(HamsterExit e) {
        Objects.requireNonNull(e.hamsterId());
        Objects.requireNonNull(e.wheelId());
        state.releaseWheel(e.wheelId(), e.hamsterId());
        state.touchHamster(e.hamsterId(), System.currentTimeMillis());
    }

    private void handleSpin(WheelSpin e) {
        if (e.durationMs() < 0) {
            log.error("Negative duration: {}", e);
            return;
        }
        state.touchWheel(e.wheelId());
        String hamster = state.occupant(e.wheelId()).orElse(null);
        if (hamster == null) {
            log.warn("WheelSpin {} but no occupant", e);
            return;
        }
        int rounds = (int) (e.durationMs() / 5000L);
        if (rounds <= 0) return;

        long now = System.currentTimeMillis();
        state.touchHamster(hamster, now);
        state.statsFor(LocalDate.now(TrackerState.ZONE), hamster).addRounds(rounds, now);
    }

    private void handleFailure(SensorFailure e, String sensorId) {
        if (sensorId != null) state.touchSensor(sensorId, System.currentTimeMillis());
        // при желании — отметить статус сенсора
    }
}