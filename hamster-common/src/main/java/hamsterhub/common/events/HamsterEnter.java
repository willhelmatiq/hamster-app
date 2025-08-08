package hamsterhub.common.events;

public record HamsterEnter(String hamsterId, String wheelId) implements HamsterEvent {
}

