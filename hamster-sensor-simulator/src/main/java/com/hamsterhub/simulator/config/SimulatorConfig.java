package com.hamsterhub.simulator.config;

public record SimulatorConfig(int hamsterCount, int sensorCount) {
    public SimulatorConfig {
        if (hamsterCount < 1 || hamsterCount > 10_000)
            throw new IllegalArgumentException("hamsterCount should be from 1 to 10000");
        if (sensorCount < 1 || sensorCount > 10_000)
            throw new IllegalArgumentException("sensorCount should be from 1 to 10000");
    }
}
