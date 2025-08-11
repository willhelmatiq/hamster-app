package com.hamsterhub.tracker.controller;

import com.hamsterhub.tracker.ReportGenerator;
import com.hamsterhub.tracker.model.DailyReport;
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

    private final ReportGenerator report;

    public ReportController(ReportGenerator report) {
        this.report = report;
    }

    @GetMapping("/daily")
    public Mono<DailyReport> daily(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Mono.fromSupplier(() -> report.generate(date));
    }
}
