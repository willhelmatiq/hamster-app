package com.hamsterhub.tracker.engine;

import com.hamsterhub.tracker.config.TrackerProperties;
import com.hamsterhub.tracker.model.EventWrapper;
import hamsterhub.common.events.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Component
public class EventProcessor {
    private static final Logger log = LoggerFactory.getLogger(EventProcessor.class);

    private final EventBus bus;
    private final TrackerState state;
    private final TrackerProperties props;

    private Disposable subscription;

    public EventProcessor(EventBus bus, TrackerState state, TrackerProperties props) {
        this.bus = bus;
        this.state = state;
        this.props = props;
    }

    @PostConstruct
    void start() {
        int workers = Math.max(1, props.workers());
        subscription = bus.flux()
                .doOnSubscribe(s -> log.info("EventProcessor subscribed, workers={}", workers))
                .parallel(workers)
                .runOn(Schedulers.parallel())
                .doOnNext(eventWrapper -> log.debug("DEQUEUE {}", eventWrapper))
                .subscribe(this::handle, e -> log.error("Event stream error", e));
        log.info("EventProcessor started with {} workers", workers);
    }

    @PreDestroy
    void stop() {
        if (subscription != null) subscription.dispose();
    }

    private void handle(EventWrapper eventWrapper) {
        HamsterEvent event = eventWrapper.event();
        String sensorId = eventWrapper.sensorId();
        long receivedAt = eventWrapper.receivedAt();

        // отмечаем «живость» сенсора для мониторинга неактивности
        if (sensorId != null && !sensorId.isBlank()) {
            state.updateSensorLastEvent(sensorId, eventWrapper.receivedAt());
        }

        try {
            switch (event) {
                case HamsterEnter e -> handleEnter(e, receivedAt);
                case HamsterExit e -> handleExit(e, receivedAt);
                case WheelSpin e -> handleSpin(e, receivedAt);
                case SensorFailure e -> handleFailure(e, sensorId, receivedAt);
            }
        } catch (Exception ex) {
            log.error("Failed to process {}: {}", event, ex, ex);
        }
    }

    private void handleEnter(HamsterEnter event, long receivedAt) {
        Objects.requireNonNull(event.hamsterId(), "HamsterId is required");
        Objects.requireNonNull(event.wheelId(), "WheelId is required");
        state.updateHamsterLastEvent(event.hamsterId(), receivedAt);
        // идемпотентность
        var current = state.getWheelHamster(event.wheelId()).orElse(null);
        if (!Objects.equals(current, event.hamsterId())) {
            state.occupyWheel(event.wheelId(), event.hamsterId());
        }
        var date = java.time.Instant.ofEpochMilli(receivedAt).atZone(TrackerState.ZONE).toLocalDate();
        state.statsFor(date, event.hamsterId()).addRounds(0, receivedAt);
        log.info("Enter: hamster={} wheel={}", event.hamsterId(), event.wheelId());
    }

    private void handleExit(HamsterExit event, long receivedAt) {
        Objects.requireNonNull(event.hamsterId(), "HamsterId is required");
        Objects.requireNonNull(event.wheelId(), "WheelId is required");
        state.updateHamsterLastEvent(event.hamsterId(), receivedAt);
        // идемпотентность
        String current = state.getWheelHamster(event.wheelId()).orElse(null);
        if (Objects.equals(current, event.hamsterId())) {
            state.releaseWheel(event.wheelId(), event.hamsterId());
            log.info("Exit: hamster={} wheel={}", event.hamsterId(), event.wheelId());
        } else {
            log.warn("Exit ignored: hamster {} is not occupying wheel {}", event.hamsterId(), event.wheelId());
        }
    }

    private void handleSpin(WheelSpin event, long receivedAt) {
        if (event.durationMs() < 0) {
            log.warn("Negative duration: {}", event);
            return;
        }

        String hamster = state.getWheelHamster(event.wheelId()).orElse(null);
        if (hamster == null) {
            log.warn("WheelSpin {} by unknown hamster", event);
            return;
        }

        if (!state.shouldAcceptSpin(event.wheelId(), event.durationMs(), receivedAt, props.deduplicationWindowMs())) {
            log.debug("Deduplicated spin: wheel={} durationMs={} ts={}",
                    event.wheelId(), event.durationMs(), receivedAt);
            return;
        }

        int rounds = (int) (event.durationMs() / 5_000L);
        if (rounds <= 0) return;

        state.updateHamsterLastEvent(hamster, receivedAt);
        state.statsFor(dateOf(receivedAt), hamster).addRounds(rounds, receivedAt);

        log.info("Spin: hamster={} wheel={} +{} rounds", hamster, event.wheelId(), rounds);
    }

    private void handleFailure(SensorFailure e, String sensorId, long receivedAt) {
        if (sensorId != null && !sensorId.isBlank()) {
            state.updateSensorLastEvent(sensorId, receivedAt);
        }
        log.info("SensorFailure: sensorId={} code={}", e.sensorId(), e.errorCode());
    }

    private static LocalDate dateOf(long tsMs) {
        return Instant.ofEpochMilli(tsMs).atZone(TrackerState.ZONE).toLocalDate();
    }
}