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
package ca.ubc.cs.beta.stationpacking.cache.containment.containmentcache;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableBiMap;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATResult;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATResult;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

/**
 * Created by newmanne on 19/04/15.
 */
public interface ISatisfiabilityCache {

    ContainmentCacheSATResult proveSATBySuperset(final StationPackingInstance aInstance, final Predicate<ContainmentCacheSATEntry> filterPredicate);
    default ContainmentCacheSATResult proveSATBySuperset(final StationPackingInstance aInstance) {
        return proveSATBySuperset(aInstance, unused -> true);
    }

    ContainmentCacheUNSATResult proveUNSATBySubset(final StationPackingInstance aInstance);

    void add(ContainmentCacheSATEntry SATEntry);
    default void addAllSAT(Collection<ContainmentCacheSATEntry> SATEntries) {
        SATEntries.forEach(this::add);
    }
    void add(ContainmentCacheUNSATEntry UNSATEntry);
    default void addAllUNSAT(Collection<ContainmentCacheUNSATEntry> UNSATEntries) {
        UNSATEntries.forEach(this::add);
    }

    List<ContainmentCacheSATEntry> filterSAT(IStationManager stationManager, boolean strong);
    List<ContainmentCacheUNSATEntry> filterUNSAT();

    List<ContainmentCacheSATEntry> findMaxIntersections(final StationPackingInstance instance, int k);

    ImmutableBiMap<Station, Integer> getPermutation();
}
