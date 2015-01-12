package ca.ubc.cs.beta.stationpacking.solvers.underconstrained;

import ca.ubc.cs.beta.stationpacking.base.Station;

import java.util.Map;
import java.util.Set;

/**
 * Created by newmanne on 1/8/15.
 */
public interface IUnderconstrainedStationFinder {

    /** Returns the set of stations that are underconstrained (they will ALWAYS have a feasible channel) */
    Set<Station> getUnderconstrainedStations(Map<Station, Set<Integer>> domains);

}
