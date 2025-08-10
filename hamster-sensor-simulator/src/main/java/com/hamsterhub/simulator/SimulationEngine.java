package com.hamsterhub.simulator;

import com.hamsterhub.simulator.config.RuntimeConfig;
import com.hamsterhub.simulator.config.SimulatorProperties;
import com.hamsterhub.simulator.entity.Sensor;
import com.hamsterhub.simulator.entity.Wheel;
import com.hamsterhub.simulator.statuses.WheelStatus;
import hamsterhub.common.events.HamsterEnter;
import hamsterhub.common.events.HamsterEvent;
import hamsterhub.common.events.HamsterExit;
import hamsterhub.common.events.WheelSpin;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class SimulationEngine {

    private final WorldState world;               // состояние: колёса, датчики, очередь хомяков
    private final SimulatorProperties props;      // статические настройки из application.yml
    private final SimulatorServiceImpl service;   // даёт текущий runtime-снимок (hamsters/sensors)
    private final WebClient client;               // HTTP в tracker
    private final Random rnd = new Random();

    private Disposable loop; // «ручка» на бесконечную симуляцию

    // <-- constructor injection: @Autowired НЕ НУЖЕН, один конструктор
    public SimulationEngine(WorldState world,
                            SimulatorProperties props,
                            SimulatorServiceImpl service,
                            WebClient trackerClient) {
        this.world  = world;
        this.props  = props;
        this.service = service;
        this.client = trackerClient;
    }

    @PostConstruct
    public void start() {
        // первичная сборка мира из дефолтов (которые сервис положил в runtime при создании)
        rebuildWorld(service.currentRuntime());

        // главный цикл: тикаем раз в секунду на одном шедулере
        loop = Flux.interval(Duration.ZERO, Duration.ofSeconds(1), Schedulers.newSingle("sim-loop"))
                .flatMap(tick -> tickOnce())   // здесь твоя логика обработки всех колёс
                .onErrorContinue((e, o) -> System.err.println("Simulation error: " + e))
                .subscribe();
    }

    @PreDestroy
    public void stop() {
        if (loop != null && !loop.isDisposed()) {
            loop.dispose(); // корректно останавливаем симуляцию при завершении приложения
        }
    }

    /** Переинициализация мира под новый runtime-снимок (вызов из сервиса при /config) */
    public void rebuildWorld(RuntimeConfig cfg) {
        world.clear();

        // пример: колёс = cfg.sensorCount(), на каждое по 10 датчиков
        int wheelsCount = cfg.sensorCount();
        int sensorsPerWheel = 10;

        for (int w = 0; w < wheelsCount; w++) {
            var sensors = new java.util.ArrayList<Sensor>(sensorsPerWheel);
            for (int j = 0; j < sensorsPerWheel; j++) {
                sensors.add(new Sensor("sensor-" + (w * sensorsPerWheel + j)));
            }
            world.wheels().add(new Wheel("wheel-" + w, sensors));
        }

        for (int i = 0; i < cfg.hamsterCount(); i++) {
            world.readyHamsters().add("ham-" + i);
        }
    }

    /** Один тик: обработать все колёса (вставь сюда свою реализацию tickWheel/broadcast/spinOnce) */
    private reactor.core.publisher.Mono<Void> tickOnce() {
        var pollHamster   = (java.util.function.Supplier<String>) () -> world.readyHamsters().poll();
        var scheduleRest  = (java.util.function.Consumer<String>) (hamId) ->
                reactor.core.publisher.Mono.delay(Duration.ofSeconds(props.restSecAfterEscape()))
                        .doOnNext(t -> world.readyHamsters().add(hamId))
                        .subscribe();

        return reactor.core.publisher.Flux.fromIterable(world.wheels())
                .flatMap(w -> tickWheel(w, props, client, rnd, pollHamster, scheduleRest), 64)
                .then();
    }

    /** --- Твои вспомогательные методы ЛЕЖАТ ЗДЕСЬ --- */

    // Отправить одно wheel‑событие через ВСЕ рабочие датчики этого колеса
    private Mono<Void> broadcastViaSensors(Wheel w, WebClient client, HamsterEvent event) {
        return Flux.fromIterable(w.sensors())
                .flatMap(sensor -> sensor.relay(client, event), 64)
                .then();
    }

    private Mono<Void> tickWheel(
            Wheel w,
            SimulatorProperties props,
            WebClient client,
            Random rnd,
            Supplier<String> pollHamster,
            Consumer<String> scheduleRest
    ) {
        double failPerSec  = props.failProb()  / 60.0;
        double enterPerSec = props.enterProb() / 60.0;

        Mono<Void> failures = Flux.fromIterable(w.sensors())
                .flatMap(s -> s.maybeFail(client, failPerSec, rnd::nextDouble), 64)
                .then();

        Mono<Void> wheelFlow = Mono.defer(() -> {
            if (w.status() == WheelStatus.FREE && rnd.nextDouble() < enterPerSec) {
                String h = pollHamster.get();
                if (h != null && w.tryEnter(h)) {
                    return broadcastViaSensors(w, client, new HamsterEnter(h, w.wheelId()))
                            .then(spinOnce(w, props, client, rnd));
                } else {
                    return Mono.empty();
                }
            }

            if (w.status() == WheelStatus.TAKEN) {
                return spinOnce(w, props, client, rnd)
                        .then(Mono.defer(() -> {
                            if (rnd.nextDouble() < 0.2) {
                                String owner = w.owner();
                                if (owner != null) {
                                    w.exitIfOwner(owner);
                                    scheduleRest.accept(owner);
                                    return broadcastViaSensors(w, client, new HamsterExit(owner, w.wheelId()));
                                }
                            }
                            return Mono.empty();
                        }));
            }

            return Mono.empty();
        });

        return failures.then(wheelFlow);
    }

    private Mono<Void> spinOnce(Wheel w, SimulatorProperties props, WebClient client, Random rnd) {
        int sec = rnd.nextInt(props.spinSecMax() - props.spinSecMin() + 1) + props.spinSecMin();
        return broadcastViaSensors(w, client, new WheelSpin(w.wheelId(), sec * 1000L));
    }
}
