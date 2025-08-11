package com.hamsterhub.tracker;

import com.hamsterhub.tracker.model.EventWrapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class EventBus {
    private final Sinks.Many<EventWrapper> sink = Sinks.many().multicast().onBackpressureBuffer();
    private static final Sinks.EmitFailureHandler RETRY_NON_SERIALIZED =
            (signalType, emitResult) -> emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED;

    public Sinks.Many<EventWrapper> sink() {
        return sink;
    }

    public void emit(EventWrapper event) {
        sink.emitNext(event, RETRY_NON_SERIALIZED);
    }

    public Flux<EventWrapper> flux() {
        return sink.asFlux();
    }
}