package com.hamsterhub.tracker.service;

import com.hamsterhub.tracker.engine.ReportGenerator;
import com.hamsterhub.tracker.model.DailyReport;
import com.hamsterhub.tracker.repository.DailyStatsRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class DailyReportService {
    private final ReportGenerator inMemoryGenerator;
    private final DailyStatsRepository repo;
    private final ZoneId zone;

    public DailyReportService(ReportGenerator gen, DailyStatsRepository repo, ZoneId zone) {
        this.inMemoryGenerator = gen;
        this.repo = repo;
        this.zone = zone;
    }

    public Mono<DailyReport> findDaily(LocalDate date) {
        LocalDate today = LocalDate.now(zone);
        if (date.isBefore(today)) {
            return Mono.fromCallable(() -> repo.loadReport(date))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(opt -> opt.map(Mono::just)
                            .orElseGet(() -> Mono.fromSupplier(() -> inMemoryGenerator.generate(date))));
        }
        return Mono.fromSupplier(() -> inMemoryGenerator.generate(date));
    }
}