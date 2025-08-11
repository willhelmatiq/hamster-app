package com.hamsterhub.tracker.service;


import com.hamsterhub.tracker.EventBus;
import com.hamsterhub.tracker.model.EventWrapper;
import hamsterhub.common.events.HamsterEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class EventService {
    private final EventBus bus;

    public EventService(EventBus bus) {
        this.bus = bus;
    }

    public Mono<Void> processEvent(HamsterEvent event, String sensorId) {
        bus.emit(new EventWrapper(event, sensorId, System.currentTimeMillis()));
        return Mono.empty();
    }
}

