package hamsterhub.common.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = WheelSpin.class, name = "WheelSpin"),
        @JsonSubTypes.Type(value = HamsterEnter.class, name = "HamsterEnter"),
        @JsonSubTypes.Type(value = HamsterExit.class, name = "HamsterExit"),
        @JsonSubTypes.Type(value = SensorFailure.class, name = "SensorFailure")
})
public sealed interface HamsterEvent
        permits WheelSpin, HamsterEnter, HamsterExit, SensorFailure {
}




