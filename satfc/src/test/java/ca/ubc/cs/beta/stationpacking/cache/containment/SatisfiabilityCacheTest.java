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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.SatisfiabilityCacheFactory;
import ca.ubc.cs.beta.stationpacking.cache.containment.containmentcache.ISatisfiabilityCache;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import containmentcache.util.PermutationUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SatisfiabilityCacheTest {

    final Station s1 = new Station(1);
    final Station s2 = new Station(2);
    final Station s3 = new Station(3);
    final Set<Station> UNIVERSE = Sets.newHashSet(s1, s2, s3);

    @Test
    public void testProveSATBySuperset() throws Exception {
        final SatisfiabilityCacheFactory factory = new SatisfiabilityCacheFactory(3, 0);
        final ImmutableBiMap<Station, Integer> permutation = PermutationUtils.makePermutation(UNIVERSE);
        final ISatisfiabilityCache satisfiabilityCache = factory.create(permutation);
        satisfiabilityCache.add(new ContainmentCacheSATEntry(ImmutableMap.of(1, UNIVERSE), permutation));

        // Expected superset
        final StationPackingInstance instance = new StationPackingInstance(ImmutableMap.of(s1, Sets.newHashSet(1), s2, Sets.newHashSet(1)));
        final ContainmentCacheSATResult containmentCacheSATResult = satisfiabilityCache.proveSATBySuperset(instance);
        assertTrue(containmentCacheSATResult.isValid());
        assertEquals(ImmutableMap.of(1, Sets.newHashSet(s1, s2, s3)), containmentCacheSATResult.getResult());

        // No expected superset
        final StationPackingInstance instance2 = new StationPackingInstance(ImmutableMap.of(s1, Sets.newHashSet(1), s2, Sets.newHashSet(2)));
        final ContainmentCacheSATResult containmentCacheSATResult2 = satisfiabilityCache.proveSATBySuperset(instance2);
        assertFalse(containmentCacheSATResult2.isValid());
    }

    @Test
    public void testProveUNSATBySubset() throws Exception {
        final SatisfiabilityCacheFactory factory = new SatisfiabilityCacheFactory(3, 0);
        final ImmutableBiMap<Station, Integer> permutation = PermutationUtils.makePermutation(UNIVERSE);
        final ISatisfiabilityCache satisfiabilityCache = factory.create(permutation);
        satisfiabilityCache.add(new ContainmentCacheUNSATEntry(ImmutableMap.of(s1, Sets.newHashSet(15), s2, Sets.newHashSet(15)), permutation));

        // Expected subset
        final StationPackingInstance instance = new StationPackingInstance(ImmutableMap.of(s1, Sets.newHashSet(15), s2, Sets.newHashSet(15), s3, Sets.newHashSet(15)));
        final ContainmentCacheUNSATResult result = satisfiabilityCache.proveUNSATBySubset(instance);
        assertTrue(result.isValid());

        // No expected subset
        final StationPackingInstance instance2 = new StationPackingInstance(ImmutableMap.of(s1, Sets.newHashSet(15)));
        final ContainmentCacheUNSATResult result2 = satisfiabilityCache.proveUNSATBySubset(instance2);
        assertFalse(result2.isValid());
    }

    @Test
    public void testFilterSAT() throws Exception {
        final SatisfiabilityCacheFactory factory = new SatisfiabilityCacheFactory(1, 0);
        final ImmutableBiMap<Station, Integer> permutation = PermutationUtils.makePermutation(UNIVERSE);
        final ISatisfiabilityCache satisfiabilityCache = factory.create(permutation);
        final ContainmentCacheSATEntry c1 = new ContainmentCacheSATEntry(ImmutableMap.of(2, Sets.newHashSet(s1), 3, Sets.newHashSet(s2)), permutation);
        final ContainmentCacheSATEntry c2 = new ContainmentCacheSATEntry(ImmutableMap.of(1, UNIVERSE), permutation);
        c1.setKey("k1");
        c2.setKey("k2");
        satisfiabilityCache.add(c1);
        satisfiabilityCache.add(c2);
        final IStationManager stationManager = new IStationManager() {

            @Override
            public Set<Station> getStations() {
                return UNIVERSE;
            }

            @Override
            public Station getStationfromID(Integer aID) throws IllegalArgumentException {
                return new Station(aID);
            }

            @Override
            public Set<Integer> getDomain(Station aStation) {
                return Sets.newHashSet(1,2,3);
            }

            @Override
            public String getDomainHash() {
                return "whocares";
            }
        };
        List<ContainmentCacheSATEntry> containmentCacheSATEntries = satisfiabilityCache.filterSAT(stationManager, true);
        assertEquals(Iterables.getOnlyElement(containmentCacheSATEntries), c1);
        containmentCacheSATEntries = satisfiabilityCache.filterSAT(stationManager, true);
        assertEquals(containmentCacheSATEntries.size(), 0);
    }

}