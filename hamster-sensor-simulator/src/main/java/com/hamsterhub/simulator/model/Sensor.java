package com.hamsterhub.simulator.model;

import com.hamsterhub.simulator.statuses.SensorStatus;
import hamsterhub.common.events.HamsterEvent;
import hamsterhub.common.events.SensorFailure;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Sensor {
    private final String id;
    private volatile SensorStatus status = SensorStatus.WORKING;
    private final AtomicLong nextAllowedEmitMs = new AtomicLong(0);

    public Sensor(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public SensorStatus status() {
        return status;
    }

    public void status(SensorStatus s) {
        this.status = s;
    }

    /**
     * ≤1 событие/сек на датчик
     */
    public boolean tryAcquireEmission() {
        long now = System.currentTimeMillis();
        long next = nextAllowedEmitMs.get();
        return (now >= next) && nextAllowedEmitMs.compareAndSet(next, now + 1000);
    }

    /**
     * Ретрансляция wheel‑события через этот датчик (если он рабочий и прошёл rate‑limit)
     */
    public Mono<Void> relay(WebClient client, HamsterEvent event) {
        if (status != SensorStatus.WORKING) {
            return Mono.empty();
        }
        if (!tryAcquireEmission()) {
            return Mono.empty();
        }

        return client.post()
                .uri("/events")
                .header("X-Sensor-Id", id())
                .bodyValue(event)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    // логируем и молча пропускаем, чтобы не ломать цикл симуляции
                    System.err.println("Sensor " + id + " POST failed: " + e);
                    return Mono.empty();
                });
    }

    public Mono<Void> maybeFail(WebClient client,
                                double failPerTick,
                                double temporaryShare,
                                Supplier<Double> rnd,
                                Consumer<String> onPermanentBreak) {
        if (status != SensorStatus.WORKING) {
            return Mono.empty();
        }
        if (rnd.get() >= failPerTick) {
            return Mono.empty();
        }

        boolean temporaryFail = rnd.get() < temporaryShare; // например 20% — 666
        int code = temporaryFail ? 500 : 666;
        if (!temporaryFail) {
            status = SensorStatus.BROKEN;
            if (onPermanentBreak != null) {
                onPermanentBreak.accept(id);
            }
        }

        return client.post().uri("/events")
                .header("X-Sensor-Id", id())
                .bodyValue(new SensorFailure(id, code))
                .retrieve().bodyToMono(Void.class)
                .onErrorResume(e -> {
                    System.err.println("Failure POST failed: " + e);
                    return Mono.empty();
                });
    }
}
