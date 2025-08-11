package com.hamsterhub.simulator.controller;

import com.hamsterhub.simulator.service.SimulatorService;
import com.hamsterhub.simulator.config.SimulatorConfig;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/simulator")
public class ConfigController {

    private final SimulatorService simulator;

    public ConfigController(SimulatorService simulator) {
        this.simulator = simulator;
    }

    @PostMapping("/config")
    public Mono<Void> configure(@Valid @RequestBody SimulatorConfig config) {
        return simulator.updateConfig(config);
    }

    @GetMapping("/config")
    public Mono<SimulatorConfig> current() {
        return Mono.just(simulator.currentRuntime());
    }
}

