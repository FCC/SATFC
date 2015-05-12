package ca.ubc.cs.beta.stationpacking.cache;

import static ca.ubc.cs.beta.stationpacking.utils.GuavaCollectors.toImmutableSet;

import java.util.Collection;
import java.util.Set;
import java.util.stream.IntStream;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.SatisfiabilityCache;
import ca.ubc.cs.beta.stationpacking.cache.containment.containmentcache.ISatisfiabilityCache;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;
import containmentcache.IContainmentCache;
import containmentcache.bitset.SimpleBitSetCache;
import containmentcache.decorators.BufferedThreadSafeContainmentCacheDecorator;

/**
* Created by newmanne on 22/04/15.
*/
public class SatisfiabilityCacheFactory implements ISatisfiabilityCacheFactory {

    private static final int SAT_BUFFER_SIZE = 100;
    private static final int UNSAT_BUFFER_SIZE = 3;
    final Set<Station> universe = IntStream.rangeClosed(1, StationPackingUtils.N_STATIONS).mapToObj(Station::new).collect(toImmutableSet());

    @Override
    public ISatisfiabilityCache create(Collection<ContainmentCacheSATEntry> SATEntries, Collection<ContainmentCacheUNSATEntry> UNSATEntries) {
        final IContainmentCache<Station, ContainmentCacheSATEntry> SATCache = BufferedThreadSafeContainmentCacheDecorator.makeBufferedThreadSafe(new SimpleBitSetCache<>(universe), SAT_BUFFER_SIZE);
        SATCache.addAll(SATEntries);
        final IContainmentCache<Station, ContainmentCacheUNSATEntry> UNSATCache = BufferedThreadSafeContainmentCacheDecorator.makeBufferedThreadSafe(new SimpleBitSetCache<>(universe), UNSAT_BUFFER_SIZE);
        UNSATCache.addAll(UNSATEntries);
        return new SatisfiabilityCache(SATCache, UNSATCache);
    }

}
