package com.hamsterhub.simulator;

import com.hamsterhub.simulator.entity.Wheel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class WorldState {

    // колёса (каждое содержит список Sensor)
    private final List<Wheel> wheels = new ArrayList<>();

    // очередь «готовых» хомяков
    private final ConcurrentLinkedQueue<String> readyHamsters = new ConcurrentLinkedQueue<>();

    public List<Wheel> wheels() { return wheels; }
    public Queue<String> readyHamsters() { return readyHamsters; }

    public void clear() {
        wheels.clear();
        readyHamsters.clear();
    }
}
