package ca.ubc.cs.beta.stationpacking.execution.extendedcache;

import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;

/**
 * Created by emily404 on 6/4/15.
 */
public class PopulationSizeStationSampler implements IStationSampler {

    /**
     * Sample stations based on population density that the station covers,
     * stations with higher population density are more likely to be selected
     * @param bitSet a BitSet representing stations that are present in a problem
     * @return stationID of the station to be added
     */
	@Override
	public Station sample(Set<Station> allStations, Set<Station> stationsInProblem) {
		return null;
	}

}
