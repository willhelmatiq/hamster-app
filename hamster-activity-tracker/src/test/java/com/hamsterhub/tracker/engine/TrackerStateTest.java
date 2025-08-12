
package com.hamsterhub.tracker.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrackerStateTest {

    @Test
    void shouldAcceptSpin_dedupsWithinWindow() {
        TrackerState state = new TrackerState();
        long t0 = 1_000_000L;

        boolean a = state.shouldAcceptSpin("wheel-1", 5000L, t0, 250L);
        boolean b = state.shouldAcceptSpin("wheel-1", 5000L, t0 + 100L, 250L);
        boolean c = state.shouldAcceptSpin("wheel-1", 5000L, t0 + 300L, 250L);

        assertThat(a).isTrue();
        assertThat(b).isFalse();
        assertThat(c).isTrue();
    }

    @Test
    void hamsterLastEvent_shouldBeMonotonic() {
        TrackerState state = new TrackerState();
        state.updateHamsterLastEvent("h1", 2000L);
        state.updateHamsterLastEvent("h1", 1000L);

        Long seen = state.hamstersLastSeen().get("h1");
        assertThat(seen).isEqualTo(2000L);
    }

    @Test
    void sensorLastEvent_shouldBeMonotonic() {
        TrackerState state = new TrackerState();
        state.updateSensorLastEvent("s1", 3000L);
        state.updateSensorLastEvent("s1", 1000L);

        Long seen = state.sensorsLastSeen().get("s1");
        assertThat(seen).isEqualTo(3000L);
    }
}
