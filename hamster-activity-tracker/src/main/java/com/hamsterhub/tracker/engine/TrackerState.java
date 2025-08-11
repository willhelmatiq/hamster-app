package com.hamsterhub.tracker.engine;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
class TrackerState {

    static final ZoneId ZONE = ZoneId.systemDefault();
    private final Map<String, ConcurrentHashMap<Long, Long>> wheelDeduplicationMap = new ConcurrentHashMap<>();

    // date -> hamsterId -> stats
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
        hamsterLastEvent.put(hamsterId, tsMs);
    }

    void updateSensorLastEvent(String sensorId, long tsMs) {
        sensorLastEvent.put(sensorId, tsMs);
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

    void cleanupDeduplicationMap(long retainMs) {
        long threshold = System.currentTimeMillis() - retainMs;
        wheelDeduplicationMap.values().forEach(map ->
                map.entrySet().removeIf(e -> e.getValue() < threshold)
        );
    }

    static final class DayStats {
        private int totalRounds;
        private volatile long lastActiveEpochMs;

        synchronized void addRounds(int rounds, long tsMs) {
            if (rounds > 0) {
                totalRounds += rounds;
            }
            lastActiveEpochMs = Math.max(lastActiveEpochMs, tsMs);
        }

        int totalRounds() {
            return totalRounds;
        }

        long lastActiveEpochMs() {
            return lastActiveEpochMs;
        }
    }
}
