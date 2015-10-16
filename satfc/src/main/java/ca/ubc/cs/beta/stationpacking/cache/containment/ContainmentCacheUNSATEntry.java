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
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import lombok.Data;
import lombok.NonNull;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.utils.GuavaCollectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;

import containmentcache.ICacheEntry;

/**
* Created by newmanne on 25/03/15.
*/
@Data
public class ContainmentCacheUNSATEntry implements ICacheEntry<Station> {
	
    private final BitSet bitSet;
    private final ImmutableMap<Station, Set<Integer>> domains;
    private final String key;
    private final ImmutableBiMap<Station, Integer> permutation;

    public ContainmentCacheUNSATEntry(
    		@NonNull Map<Station, Set<Integer>> domains, 
    		@NonNull String key, 
    		@NonNull BiMap<Station, Integer> permutation) {
        this.key = key;
        this.domains = ImmutableMap.copyOf(domains);
        this.permutation = ImmutableBiMap.copyOf(permutation);
        this.bitSet = new BitSet(2174); // hardcoded number that represents the expected number of stations in the universe. This is just space pre-allocation, if the estimate is wrong, no harm is done.
        domains.keySet().forEach(station -> bitSet.set(permutation.get(station)));
    }

    @Override
    public Set<Station> getElements() {
    	final Map<Integer, Station> inverse = permutation.inverse();
        return bitSet.stream().mapToObj(inverse::get).collect(GuavaCollectors.toImmutableSet());
    }

    /*
     * returns true if this UNSAT entry is less restrictive than the cacheEntry
     * this UNSAT entry is less restrictive cacheEntry if this UNSAT has same or less stations than cacheEntry
     * and each each stations has same or more candidate channels than the corresponding station in cacheEntry
     * UNSAT entry with same key is not considered
     */
    public boolean isLessRestrictive(ContainmentCacheUNSATEntry cacheEntry) {
        // skip checking against itself
        if (!this.getKey().equals(cacheEntry.getKey())) {
            Map<Station, Set<Integer>> moreRes = cacheEntry.domains;
            Map<Station, Set<Integer>> lessRes = this.domains;
            // lessRes has less stations to pack
            if (moreRes.keySet().containsAll(lessRes.keySet())) {
                // each station in lessRes has same or more candidate channels than the corresponding station in moreRes
                return StreamSupport.stream(lessRes.keySet().spliterator(), false)
                        .allMatch(station -> lessRes.get(station).containsAll(moreRes.get(station)));
            }
        }
        return false;
    }

}
