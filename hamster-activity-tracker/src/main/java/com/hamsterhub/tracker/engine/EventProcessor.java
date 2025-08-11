package com.hamsterhub.tracker.engine;

import com.hamsterhub.tracker.EventBus;
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
                .runOn(Schedulers.boundedElastic())
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

        // отмечаем «живость» сенсора для мониторинга неактивности
        if (sensorId != null && !sensorId.isBlank()) {
            state.touchSensor(sensorId, eventWrapper.receivedAt());
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

    private void handleEnter(HamsterEnter event) {
        Objects.requireNonNull(event.hamsterId(), "HamsterId is required");
        Objects.requireNonNull(event.wheelId(), "WheelId is required");
        state.occupyWheel(event.wheelId(), event.hamsterId());
        long now = System.currentTimeMillis();
        state.touchHamster(event.hamsterId(), now);
        state.statsFor(LocalDate.now(TrackerState.ZONE), event.hamsterId()).addRounds(0, now);
        log.info("Enter: hamster={} wheel={}", event.hamsterId(), event.wheelId());
    }

    private void handleExit(HamsterExit event) {
        Objects.requireNonNull(event.hamsterId(), "HamsterId is required");
        Objects.requireNonNull(event.wheelId(), "WheelId is required");
        state.releaseWheel(event.wheelId(), event.hamsterId());
        state.touchHamster(event.hamsterId(), System.currentTimeMillis());
        log.info("Exit: hamster={} wheel={}", event.hamsterId(), event.wheelId());
    }

    private void handleSpin(WheelSpin event) {
        if (event.durationMs() < 0) {
            log.warn("Negative duration: {}", event);
            return;
        }

        state.touchWheel(event.wheelId());

        String hamster = state.getWheelHamster(event.wheelId()).orElse(null);
        if (hamster == null) {
            log.warn("WheelSpin {} by unknown hamster", event);
            return;
        }

        int rounds = (int) (event.durationMs() / 5000L);
        if (rounds <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        state.touchHamster(hamster, now);
        state.statsFor(LocalDate.now(TrackerState.ZONE), hamster).addRounds(rounds, now);

        log.info("Spin: hamster={} wheel={} +{} rounds", hamster, event.wheelId(), rounds);
    }

    private void handleFailure(SensorFailure e, String sensorId) {
        if (sensorId != null && !sensorId.isBlank()) {
            state.touchSensor(sensorId, System.currentTimeMillis());
        }
        log.info("SensorFailure: sensorId={} code={}", e.sensorId(), e.errorCode());
    }
}