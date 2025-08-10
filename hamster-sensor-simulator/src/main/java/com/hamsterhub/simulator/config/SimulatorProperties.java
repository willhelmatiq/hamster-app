package com.hamsterhub.simulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "simulator")
public record SimulatorProperties(
        Defaults defaults,     // <- сюда приедут стартовые counts
        double enterProb,
        double failProb,
        int spinSecMin,
        int spinSecMax,
        int restSecAfterEscape
) {
    public record Defaults(int hamsterCount, int sensorCount) {}
}
