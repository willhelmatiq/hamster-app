package com.hamsterhub.simulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "simulator")
public record SimulatorProperties(
        Defaults defaults,
        int wheelCount,
        int tickSeconds,
        double enterPerMinute,
        double failPerMinute,
        double temporaryFailShare,
        int spinSecMin,
        int spinSecMax,
        int restSecAfterEscape
) {
    public record Defaults(int hamsterCount, int sensorCount) {
    }
}
