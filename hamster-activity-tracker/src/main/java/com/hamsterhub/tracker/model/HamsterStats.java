package com.hamsterhub.tracker.model;

public class HamsterStats {
    private final String hamsterId;
    private int totalRounds;
    private boolean active;

    public HamsterStats(String hamsterId) {
        this.hamsterId = hamsterId;
    }

    public String getHamsterId() {
        return hamsterId;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public boolean isActive() {
        return active;
    }

    public void addRounds(int rounds) {
        this.totalRounds += rounds;
        this.active = this.totalRounds > 10;
    }
}
