package com.hamsterhub.tracker.model;

import java.time.LocalDate;
import java.util.Map;

public class DailyReport {
    private final LocalDate date;
    private final Map<String, HamsterStats> hamsterStats;

    public DailyReport(LocalDate date, Map<String, HamsterStats> hamsterStats) {
        this.date = date;
        this.hamsterStats = hamsterStats;
    }

    public LocalDate getDate() {
        return date;
    }

    public Map<String, HamsterStats> getHamsterStats() {
        return hamsterStats;
    }
}
