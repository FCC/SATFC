package ca.ubc.cs.beta.stationpacking.cache;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.containment.containmentcache.ISatisfiabilityCache;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import lombok.RequiredArgsConstructor;

/**
 * Created by newmanne on 01/02/16.
 * Filter entries (determine whether or not they should be cached)
 */
public interface ICacheEntryFilter {

    /**
     * @Return True if should cache
     */
    boolean shouldCache(CacheCoordinate coordinate, StationPackingInstance instance, SolverResult result);

}
