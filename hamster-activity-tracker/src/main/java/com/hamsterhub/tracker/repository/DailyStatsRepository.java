package com.hamsterhub.tracker.repository;

import com.hamsterhub.tracker.model.DailyReport;
import com.hamsterhub.tracker.model.HamsterStats;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class DailyStatsRepository {
    private final JdbcTemplate jdbc;

    DailyStatsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<DailyReport> loadReport(LocalDate day) {
        var rows = jdbc.query("""
            SELECT hamster_id, total_rounds, is_active
            FROM daily_stats
            WHERE date = ?
        """, (rs, i) -> new DbRow(
                rs.getString("hamster_id"),
                rs.getInt("total_rounds"),
                rs.getBoolean("is_active")
        ), Date.valueOf(day));

        if (rows.isEmpty()) return Optional.empty();

        Map<String, HamsterStats> map = rows.stream()
                .collect(Collectors.toMap(
                        r -> r.hamsterId,
                        r -> new HamsterStats(r.hamsterId, r.totalRounds, r.isActive)
                ));
        return Optional.of(new DailyReport(day, map));
    }

    private record DbRow(String hamsterId, int totalRounds, boolean isActive) {}
}