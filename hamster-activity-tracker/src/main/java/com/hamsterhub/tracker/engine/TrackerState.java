package com.hamsterhub.tracker.engine;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Component
public class TrackerState {

    static final ZoneId ZONE = ZoneId.systemDefault();
    private final Map<String, ConcurrentHashMap<Long, Long>> wheelDeduplicationMap = new ConcurrentHashMap<>();

    // map со статистикой
    private final Map<LocalDate, Map<String, DayStats>> daily = new ConcurrentHashMap<>();

    private final Map<String, String> wheelWithHamster = new ConcurrentHashMap<>();
    private final Map<String, Long> sensorLastEvent = new ConcurrentHashMap<>();
    private final Map<String, Long> hamsterLastEvent = new ConcurrentHashMap<>();

    DayStats statsFor(LocalDate date, String hamsterId) {
        return daily.computeIfAbsent(date, d -> new ConcurrentHashMap<>())
                .computeIfAbsent(hamsterId, id -> new DayStats());
    }

    Map<String, DayStats> getStatsForDate(LocalDate date) {
        return daily.getOrDefault(date, Map.of());
    }

    void occupyWheel(String wheelId, String hamsterId) {
        wheelWithHamster.put(wheelId, hamsterId);
    }

    void releaseWheel(String wheelId, String hamsterId) {
        wheelWithHamster.compute(wheelId, (w, h) -> (Objects.equals(h, hamsterId) ? null : h));
    }

    Optional<String> getWheelHamster(String wheelId) {
        return Optional.ofNullable(wheelWithHamster.get(wheelId));
    }

    void updateHamsterLastEvent(String hamsterId, long tsMs) {
        hamsterLastEvent.merge(hamsterId, tsMs, Math::max);
    }

    void updateSensorLastEvent(String sensorId, long tsMs) {
        sensorLastEvent.merge(sensorId, tsMs, Math::max);
    }

    Map<String, Long> hamstersLastSeen() {
        return hamsterLastEvent;
    }

    Map<String, Long> sensorsLastSeen() {
        return sensorLastEvent;
    }

    boolean shouldAcceptSpin(String wheelId, long durationMs, long tsMs, long windowMs) {
        var perWheel = wheelDeduplicationMap.computeIfAbsent(wheelId, k -> new ConcurrentHashMap<>());
        return perWheel.compute(durationMs, (dur, lastTs) -> {
            if (lastTs != null && Math.abs(tsMs - lastTs) <= windowMs) {
                return lastTs;
            }
            return tsMs;
        }) == tsMs;
    }

    void removeDay(LocalDate day) {
        daily.remove(day);
    }

    static final class DayStats {
        private final LongAdder totalRounds = new LongAdder();

        void addRounds(int rounds) {
            if (rounds > 0) {
                totalRounds.add(rounds);
            }
        }

        int totalRounds() {
            return totalRounds.intValue();
        }
    }
}
