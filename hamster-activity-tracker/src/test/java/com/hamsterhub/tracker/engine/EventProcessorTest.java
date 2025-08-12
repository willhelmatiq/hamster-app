
package com.hamsterhub.tracker.engine;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hamsterhub.tracker.config.TrackerProperties;
import com.hamsterhub.tracker.model.EventWrapper;
import hamsterhub.common.events.HamsterEnter;
import hamsterhub.common.events.HamsterExit;
import hamsterhub.common.events.WheelSpin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
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
    void spinIsCountedOnEventDate() throws Exception {
        String wheel = "wheel-1";
        String hamster = "ham-1";

        long tEnter = 1_720_000_000_000L;
        bus.emit(new EventWrapper(new HamsterEnter(hamster, wheel), "sensor-1", tEnter));

        long tSpin1 = tEnter + 5_000L;
        bus.emit(new EventWrapper(new WheelSpin(wheel, 15_000L), "sensor-1", tSpin1));

        long tSpin2 = tEnter + 24*60*60*1000L + 1_000L;
        bus.emit(new EventWrapper(new WheelSpin(wheel, 14_000L), "sensor-1", tSpin2));

        Thread.sleep(200);

        LocalDate d1 = Instant.ofEpochMilli(tSpin1).atZone(TrackerState.ZONE).toLocalDate();
        LocalDate d2 = Instant.ofEpochMilli(tSpin2).atZone(TrackerState.ZONE).toLocalDate();

        assertThat(state.getStatsForDate(d1).get(hamster).totalRounds()).isEqualTo(3);
        assertThat(state.getStatsForDate(d2).get(hamster).totalRounds()).isEqualTo(2);
    }

    @Test
    void duplicateSpinForSameWheelWithinWindow_isIgnored() throws Exception {
        String wheel = "wheel-2";
        String hamster = "ham-2";

        long tEnter = 1_730_000_000_000L;
        bus.emit(new EventWrapper(new HamsterEnter(hamster, wheel), "sensor-2", tEnter));

        long t1 = tEnter + 10_000L;
        long t2 = t1 + 100L; // within 250ms
        bus.emit(new EventWrapper(new WheelSpin(wheel, 10_000L), "sensor-1", t1));
        bus.emit(new EventWrapper(new WheelSpin(wheel, 10_000L), "sensor-2", t2));

        Thread.sleep(100);

        LocalDate d = Instant.ofEpochMilli(t1).atZone(TrackerState.ZONE).toLocalDate();
        assertThat(state.getStatsForDate(d).get(hamster).totalRounds()).isEqualTo(2);
    }

    @Test
    void exitFromAnotherHamster_isIgnored() throws Exception {
        String wheel = "wheel-3";
        String h1 = "ham-3a";
        String h2 = "ham-3b";

        long t = 1_740_000_000_000L;
        bus.emit(new EventWrapper(new HamsterEnter(h1, wheel), "sensor-3", t));

        bus.emit(new EventWrapper(new HamsterExit(h2, wheel), "sensor-3", t + 5_000L));

        Thread.sleep(100);

        Optional<String> wheelHamster = state.getWheelHamster(wheel);
        assertThat(wheelHamster).contains(h1);
    }

    @Test
    void negativeDuration_isIgnored_andWarnLogged(CapturedOutput output) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(EventProcessor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();

        logger.addAppender(appender);

        String wheel = "wheel-1";
        String hamster = "ham-1";

        long tEnter = 1_760_000_000_000L;
        bus.emit(new EventWrapper(new HamsterEnter(hamster, wheel), "sensor-1", tEnter));

        long tSpinNeg = tEnter + 10_000L;
        bus.emit(new EventWrapper(new WheelSpin(wheel, -10_000L), "sensor-1", tSpinNeg));

        Thread.sleep(100);

        LocalDate d = Instant.ofEpochMilli(tSpinNeg).atZone(TrackerState.ZONE).toLocalDate();
        assertThat(state.getStatsForDate(d).get(hamster).totalRounds()).isEqualTo(0);

        assertThat(appender.list)
                .anySatisfy(ev -> {
                    assertThat(ev.getLevel()).isEqualTo(Level.WARN);
                    assertThat(ev.getFormattedMessage())
                            .isEqualTo("Negative duration: wheel=wheel-1 durationMs=-10000");
                });
        logger.detachAppender(appender);
    }
}
