package ca.ubc.cs.beta.stationpacking.execution.extendedcache;

import java.util.BitSet;

import ca.ubc.cs.beta.stationpacking.utils.GuavaCollectors;

import com.google.common.collect.ImmutableSet;

/**
 * Created by emily404 on 6/10/15.
 */
public class AddSmallestStationSampler implements IStationSampler {

    /**
     * Find all stations that are not present in current problem, return the first one
     * @param flipped BitSet representing stations that are present in current problem
     * @return stationID of the station to be added
     */
    @Override
    public Integer sample(BitSet flipped) {
        flipped.flip(1, flipped.length());
        ImmutableSet<Integer> stationsToAdd = flipped.stream().mapToObj(Integer::new).collect(GuavaCollectors.toImmutableSet());
        return stationsToAdd.asList().get(0);
    }
}
