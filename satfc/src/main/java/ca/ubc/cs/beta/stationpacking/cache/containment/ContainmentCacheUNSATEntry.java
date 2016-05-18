/**
 * Copyright 2016, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimaps;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.cache.ISATFCCacheEntry;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.utils.GuavaCollectors;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;
import containmentcache.ICacheEntry;
import lombok.Data;
import lombok.NonNull;

/**
* Created by newmanne on 25/03/15.
*/
@Data
public class ContainmentCacheUNSATEntry implements ICacheEntry<Station>, ISATFCCacheEntry {

    public static final int BITS_PER_STATION = StationPackingUtils.UHFmax - StationPackingUtils.LVHFmin + 1;

    private final BitSet bitSet;
    private final BitSet domainsBitSet;
    private final ImmutableBiMap<Station, Integer> permutation;

    private String key;
    private String auction;

    public ContainmentCacheUNSATEntry(
    		@NonNull Map<Station, Set<Integer>> domains, 
    		@NonNull BiMap<Station, Integer> permutation) {
        domainsBitSet = new BitSet(domains.size() * BITS_PER_STATION);
        // Sort stations according to the permutation
        final ImmutableList<Station> stations = domains.keySet().stream()
                .sorted((a, b) -> permutation.get(a).compareTo(permutation.get(b)))
                .collect(GuavaCollectors.toImmutableList());
        int offset = 0;
        for (Station s: stations) {
            // Set a bit for each channel that is in the domain
            for (Integer chan : domains.get(s)) {
                int index = offset + chan - StationPackingUtils.UHFmin;
                domainsBitSet.set(index);
            }
            offset += BITS_PER_STATION;
        }
        this.permutation = ImmutableBiMap.copyOf(permutation);
        this.bitSet = new BitSet(permutation.size());
        domains.keySet().forEach(station -> bitSet.set(permutation.get(station)));
    }

    // construct from Redis cache entry
    public ContainmentCacheUNSATEntry(
            @NonNull BitSet bitSet,
            @NonNull BitSet domains,
            @NonNull String key,
            @NonNull BiMap<Station, Integer> permutation,
            String auction
    ) {
        this.permutation = ImmutableBiMap.copyOf(permutation);
        this.key = key;
        this.bitSet = bitSet;
        this.domainsBitSet = domains;
        this.auction = auction;
    }


    @Override
    public Set<Station> getElements() {
    	final Map<Integer, Station> inverse = permutation.inverse();
        return bitSet.stream().mapToObj(inverse::get).collect(GuavaCollectors.toImmutableSet());
    }

    public Map<Station, Set<Integer>> getDomains() {
        final HashMultimap<Station, Integer> domains = HashMultimap.create();
        final Map<Integer, Station> inversePermutation = permutation.inverse();
        int offset = 0;
        // Loop over all stations
        for (int bit = bitSet.nextSetBit(0); bit >= 0; bit = bitSet.nextSetBit(bit+1)) {
            final Station station = inversePermutation.get(bit);
            // Reconstruct a station's domain
            for (int chanBit = domainsBitSet.nextSetBit(offset); chanBit < offset + BITS_PER_STATION && chanBit >= 0; chanBit = domainsBitSet.nextSetBit(chanBit+1)) {
                int chan = (chanBit % BITS_PER_STATION) + StationPackingUtils.UHFmin;
                domains.put(station, chan);
            }
            offset += BITS_PER_STATION;
        }
        return Multimaps.asMap(domains);
    }


    /*
     * returns true if this UNSAT entry is less restrictive than the cacheEntry
     * this UNSAT entry is less restrictive cacheEntry if this UNSAT has same or less stations than cacheEntry
     * and each each stations has same or more candidate channels than the corresponding station in cacheEntry
     * UNSAT entry with same key is not considered
     */
    public boolean isLessRestrictive(ContainmentCacheUNSATEntry cacheEntry) {
        // skip checking against itself
        if (this != cacheEntry) {
            Map<Station, Set<Integer>> moreRes = cacheEntry.getDomains();
            Map<Station, Set<Integer>> lessRes = this.getDomains();
            // lessRes has less stations to pack
            if (moreRes.keySet().containsAll(lessRes.keySet())) {
                // each station in lessRes has same or more candidate channels than the corresponding station in moreRes
                return StreamSupport.stream(lessRes.keySet().spliterator(), false)
                        .allMatch(station -> lessRes.get(station).containsAll(moreRes.get(station)));
            }
        }
        return false;
    }

    @Override
    public SATResult getResult() {
        return SATResult.UNSAT;
    }
}
