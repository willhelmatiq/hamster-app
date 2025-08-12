
package com.hamsterhub.tracker.engine;

import com.hamsterhub.tracker.model.DailyReport;
import com.hamsterhub.tracker.model.HamsterStats;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReportGeneratorImplTest {

    @Test
    void dailyReportAggregatesRoundsAndSetsActive() {
        TrackerState state = new TrackerState();
        ReportGeneratorImpl generator = new ReportGeneratorImpl(state);

        long ts = System.currentTimeMillis();
        LocalDate date = Instant.ofEpochMilli(ts).atZone(TrackerState.ZONE).toLocalDate();

        state.statsFor(date, "ham-1").addRounds(7);
        state.statsFor(date, "ham-1").addRounds(5);
        state.statsFor(date, "ham-2").addRounds(9);

        DailyReport report = generator.generate(date);
        HamsterStats hamsterStatsActive = report.getHamsterStats().get("ham-1");
        HamsterStats hamsterStatsNotActive = report.getHamsterStats().get("ham-2");

        assertThat(hamsterStatsActive).isNotNull();
        assertThat(hamsterStatsActive.getTotalRounds()).isEqualTo(12);
        assertThat(hamsterStatsActive.isActive()).isTrue();

        assertThat(hamsterStatsNotActive).isNotNull();
        assertThat(hamsterStatsNotActive.getTotalRounds()).isEqualTo(9);
        assertThat(hamsterStatsNotActive.isActive()).isFalse();
    }
}
