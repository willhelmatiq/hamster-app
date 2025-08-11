package com.hamsterhub.simulator.service;

import com.hamsterhub.simulator.config.SimulatorConfig;
import reactor.core.publisher.Mono;

public interface SimulatorService {
    Mono<Void> updateConfig(SimulatorConfig config);
    SimulatorConfig currentRuntime();
}
