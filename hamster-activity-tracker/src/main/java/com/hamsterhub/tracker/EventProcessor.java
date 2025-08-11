package com.hamsterhub.tracker;

import com.hamsterhub.tracker.config.TrackerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

@Component
public class EventProcessor {
    @Autowired
    private Disposable sub;

    public EventProcessor(EventBus bus, TrackerState state, TrackerProperties props) {
        this.sub = bus.sink().asFlux()
                .parallel(Math.max(1, props.workers()))
                .runOn(Schedulers.boundedElastic())
                .subscribe(env -> /* обработка env */, err -> {/* log */});
    }
}
