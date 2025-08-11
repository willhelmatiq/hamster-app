package com.hamsterhub.tracker;


import hamsterhub.common.events.HamsterEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public Mono<Void> receiveEvent(
            @RequestBody HamsterEvent event,
            @RequestHeader(name = "X-Sensor-Id", required = false) String sensorId
    ) {
        return eventService.processEvent(event, sensorId);
    }
}

