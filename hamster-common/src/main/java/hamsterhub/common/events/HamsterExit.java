package hamsterhub.common.events;

public record HamsterExit(String hamsterId, String wheelId) implements HamsterEvent {
}

