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
package ca.ubc.cs.beta.stationpacking.cache;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.SatisfiabilityCache;
import ca.ubc.cs.beta.stationpacking.cache.containment.containmentcache.ISatisfiabilityCache;
import ca.ubc.cs.beta.stationpacking.utils.GuavaCollectors;
import containmentcache.IContainmentCache;
import containmentcache.ILockableContainmentCache;
import containmentcache.bitset.simple.SimpleBitSetCache;
import containmentcache.decorators.BufferedThreadSafeCacheDecorator;

/**
* Created by newmanne on 22/04/15.
*/
@Slf4j
public class SatisfiabilityCacheFactory implements ISatisfiabilityCacheFactory {

    private static final int SAT_BUFFER_SIZE = 100;
    private static final int UNSAT_BUFFER_SIZE = 3;
    private final Set<Station> universe;

    public SatisfiabilityCacheFactory(List<String> stationIds) {
        universe = stationIds.stream().map(Integer::parseInt).sorted().map(Station::new).collect(GuavaCollectors.toImmutableSet());
    }

    @Override
    public ISatisfiabilityCache create(Collection<ContainmentCacheSATEntry> SATEntries, Collection<ContainmentCacheUNSATEntry> UNSATEntries) {
        final IContainmentCache<Station, ContainmentCacheSATEntry> SATCache = BufferedThreadSafeCacheDecorator.makeBufferedThreadSafe(new SimpleBitSetCache<>(universe), SAT_BUFFER_SIZE);
        SATCache.addAll(SATEntries);
        final IContainmentCache<Station, ContainmentCacheUNSATEntry> UNSATCache = BufferedThreadSafeCacheDecorator.makeBufferedThreadSafe(new SimpleBitSetCache<>(universe), UNSAT_BUFFER_SIZE);
        UNSATCache.addAll(UNSATEntries);
        return new SatisfiabilityCache((ILockableContainmentCache<Station, ContainmentCacheSATEntry>)SATCache, (ILockableContainmentCache<Station, ContainmentCacheUNSATEntry>)UNSATCache);
    }

}
