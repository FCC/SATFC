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
package ca.ubc.cs.beta.stationpacking.cache;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.containment.containmentcache.ISatisfiabilityCache;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import lombok.RequiredArgsConstructor;

/**
 * Created by newmanne on 2016-02-14.
 */
@RequiredArgsConstructor
public class NewInfoEntryFilter implements ICacheEntryFilter {

    private final ICacheLocator cacheLocator;

    @Override
    public boolean shouldCache(CacheCoordinate coordinate, StationPackingInstance instance, SolverResult result) {
        final ISatisfiabilityCache cache = cacheLocator.locate(coordinate);
        final boolean containsNewInfo;
        if (result.getResult().equals(SATResult.SAT)) {
            containsNewInfo = !cache.proveSATBySuperset(instance).isValid();
        } else if (result.getResult().equals(SATResult.UNSAT)) {
            containsNewInfo = !cache.proveUNSATBySubset(instance).isValid();
        } else {
            throw new IllegalStateException("Tried adding a result that was neither SAT or UNSAT");
        }
        return containsNewInfo;
    }
}
