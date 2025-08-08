package hamsterhub.common.events;

public record WheelSpin(String wheelId, long durationMs) implements HamsterEvent {
}

