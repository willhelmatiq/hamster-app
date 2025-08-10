package com.hamsterhub.simulator;

import com.hamsterhub.simulator.config.SimulatorConfig;
import reactor.core.publisher.Mono;

public interface SimulatorService {
    Mono<Void> updateConfig(SimulatorConfig config);
}
