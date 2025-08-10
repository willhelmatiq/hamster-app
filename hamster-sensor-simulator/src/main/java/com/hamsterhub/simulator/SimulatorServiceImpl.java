package com.hamsterhub.simulator;

import com.hamsterhub.simulator.config.RuntimeConfig;
import com.hamsterhub.simulator.config.SimulatorConfig;
import com.hamsterhub.simulator.config.SimulatorProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class SimulatorServiceImpl implements SimulatorService {

    private final AtomicReference<RuntimeConfig> runtime;
    private final SimulationEngine engine;           // чтобы пересобирать мир
    private final SimulatorProperties props;         // если где-то пригодится

    public SimulatorServiceImpl(SimulatorProperties props, SimulationEngine engine) {
        this.props = props;
        this.engine = engine;
        this.runtime = new AtomicReference<>(
                new RuntimeConfig(props.defaults().hamsterCount(), props.defaults().sensorCount())
        );
    }

    @Override
    public Mono<Void> updateConfig(SimulatorConfig cfg) {
        return Mono.fromRunnable(() -> {
            runtime.set(new RuntimeConfig(cfg.hamsterCount(), cfg.sensorCount()));
            engine.rebuildWorld(currentRuntime()); // перестроить мир под новые counts
        });
    }

    public RuntimeConfig currentRuntime() {
        return runtime.get();
    }
}

