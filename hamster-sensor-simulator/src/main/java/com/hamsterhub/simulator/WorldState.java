package com.hamsterhub.simulator;

import com.hamsterhub.simulator.config.SimulatorProperties;
import com.hamsterhub.simulator.entity.Sensor;
import com.hamsterhub.simulator.entity.Wheel;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WorldState {

    private final AtomicInteger totalSensors = new AtomicInteger(0);
    private final AtomicInteger nextHamsterSeq = new AtomicInteger(0);
    private final AtomicInteger nextSensorSeq  = new AtomicInteger(0);

    private final List<Wheel> wheels;
    private final ConcurrentLinkedQueue<String> readyHamsters = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, Wheel> hamsterOnWheel = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<String> brokenSensors = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, Wheel> sensorToWheel = new ConcurrentHashMap<>();

    // один общий мьютекс на перестановки (простая модель)
    private final Object mutateLock = new Object();

    public WorldState(SimulatorProperties props) {
        int wheelsCount = Math.max(1, props.wheelCount());
        var tmp = new ArrayList<Wheel>(wheelsCount);
        for (int i = 0; i < wheelsCount; i++) {
            tmp.add(new Wheel("wheel-" + i, List.of()));
        }
        this.wheels = Collections.unmodifiableList(tmp);
    }

    public List<Wheel> wheels() { return wheels; }
    public Queue<String> readyHamsters() { return readyHamsters; }
    public void noteEnter(String hamsterId, Wheel w) { hamsterOnWheel.put(hamsterId, w); }
    public void noteExit(String hamsterId)         { hamsterOnWheel.remove(hamsterId); }

    // -------------------- ХОМЯКИ: приведение к целевому числу --------------------
    public void adjustTotalHamsters(int desiredHamsterCount) {
        synchronized (mutateLock) {
            int total = readyHamsters.size() + hamsterOnWheel.size();

            if (total < desiredHamsterCount) {
                for (int i = 0; i < desiredHamsterCount - total; i++) {
                    readyHamsters.add("ham-" + nextHamsterSeq.getAndIncrement());
                }
                return;
            }

            int needRemove = total - desiredHamsterCount;

            // 1) Сначала выпускаем из очереди
            while (needRemove > 0 && !readyHamsters.isEmpty()) {
                readyHamsters.poll();
                needRemove--;
            }
            // 2) Если ещё надо — снимаем владельцев с колёс (через мапу)
            if (needRemove > 0) {
                var it = hamsterOnWheel.entrySet().iterator();
                while (needRemove > 0 && it.hasNext()) {
                    var e = it.next();
                    String hamsterId = e.getKey();
                    Wheel wheel = e.getValue();

                    // проверим, что владелец актуален
                    if (hamsterId.equals(wheel.owner())) {
                        wheel.exitIfOwner(hamsterId); // приведёт колесо к FREE
                    }
                    it.remove(); // убрали из мапы -> хомяк «выпущен»
                    needRemove--;
                }
            }
        }
    }

    public void markSensorBroken(String sensorId) {
        if (sensorId != null) {
            brokenSensors.add(sensorId);
        }
    }
    // -------------------- ДАТЧИКИ: приведение к целевому числу --------------------
    public void adjustSensors(int newTotalSensorCount) {
        synchronized (mutateLock) {
            if (wheels.isEmpty()) {
                return;
            }
            final int wheelsSize = wheels.size();
            int current = totalSensors.get();
            if (current == newTotalSensorCount) {
                return;
            }

            if (current > newTotalSensorCount) {
                int needRemove = current - newTotalSensorCount;

                // 1) сперва удаляем те, что в очереди сломанных
                while (needRemove > 0) {
                    String sensorId = brokenSensors.poll();
                    if (sensorId == null) {
                        break;
                    }
                    if (removeSensorById(sensorId)) {
                        needRemove--;
                    }
                }

                // 2) если ещё нужно — снимаем «лишние» живые до целевых per-wheel
                if (needRemove > 0) {
                    int base = newTotalSensorCount / wheelsSize;
                    int extra = newTotalSensorCount % wheelsSize;

                    for (int i = 0; i < wheelsSize && needRemove > 0; i++) {
                        Wheel wheel = wheels.get(i);
                        int target = base + (i < extra ? 1 : 0);
                        int cur = wheel.sensorSnapshot().size(); // только чтобы знать текущее число
                        while (needRemove > 0 && cur > target) {
                            Sensor removed = wheel.removeLastSensor();
                            if (removed == null) {
                                break;
                            }
                            sensorToWheel.remove(removed.id());
                            totalSensors.decrementAndGet();
                            needRemove--;
                            cur--;
                        }
                    }
                }
                return;
            }

            // увеличение
            int needAdd = newTotalSensorCount - current;
            int limit = newTotalSensorCount / wheelsSize + 1;

            for (int i = 0; i < wheelsSize && needAdd > 0; i++) {
                Wheel wheel = wheels.get(i);
                int cur = wheel.sensorSnapshot().size();
                while (needAdd > 0 && cur < limit) {
                    Sensor s = new Sensor("sensor-" + nextSensorSeq.getAndIncrement());
                    addSensorToWheel(wheel, s);
                    cur++;
                    needAdd--;
                }
            }
        }
    }

    private void addSensorToWheel(Wheel wheel, Sensor s) {
        wheel.addSensor(s);
        sensorToWheel.put(s.id(), wheel);
        totalSensors.incrementAndGet();
    }

    private boolean removeSensorById(String sensorId) {
        Wheel wheel = sensorToWheel.remove(sensorId);
        if (wheel == null) {
            return false;
        }
        boolean removed = wheel.removeSensorById(sensorId);
        if (removed) {
            totalSensors.decrementAndGet();
        }
        return removed;
    }
}
