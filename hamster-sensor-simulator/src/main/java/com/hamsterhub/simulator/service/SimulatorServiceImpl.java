package com.hamsterhub.simulator.service;

import com.hamsterhub.simulator.SimulationEngine;
import com.hamsterhub.simulator.config.SimulatorConfig;
import com.hamsterhub.simulator.config.SimulatorProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class SimulatorServiceImpl implements SimulatorService {

    private final AtomicReference<SimulatorConfig> runtime;
    private final SimulationEngine engine;           // чтобы пересобирать мир
    private final SimulatorProperties props;         // если где-то пригодится

    public SimulatorServiceImpl(SimulatorProperties props, SimulationEngine engine) {
        this.props = props;
        this.engine = engine;
        this.runtime = new AtomicReference<>(
                new SimulatorConfig(props.defaults().hamsterCount(), props.defaults().sensorCount())
        );
    }

    @Override
    public Mono<Void> updateConfig(SimulatorConfig cfg) {
        SimulatorConfig runtimeConfig = new SimulatorConfig(cfg.hamsterCount(), cfg.sensorCount());
        runtime.set(runtimeConfig);
        return engine.applyRuntime(cfg);
    }

    @Override
    public SimulatorConfig currentRuntime() {
        return runtime.get();
    }
}

