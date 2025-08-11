package com.hamsterhub.simulator.model;

import com.hamsterhub.simulator.statuses.WheelStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class Wheel {
    private final String wheelId;
    private final List<Sensor> sensors;
    private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
    private final AtomicReference<String> occupiedBy = new AtomicReference<>(null);
    private final AtomicLong spinUntilEpochMs = new AtomicLong(0);
    private volatile WheelStatus status = WheelStatus.FREE;

    public Wheel(String wheelId, List<Sensor> sensors) {
        this.wheelId = wheelId;
        this.sensors = new ArrayList<>(sensors);
    }

    public String wheelId() {
        return wheelId;
    }

    public WheelStatus status() {
        return status;
    }

    public List<Sensor> sensors() {
        return sensors;
    }

    // CAS — Compare-And-Set: атомарно садим хомяка
    public boolean tryEnter(String hamsterId) {
        if (occupiedBy.compareAndSet(null, hamsterId)) {
            status = WheelStatus.TAKEN;
            return true;
        }
        return false;
    }

    public void addSensor(Sensor s) {
        rw.writeLock().lock();
        try {
            sensors.add(s);
        } finally {
            rw.writeLock().unlock();
        }
    }

    public boolean removeSensorById(String id) {
        rw.writeLock().lock();
        try {
            for (int i = sensors.size() - 1; i >= 0; i--)
                if (sensors.get(i).id().equals(id)) {
                    sensors.remove(i);
                    return true;
                }
            return false;
        } finally {
            rw.writeLock().unlock();
        }
    }

    public Sensor removeLastSensor() {
        rw.writeLock().lock();
        try {
            int n = sensors.size();
            if (n == 0) return null;
            return sensors.remove(n - 1);
        } finally {
            rw.writeLock().unlock();
        }
    }

    public List<Sensor> sensorSnapshot() {
        rw.readLock().lock();
        try {
            return List.copyOf(sensors);
        } finally {
            rw.readLock().unlock();
        }
    }

    public String owner() {
        return occupiedBy.get();
    }

    public void exitIfOwner(String hamsterId) {
        if (occupiedBy.compareAndSet(hamsterId, null)) status = WheelStatus.FREE;
    }

    public boolean isSpinningNow() {
        return System.currentTimeMillis() < spinUntilEpochMs.get();
    }

    public void startSpinForMs(long durationMs) {
        long now = System.currentTimeMillis();
        spinUntilEpochMs.set(now + Math.max(0L, durationMs));
    }

    public void stopSpinNow() {
        spinUntilEpochMs.set(0L);
    }

}
