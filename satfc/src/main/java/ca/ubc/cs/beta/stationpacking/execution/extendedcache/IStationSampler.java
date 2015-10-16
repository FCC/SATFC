package ca.ubc.cs.beta.stationpacking.execution.extendedcache;

import java.util.Set;

/**
 * Created by emily404 on 6/4/15.
 */
public interface IStationSampler {

    /**
     * Determine a new station to add to the problem based on the sampling method
     * {@link ca.ubc.cs.beta.stationpacking.execution.extendedcache.StationSamplerParameters.StationSamplingMethod}
     * @param stationsInProblem a set representing stations that are present in a problem
     * @return stationID of the station to be added
     */
    Integer sample(Set<Integer> stationsInProblem);
}
