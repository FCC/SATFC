package ca.ubc.cs.beta.stationpacking.cache.containment;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.Data;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.utils.CacheUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;

/**
* Created by newmanne on 25/03/15.
*/
@Data
public class ContainmentCacheSATEntry {
    byte[] channels;
    BitSet bitSet;
    String key;

    // fake constructor
    public ContainmentCacheSATEntry(BitSet bitSet) {
        this.bitSet = bitSet;
    }

    public ContainmentCacheSATEntry(Map<Integer, Set<Station>> answer, String key) {
        this.bitSet = CacheUtils.toBitSet(answer);
        final Map<Station, Integer> stationToChannel = CacheUtils.stationToChannelFromChannelToStation(answer);
        this.key = key;
        final int numStations = this.bitSet.cardinality();
        channels = new byte[numStations];
        int j = 0;
        for (int stationId = bitSet.nextSetBit(0); stationId >= 0; stationId = bitSet.nextSetBit(stationId+1)) {
            channels[j] = stationToChannel.get(new Station(stationId)).byteValue();
            j++;
        }
    }

    // aInstance is already known to be a subset of this entry
    public boolean isSolutionTo(StationPackingInstance aInstance) {
        final ImmutableMap<Station, Set<Integer>> domains = aInstance.getDomains();
        final Map<Integer, Integer> stationToChannel = getAssignment();

        return domains.entrySet().stream().allMatch(entry -> entry.getValue().contains(stationToChannel.get(entry.getKey().getID())));
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, Set<Station>> getAssignmentChannelToStation() {
        final Map<Integer, Integer> stationToChannel = getAssignment();
        final HashMultimap<Integer, Station> channelAssignment = HashMultimap.create();
        stationToChannel.entrySet().forEach(entry -> {
            channelAssignment.get(entry.getValue()).add(new Station(entry.getKey()));
        });
        // safe conversion because of SetMultimap
        return (Map<Integer, Set<Station>>) (Map<?, ?>) channelAssignment.asMap();
    }

    public Map<Integer,Integer> getAssignment() {
        final Map<Integer, Integer> stationToChannel = new HashMap<>();
        int j = 0;
        for (int stationId = bitSet.nextSetBit(0); stationId >= 0; stationId = bitSet.nextSetBit(stationId+1)) {
            stationToChannel.put(stationId, Byte.toUnsignedInt(channels[j]));
            j++;
        }
        return stationToChannel;
    }
}
