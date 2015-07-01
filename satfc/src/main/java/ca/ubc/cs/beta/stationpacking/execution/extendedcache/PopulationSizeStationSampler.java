package ca.ubc.cs.beta.stationpacking.execution.extendedcache;

import java.util.BitSet;

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
    public Integer sample(BitSet bitSet) {
        return 0;
    }
}
