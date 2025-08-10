package com.hamsterhub.simulator.entity;

import com.hamsterhub.simulator.statuses.HamsterStatus;

public final class Hamster {
    private final String id;
    private HamsterStatus status;

    public Hamster(String id, HamsterStatus status) { this.id = id; this.status = status; }
    public String id() { return id; }
    public HamsterStatus status() { return status; }
    public void status(HamsterStatus s) { this.status = s; }
}
