package ca.ubc.cs.beta.stationpacking.cache.containment.containmentcache;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATResult;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;

import java.util.List;

/**
 * Created by newmanne on 19/04/15.
 */
public interface ISatisfiabilityCache {
    ContainmentCacheSATResult proveSATBySuperset(final StationPackingInstance aInstance);
    ContainmentCacheUNSATResult proveUNSATBySubset(final StationPackingInstance aInstance);
    void add(final StationPackingInstance aInstance, final SolverResult result, final String key);
    List<ContainmentCacheSATEntry> filterSAT();
    List<ContainmentCacheUNSATEntry> filterUNSAT();
}
