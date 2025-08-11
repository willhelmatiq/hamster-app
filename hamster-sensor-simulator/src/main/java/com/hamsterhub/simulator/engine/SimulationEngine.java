package com.hamsterhub.simulator.engine;

import com.hamsterhub.simulator.config.SimulatorConfig;
import com.hamsterhub.simulator.config.SimulatorProperties;
import com.hamsterhub.simulator.model.Wheel;
import com.hamsterhub.simulator.statuses.WheelStatus;
import hamsterhub.common.events.HamsterEnter;
import hamsterhub.common.events.HamsterEvent;
import hamsterhub.common.events.HamsterExit;
import hamsterhub.common.events.WheelSpin;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class SimulationEngine {

    private final WorldState world;
    private final SimulatorProperties props;
    private final WebClient client;
    private final Random rnd = new Random();
    private Disposable loop;

    private final Scheduler loopScheduler = Schedulers.newSingle("sim-loop");
    private Scheduler wheelScheduler;

    public SimulationEngine(WorldState world,
                            SimulatorProperties props,
                            WebClient trackerClient) {
        this.world = world;
        this.props = props;
        this.client = trackerClient;
    }

    @PostConstruct
    public void start() {
        applyRuntime(new SimulatorConfig(
                props.defaults().hamsterCount(),
                props.defaults().sensorCount()
        )).subscribeOn(loopScheduler).block();

        wheelScheduler = Schedulers.newParallel("sim-exec", Math.max(1, props.parallelism()));

        loop = Flux.interval(Duration.ZERO, Duration.ofSeconds(props.tickSeconds()), loopScheduler)
                .flatMap(tick -> tickOnce())
                .onErrorContinue((e, o) -> System.err.println("Simulation error: " + e))
                .subscribe();
    }

    @PreDestroy
    public void stop() {
        if (loop != null && !loop.isDisposed()) {
            loop.dispose();
        }
        if (wheelScheduler != null) {
            wheelScheduler.dispose();
        }
    }

    public Mono<Void> applyRuntime(SimulatorConfig cfg) {
        return Mono.fromRunnable(() -> {
                    world.adjustTotalHamsters(Math.max(1, cfg.hamsterCount()));
                    world.adjustSensors(Math.max(1, cfg.sensorCount()));
                })
                .subscribeOn(loopScheduler)
                .then();
    }

    // P_tick = 1 - (1 - P_min)^(tick/60) - формула для вычисления вероятности
    private static double perTick(double perMinute, int tickSeconds) {
        return 1.0 - Math.pow(1.0 - perMinute, tickSeconds / 60.0);
    }

    /**
     * Один тик: обработать все колёса
     */
    private Mono<Void> tickOnce() {
        Supplier<String>  pollHamster  = () -> world.readyHamsters().poll();
        Consumer<String>  scheduleRest = hamId ->
                Mono.delay(Duration.ofSeconds(props.restSecAfterEscape()))
                        .doOnNext(t -> world.readyHamsters().add(hamId))
                        .subscribe();

        return Flux.fromIterable(world.wheels())
                .parallel(Math.max(1, props.parallelism()))
                .runOn(wheelScheduler)
                .flatMap(wheel -> tickWheel(wheel, props, client, pollHamster, scheduleRest)) // ограничение внутри rail
                .sequential()
                .then();
    }

    // Отправить одно wheel‑событие через ВСЕ рабочие датчики этого колеса
    private Mono<Void> broadcastViaSensors(Wheel wheel, WebClient client, HamsterEvent event) {
        return Flux.fromIterable(wheel.sensorSnapshot())
                .flatMap(sensor -> sensor.relay(client, event), 64)
                .then();
    }

    private Mono<Void> tickWheel(
            Wheel wheel,
            SimulatorProperties props,
            WebClient client,
            Supplier<String> pollHamster,
            Consumer<String> scheduleRest
    ) {
        int tick = props.tickSeconds();
        double failPerTick = perTick(props.failPerMinute(), tick);
        double enterPerTick = perTick(props.enterPerMinute(), tick);
        double exitPerTick = enterPerTick;
        var rnd = ThreadLocalRandom.current();

        Mono<Void> failures = Flux.fromIterable(wheel.sensorSnapshot())
                .flatMap(sensor -> sensor.maybeFail(
                        client,
                        failPerTick,
                        props.temporaryFailShare(),
                        rnd::nextDouble,
                        world::markSensorBroken
                ), Math.max(1, props.perWheelIoConcurrency()))
                .then();

        Mono<Void> wheelFlow = Mono.defer(() -> {
            if (wheel.status() == WheelStatus.FREE && rnd.nextDouble() < enterPerTick) {
                String hamsterId = pollHamster.get();
                if (hamsterId != null && wheel.tryEnter(hamsterId)) {
                    world.noteEnter(hamsterId, wheel);
                    int sec = rnd.nextInt(props.spinSecMin(), props.spinSecMax() + 1);
                    return broadcastViaSensors(wheel, client, new HamsterEnter(hamsterId, wheel.wheelId()))
                            .then(broadcastViaSensors(wheel, client, new WheelSpin(wheel.wheelId(), sec * 1000L)));
                }
                return Mono.empty();
            }

            if (wheel.status() == WheelStatus.TAKEN) {
                if (wheel.isSpinningNow()) {
                    return Mono.empty();
                }
                if (rnd.nextDouble() < exitPerTick) {
                    String owner = wheel.owner();
                    if (owner != null) {
                        wheel.exitIfOwner(owner);
                        world.noteExit(owner);
                        scheduleRest.accept(owner);
                        return broadcastViaSensors(wheel, client, new HamsterExit(owner, wheel.wheelId()));
                    }
                    return Mono.empty();
                } else {
                    int sec = rnd.nextInt(props.spinSecMin(), props.spinSecMax() + 1);
                    wheel.startSpinForMs(sec * 1000L);
                    return broadcastViaSensors(wheel, client, new WheelSpin(wheel.wheelId(), sec * 1000L));
                }
            }
            return Mono.empty();
        });

        return failures.then(wheelFlow);
    }
}
