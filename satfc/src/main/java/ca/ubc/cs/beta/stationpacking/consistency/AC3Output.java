package ca.ubc.cs.beta.stationpacking.consistency;

import java.util.Map;
import java.util.Set;

import lombok.Data;
import ca.ubc.cs.beta.stationpacking.base.Station;

/**
* Created by newmanne on 10/06/15.
*/
@Data
public class AC3Output {
    private boolean timedOut = false;
    private boolean noSolution = false;
    private final Map<Station, Set<Integer>> reducedDomains;
    private int numReducedChannels = 0;
}
