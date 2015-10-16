package ca.ubc.cs.beta.stationpacking.cache.containment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.SatisfiabilityCacheFactory;
import ca.ubc.cs.beta.stationpacking.cache.containment.containmentcache.ISatisfiabilityCache;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import containmentcache.util.PermutationUtils;

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
        satisfiabilityCache.add(new ContainmentCacheSATEntry(ImmutableMap.of(1, UNIVERSE), "", permutation));

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
        satisfiabilityCache.add(new ContainmentCacheUNSATEntry(ImmutableMap.of(s1, Sets.newHashSet(1), s2, Sets.newHashSet(1)), "", permutation));

        // Expected subset
        final StationPackingInstance instance = new StationPackingInstance(ImmutableMap.of(s1, Sets.newHashSet(1), s2, Sets.newHashSet(1), s3, Sets.newHashSet(1)));
        final ContainmentCacheUNSATResult result = satisfiabilityCache.proveUNSATBySubset(instance);
        assertTrue(result.isValid());

        // No expected subset
        final StationPackingInstance instance2 = new StationPackingInstance(ImmutableMap.of(s1, Sets.newHashSet(1)));
        final ContainmentCacheUNSATResult result2 = satisfiabilityCache.proveUNSATBySubset(instance2);
        assertFalse(result2.isValid());
    }
}