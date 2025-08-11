package com.hamsterhub.tracker;

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

    static final class DayStats {
        private int totalRounds;
        private volatile long lastActiveEpochMs;

        synchronized void addRounds(int rounds, long tsMs) {
            if (rounds > 0) totalRounds += rounds;
            lastActiveEpochMs = Math.max(lastActiveEpochMs, tsMs);
        }
        int totalRounds() { return totalRounds; }
        long lastActiveEpochMs() { return lastActiveEpochMs; }
    }

    // date -> hamsterId -> stats
    private final Map<LocalDate, Map<String, DayStats>> daily = new ConcurrentHashMap<>();

    // wheel occupancy & last wheel activity
    private final Map<String, String> wheelOccupancy = new ConcurrentHashMap<>();
    private final Map<String, Long> wheelLastEvent = new ConcurrentHashMap<>();

    // «последний раз видели»
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
        wheelOccupancy.put(wheelId, hamsterId);
        touchWheel(wheelId);
    }

    void releaseWheel(String wheelId, String hamsterId) {
        wheelOccupancy.compute(wheelId, (w, h) -> (Objects.equals(h, hamsterId) ? null : h));
        touchWheel(wheelId);
    }

    Optional<String> occupant(String wheelId) {
        return Optional.ofNullable(wheelOccupancy.get(wheelId));
    }

    void touchWheel(String wheelId) {
        wheelLastEvent.put(wheelId, System.currentTimeMillis());
    }
    void touchHamster(String hamsterId, long tsMs) { hamsterLastEvent.put(hamsterId, tsMs); }
    void touchSensor(String sensorId, long tsMs)  { sensorLastEvent.put(sensorId, tsMs); }

    Map<String, Long> sensorsLastSeen()  { return sensorLastEvent; }
    Map<String, Long> hamstersLastSeen() { return hamsterLastEvent; }
}
