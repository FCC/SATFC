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
package ca.ubc.cs.beta.stationpacking.cache.containment.transformer;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;

/**
 * Created by newmanne on 2016-03-28.
 * Restrict the instance / result to be in UHF only (or return null if that doesn't make sense)
 */
public class UHFRestrictionTransformer implements ICacheEntryTransformer {

    @Override
    public InstanceAndResult transform(InstanceAndResult instanceAndResult) {
        final SolverResult result = instanceAndResult.getResult();
        final SATResult satResult = result.getResult();
        final StationPackingInstance instance = instanceAndResult.getInstance();
        final Map<Station, Set<Integer>> uhfRestrictedDomains = Maps.filterValues(instance.getDomains(), StationPackingUtils.UHF_CHANNELS::containsAll);
        if (satResult.equals(SATResult.UNSAT)) {
            // We require the ENTIRE problem to have been in UHF
            if (uhfRestrictedDomains.equals(instance.getDomains())) {
                return instanceAndResult;
            } else {
                return null;
            }
        } else if (satResult.equals(SATResult.SAT)) {
            final StationPackingInstance uhfRestrictedInstance = new StationPackingInstance(uhfRestrictedDomains, instance.getPreviousAssignment(), instance.getMetadata());
            final Map<Integer, Set<Station>> uhfRestrictedAssignment = Maps.filterKeys(instanceAndResult.getResult().getAssignment(), StationPackingUtils.UHF_CHANNELS::contains);
            if (!uhfRestrictedAssignment.isEmpty()) {
                final SolverResult uhfRestrictedResult = new SolverResult(result.getResult(), result.getRuntime(), uhfRestrictedAssignment, result.getSolvedBy(), result.getNickname());
                return new InstanceAndResult(uhfRestrictedInstance, uhfRestrictedResult);
            } else {
                return null;
            }
        } else {
            throw new IllegalArgumentException("Result neither SAT nor UNSAT");
        }
    }
}
