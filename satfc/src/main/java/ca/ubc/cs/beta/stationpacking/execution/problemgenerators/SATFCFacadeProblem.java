package ca.ubc.cs.beta.stationpacking.execution.problemgenerators;

import java.util.Map;
import java.util.Set;

import lombok.Data;

/**
* Created by newmanne on 12/05/15.
*/
@Data
public class SATFCFacadeProblem {

    private final Set<Integer> stationsToPack;
    private final Set<Integer> channelsToPackOn;
    private final Map<Integer, Set<Integer>> domains;
    private final Map<Integer, Integer> previousAssignment;
    private final String stationConfigFolder;
    private final String instanceName;

}
