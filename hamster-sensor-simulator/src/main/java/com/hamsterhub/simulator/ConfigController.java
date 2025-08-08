package com.hamsterhub.simulator;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/simulator")
public class ConfigController {

    private final SimulatorService simulator;

    public ConfigController(SimulatorService simulator) {
        this.simulator = simulator;
    }

    @PostMapping("/config")
    public Mono<Void> configure(@RequestBody SimulatorConfig config) {
        return simulator.updateConfig(config);
    }
}

