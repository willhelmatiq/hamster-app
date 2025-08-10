package com.hamsterhub.tracker;


import hamsterhub.common.events.HamsterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    public Mono<Void> processEvent(HamsterEvent event) {
        log.info("EVENT: {}", event);
        return Mono.empty();
    }
}

