package com.hamsterhub.tracker.engine;

import com.hamsterhub.tracker.config.TrackerProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
class DailyStatsExporter {

    private final TrackerState state;
    private final JdbcTemplate jdbc;
    private final TrackerProperties props;

    DailyStatsExporter(TrackerState state, JdbcTemplate jdbc, TrackerProperties props) {
        this.state = state;
        this.jdbc = jdbc;
        this.props = props;
    }

    @Transactional
    void exportDay(LocalDate day) {
        Map<String, TrackerState.DayStats> map = state.getStatsForDate(day);
        if (map.isEmpty()) {
            return;
        }

        List<Object[]> batch = new ArrayList<>(map.size());
        for (var stats : map.entrySet()) {
            String hamsterId = stats.getKey();
            int total = stats.getValue().totalRounds();
            boolean active = total > props.activeThreshold();
            batch.add(new Object[]{day, hamsterId, total, active});
        }

        int[] res = jdbc.batchUpdate("""
            INSERT INTO daily_stats(date, hamster_id, total_rounds, is_active)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (date, hamster_id) DO UPDATE
            SET total_rounds = EXCLUDED.total_rounds,
                is_active    = EXCLUDED.is_active,
                updated_at   = now()
        """, batch);

        state.removeDay(day);
    }

    // Экспорт каждый день в указанное время;
    @Scheduled(cron = "${tracker.export-cron}", zone = "${tracker.zone-id}")
    void dailyExport() {
        ZoneId zone = (props.zoneId() != null && !props.zoneId().isBlank())
                ? ZoneId.of(props.zoneId())
                : ZoneId.systemDefault();

        LocalDate today = LocalDate.now(zone);
        for (int i = 1; i <= Math.max(1, props.exportDaysBack()); i++) {
            exportDay(today.minusDays(i));
        }
    }
}
