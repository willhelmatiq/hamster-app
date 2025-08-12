
package com.hamsterhub.tracker.engine;

import com.hamsterhub.tracker.model.DailyReport;
import com.hamsterhub.tracker.model.HamsterStats;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReportGeneratorImplTest {

    @Test
    void dailyReportAggregatesRoundsAndSetsActiveOver10() {
        TrackerState state = new TrackerState();
        ReportGeneratorImpl generator = new ReportGeneratorImpl(state);

        long ts = System.currentTimeMillis();
        LocalDate date = Instant.ofEpochMilli(ts).atZone(TrackerState.ZONE).toLocalDate();

        state.statsFor(date, "ham-1").addRounds(7);
        state.statsFor(date, "ham-1").addRounds(5);

        DailyReport report = generator.generate(date);
        HamsterStats hs = report.getHamsterStats().get("ham-1");

        assertThat(hs).isNotNull();
        assertThat(hs.getTotalRounds()).isEqualTo(12);
        assertThat(hs.isActive()).isTrue();
    }
}
