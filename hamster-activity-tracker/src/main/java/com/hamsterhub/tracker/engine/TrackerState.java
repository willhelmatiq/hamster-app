package com.hamsterhub.tracker.engine;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TrackerState {

    static final ZoneId ZONE = ZoneId.systemDefault();
    private final Map<String, ConcurrentHashMap<Long, Long>> wheelDeduplicationMap = new ConcurrentHashMap<>();
    private final Map<String, Long> enterDeduplicationMap = new ConcurrentHashMap<>();
    private final Map<String, Long> exitDeduplicationMap = new ConcurrentHashMap<>();

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

    long canonicalizeEnterTs(String wheelId, String hamsterId, long ts, long windowMs) {
        String key = wheelId + "|" + hamsterId;
        return enterDeduplicationMap.compute(key, (k, prev) -> {
            if (prev == null || Math.abs(ts - prev) > windowMs) return ts; // новое окно
            return Math.min(prev, ts); // в окне — держим самый ранний
        });
    }

    long canonicalizeExitTs(String wheelId, String hamsterId, long ts, long windowMs) {
        String key = wheelId + "|" + hamsterId;
        return exitDeduplicationMap.compute(key, (k, prev) -> {
            if (prev == null || Math.abs(ts - prev) > windowMs) return ts;
            return Math.min(prev, ts);
        });
    }

    void cleanupDeduplicationMap(long retainMs) {
        long threshold = System.currentTimeMillis() - retainMs;
        wheelDeduplicationMap.values().forEach(map ->
                map.entrySet().removeIf(e -> e.getValue() < threshold)
        );
    }

    void cleanupEnterExitDedup(long retainMs) {
        long threshold = System.currentTimeMillis() - retainMs;
        enterDeduplicationMap.values().removeIf(ts -> ts < threshold);
        exitDeduplicationMap.values().removeIf(ts -> ts < threshold);
    }

    static final class DayStats {
        private int totalRounds;

        synchronized void addRounds(int rounds) {
            if (rounds > 0) {
                totalRounds += rounds;
            }
        }

        int totalRounds() {
            return totalRounds;
        }
    }
}
