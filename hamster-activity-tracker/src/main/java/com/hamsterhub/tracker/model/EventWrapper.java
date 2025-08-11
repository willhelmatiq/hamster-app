package com.hamsterhub.tracker.model;

import hamsterhub.common.events.HamsterEvent;

public record EventWrapper(
        HamsterEvent event,
        String sensorId,
        long receivedAt
) {}