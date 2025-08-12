package com.hamsterhub.tracker.controller;

import com.hamsterhub.tracker.engine.ReportGenerator;
import com.hamsterhub.tracker.model.DailyReport;
import com.hamsterhub.tracker.service.DailyReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequestMapping("/report")
public class ReportController {

    private final DailyReportService dailyReportService;

    public ReportController(DailyReportService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    @GetMapping("/daily")
    public Mono<DailyReport> daily(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dailyReportService.findDaily(date);
    }
}
