package com.hamsterhub.simulator.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SimulatorConfig(
        @Min(1) @Max(10000) int hamsterCount,
        @Min(1) @Max(10000) int sensorCount
) {}
