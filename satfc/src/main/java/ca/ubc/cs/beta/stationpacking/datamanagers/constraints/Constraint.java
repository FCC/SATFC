package ca.ubc.cs.beta.stationpacking.datamanagers.constraints;

import lombok.Value;
import ca.ubc.cs.beta.stationpacking.base.Station;

/**
 * Representation of a binary constraint
 */
@Value
public class Constraint {
    private final Station source;
    private final Station target;
    private final int sourceChannel;
    private final int targetChannel;
}