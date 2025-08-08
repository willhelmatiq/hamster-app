package com.hamsterhub.tracker;


import hamsterhub.common.events.HamsterEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class EventService {

    private final HamsterStateTracker tracker;
    private final AlertService alertService;

    public EventService(HamsterStateTracker tracker, AlertService alertService) {
        this.tracker = tracker;
        this.alertService = alertService;
    }

    public Mono<Void> processEvent(HamsterEvent event) {
        return tracker.handle(event)
                .flatMap(alertService::checkAlerts);
    }
}

