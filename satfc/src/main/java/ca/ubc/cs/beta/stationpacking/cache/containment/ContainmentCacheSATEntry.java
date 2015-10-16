/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
 *
 * This file is part of SATFC.
 *
 * SATFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SATFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.cache.containment;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import lombok.Data;
import lombok.NonNull;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.utils.CacheUtils;
import ca.ubc.cs.beta.stationpacking.utils.GuavaCollectors;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimaps;

import containmentcache.ICacheEntry;

/**
* Created by newmanne on 25/03/15.
*/
@Data
public class ContainmentCacheSATEntry implements ICacheEntry<Station> {
    
	private final byte[] channels;
    private final BitSet bitSet;
    private final ImmutableBiMap<Station, Integer> permutation;
    private final String key;

    public ContainmentCacheSATEntry(
    		@NonNull Map<Integer, Set<Station>> answer, 
    		@NonNull String key, 
    		@NonNull BiMap<Station, Integer> permutation) {
    	this.permutation = ImmutableBiMap.copyOf(permutation);
    	this.key = key;
        this.bitSet = CacheUtils.toBitSet(answer, permutation);
        final Map<Station, Integer> stationToChannel = StationPackingUtils.stationToChannelFromChannelToStation(answer);
        final int numStations = this.bitSet.cardinality();
        channels = new byte[numStations];
        int j = 0;
        final Map<Integer, Station> inversePermutation = permutation.inverse();
        for (int bit = bitSet.nextSetBit(0); bit >= 0; bit = bitSet.nextSetBit(bit+1)) {
            channels[j] = stationToChannel.get(inversePermutation.get(bit)).byteValue();
            j++;
        }
    }

    // aInstance is already known to be a subset of this entry
    public boolean isSolutionTo(StationPackingInstance aInstance) {
        final ImmutableMap<Station, Set<Integer>> domains = aInstance.getDomains();
        final Map<Integer, Integer> stationToChannel = getAssignment();
        return domains.entrySet().stream().allMatch(entry -> entry.getValue().contains(stationToChannel.get(entry.getKey().getID())));
    }

    public Map<Integer, Set<Station>> getAssignmentChannelToStation() {
        final Map<Integer, Integer> stationToChannel = getAssignment();
        final HashMultimap<Integer, Station> channelAssignment = HashMultimap.create();
        stationToChannel.entrySet().forEach(entry -> {
            channelAssignment.get(entry.getValue()).add(new Station(entry.getKey()));
        });
        return Multimaps.asMap(channelAssignment);
    }

    public Map<Integer,Integer> getAssignment() {
        final Map<Integer, Integer> stationToChannel = new HashMap<>();
        int j = 0;
        final Map<Integer, Station> inversePermutation = permutation.inverse();
        for (int bit = bitSet.nextSetBit(0); bit >= 0; bit = bitSet.nextSetBit(bit+1)) {
            stationToChannel.put(inversePermutation.get(bit).getID(), Byte.toUnsignedInt(channels[j]));
            j++;
        }
        return stationToChannel;
    }

    @Override
    public Set<Station> getElements() {
        final Map<Integer, Station> inversePermutation = permutation.inverse();
        return bitSet.stream().mapToObj(inversePermutation::get).collect(GuavaCollectors.toImmutableSet());
    }

    /*
     * returns true if this SAT entry is a superset of the cacheEntry, hence this SAT has more solving power than cacheEntry
     * this SAT entry is superset of the cacheEntry if this SAT has same or more channels than cacheEntry
     * and each each channel covers same or more stations than the corresponding channel in cacheEntry
     * SAT entry with same key is not considered as a superset
     */
    public boolean hasMoreSolvingPower(ContainmentCacheSATEntry cacheEntry) {
        // skip checking against itself
        if (!this.getKey().equals(cacheEntry.getKey())) {
            final Map<Integer, Set<Station>> subset = cacheEntry.getAssignmentChannelToStation();
            final Map<Integer, Set<Station>> superset = getAssignmentChannelToStation();
            if (superset.keySet().containsAll(subset.keySet())) {
                return StreamSupport.stream(subset.keySet().spliterator(), false)
                        .allMatch(channel -> superset.get(channel).containsAll(subset.get(channel)));
            }
        }
        return false;
    }
    
}
