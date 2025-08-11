package com.hamsterhub.tracker.engine;

import com.hamsterhub.tracker.model.DailyReport;
import com.hamsterhub.tracker.model.HamsterStats;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ReportGeneratorImpl implements ReportGenerator {

    private final TrackerState state;

    public ReportGeneratorImpl(TrackerState state) {
        this.state = state;
    }

    @Override
    public DailyReport generate(LocalDate date) {
        Map<String, HamsterStats> map = state.getStatsForDate(date).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            HamsterStats hs = new HamsterStats(e.getKey());
                            hs.addRounds(e.getValue().totalRounds());
                            return hs;
                        }
                ));
        return new DailyReport(date, map);
    }
}
