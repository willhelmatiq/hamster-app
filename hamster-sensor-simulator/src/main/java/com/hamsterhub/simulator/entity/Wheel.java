package com.hamsterhub.simulator.entity;

import com.hamsterhub.simulator.statuses.SensorStatus;
import com.hamsterhub.simulator.statuses.WheelStatus;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class Wheel {
    private final String wheelId;
    private final List<Sensor> sensors;
    private final AtomicReference<String> occupiedBy = new AtomicReference<>(null);
    private volatile WheelStatus status = WheelStatus.FREE;

    public Wheel(String wheelId, List<Sensor> sensors) {
        this.wheelId = wheelId;
        this.sensors = List.copyOf(sensors);
    }

    public String wheelId() { return wheelId; }
    public WheelStatus status() { return status; }
    public List<Sensor> sensors() { return sensors; }

    // CAS — Compare-And-Set: атомарно садим хомяка
    public boolean tryEnter(String hamsterId) {
        if (occupiedBy.compareAndSet(null, hamsterId)) {
            status = WheelStatus.TAKEN;
            return true;
        }
        return false;
    }

    public String owner() { return occupiedBy.get(); }

    public void exitIfOwner(String hamsterId) {
        if (occupiedBy.compareAndSet(hamsterId, null)) status = WheelStatus.FREE;
    }

}
