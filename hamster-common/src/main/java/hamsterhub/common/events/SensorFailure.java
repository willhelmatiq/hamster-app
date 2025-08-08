package hamsterhub.common.events;

public record SensorFailure(String sensorId, int errorCode) implements HamsterEvent {
}

