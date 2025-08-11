package com.hamsterhub.tracker.engine;

import com.hamsterhub.tracker.model.DailyReport;

import java.time.LocalDate;

public interface ReportGenerator {
    DailyReport generate(LocalDate date);
}
