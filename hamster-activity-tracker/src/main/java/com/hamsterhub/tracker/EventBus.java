package com.hamsterhub.tracker;

import com.hamsterhub.tracker.model.EventWrapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

@Component
public class EventBus {
    private final Sinks.Many<EventWrapper> sink = Sinks.many().unicast().onBackpressureBuffer();

    public Sinks.Many<EventWrapper> sink() {
        return sink;
    }
}