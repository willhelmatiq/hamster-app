
package com.hamsterhub.tracker.engine;

import com.hamsterhub.tracker.config.TrackerProperties;
import com.hamsterhub.tracker.model.EventWrapper;
import hamsterhub.common.events.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EventProcessorTest {

    private EventBus bus;
    private TrackerState state;
    private EventProcessor processor;
    private TrackerProperties props;

    @BeforeEach
    void setUp() {
        bus = new EventBus();
        state = new TrackerState();
        props = new TrackerProperties(
                1,           // workers
                250L,        // deduplicationWindowMs
                60 * 60 * 1000L, // hamsterInactivityMs
                30 * 60 * 1000L, // sensorInactivityMs
                60 * 60 * 1000L  // cleanupIntervalMs
        );
        processor = new EventProcessor(bus, state, props);
        processor.start();
    }

    @AfterEach
    void tearDown() {
        processor.stop();
    }

    @Test
    void spinIsCountedOnEventDate_notOnNow() throws Exception {
        String wheel = "w-1";
        String hamster = "h-1";

        long tEnter = 1_720_000_000_000L;
        bus.emit(new EventWrapper(new HamsterEnter(hamster, wheel), "s-1", tEnter));

        long tSpin1 = tEnter + 5_000L;
        bus.emit(new EventWrapper(new WheelSpin(wheel, 15_000L), "s-1", tSpin1));

        long tSpin2 = tEnter + 24*60*60*1000L + 1_000L;
        bus.emit(new EventWrapper(new WheelSpin(wheel, 15_000L), "s-1", tSpin2));

        Thread.sleep(200);

        LocalDate d1 = Instant.ofEpochMilli(tSpin1).atZone(TrackerState.ZONE).toLocalDate();
        LocalDate d2 = Instant.ofEpochMilli(tSpin2).atZone(TrackerState.ZONE).toLocalDate();

        assertThat(state.getStatsForDate(d1).get(hamster).totalRounds()).isEqualTo(3);
        assertThat(state.getStatsForDate(d2).get(hamster).totalRounds()).isEqualTo(3);
    }

    @Test
    void duplicateSpinWithinWindow_isIgnored() throws Exception {
        String wheel = "w-2";
        String hamster = "h-2";

        long tEnter = 1_730_000_000_000L;
        bus.emit(new EventWrapper(new HamsterEnter(hamster, wheel), "s-2", tEnter));

        long t1 = tEnter + 10_000L;
        long t2 = t1 + 100L; // within 250ms
        bus.emit(new EventWrapper(new WheelSpin(wheel, 10_000L), "s-2", t1)); // 2 rounds
        bus.emit(new EventWrapper(new WheelSpin(wheel, 10_000L), "s-2", t2)); // duplicate

        Thread.sleep(100);

        LocalDate d = Instant.ofEpochMilli(t1).atZone(TrackerState.ZONE).toLocalDate();
        assertThat(state.getStatsForDate(d).get(hamster).totalRounds()).isEqualTo(2);
    }

    @Test
    void exitFromAnotherHamster_isIgnored() throws Exception {
        String wheel = "w-3";
        String h1 = "h-3a";
        String h2 = "h-3b";

        long t = 1_740_000_000_000L;
        bus.emit(new EventWrapper(new HamsterEnter(h1, wheel), "s-3", t));

        bus.emit(new EventWrapper(new HamsterExit(h2, wheel), "s-3", t + 5_000L));

        Thread.sleep(100);

        Optional<String> occ = state.getWheelHamster(wheel);
        assertThat(occ).contains(h1);
    }
}
