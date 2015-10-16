/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
 *
 * This file is part of satfc.
 *
 * satfc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * satfc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with satfc.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult.SolvedBy;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

import com.google.common.collect.ImmutableMap;

/**
 * UNSAT certifier. Checks if a given neighborhood of instances cannot be packed together.
 *
 * @author afrechet
 */
@Slf4j
public class StationSubsetUNSATCertifier implements IStationSubsetCertifier {

    private final ISolver fSolver;

    public StationSubsetUNSATCertifier(ISolver aSolver) {
        fSolver = aSolver;
    }

    @Override
    public SolverResult certify(StationPackingInstance aInstance, Set<Station> aToPackStations, ITerminationCriterion aTerminationCriterion, long aSeed) {
        final Watch watch = Watch.constructAutoStartWatch();
        final ImmutableMap<Station, Set<Integer>> domains = aInstance.getDomains();
        log.debug("Evaluating if stations not in previous assignment ({}) with their neighborhood are unpackable.", aToPackStations.size());
        final Map<Station, Set<Integer>> toPackDomains = aToPackStations.stream().collect(Collectors.toMap(Function.identity(), domains::get));

        final StationPackingInstance UNSATboundInstance = new StationPackingInstance(toPackDomains, aInstance.getPreviousAssignment(), aInstance.getMetadata());

        final SolverResult UNSATboundResult = fSolver.solve(UNSATboundInstance, aTerminationCriterion, aSeed);

        if (UNSATboundResult.getResult().equals(SATResult.UNSAT)) {
            log.debug("Stations not in previous assignment cannot be packed with their neighborhood.");
            return SolverResult.createNonSATResult(SATResult.UNSAT, watch.getElapsedTime(), SolvedBy.UNSAT_PRESOLVER);
        } else {
            return SolverResult.createTimeoutResult(watch.getElapsedTime());
        }
    }

    @Override
    public void notifyShutdown() {
        fSolver.interrupt();
    }

    @Override
    public void interrupt() throws UnsupportedOperationException {
        fSolver.interrupt();
    }

}