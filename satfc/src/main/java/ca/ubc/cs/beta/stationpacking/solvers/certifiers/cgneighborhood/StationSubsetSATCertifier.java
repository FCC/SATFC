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
package ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;
import lombok.extern.slf4j.Slf4j;

/**
 * SAT certifier. Pre-solver that checks whether the missing stations plus their neighborhood are packable when all other stations are fixed
 * to their previous assignment values.
 *
 * @author afrechet
 */
@Slf4j
public class StationSubsetSATCertifier implements IStationSubsetCertifier {

    private final ISolver fSolver;

    public StationSubsetSATCertifier(ISolver aSolver) {
        fSolver = aSolver;
    }

    @Override
    public SolverResult certify(StationPackingInstance aInstance, Set<Station> aToPackStations, ITerminationCriterion aTerminationCriterion, long aSeed) {

        final Watch watch = Watch.constructAutoStartWatch();

        final Map<Station, Integer> previousAssignment = aInstance.getPreviousAssignment();

		/*
         * Try the SAT bound, namely to see if the missing stations plus their neighborhood are packable when all other stations are fixed
		 * to their previous assignment values.
		 */
        //Change station packing instance so that the 'not to pack' stations have reduced domain.
        final Map<Station, Set<Integer>> domains = aInstance.getDomains();
        final Map<Station, Set<Integer>> reducedDomains = new HashMap<>();

        for (final Station station : aInstance.getStations()) {
            final Set<Integer> domain = domains.get(station);

            if (!aToPackStations.contains(station)) {
                final Integer previousChannel = previousAssignment.get(station);
                if (!domain.contains(previousChannel)) {
                    log.warn("Station {} in previous assignment is assigned to channel {} not in current domain {} - SAT certifier is indecisive.", station, previousChannel, domain);
                    return SolverResult.createTimeoutResult(watch.getElapsedTime());
                }
                reducedDomains.put(station, Sets.newHashSet(previousChannel));
            } else {
                reducedDomains.put(station, domain);
            }
        }
        log.debug("Evaluating if stations not in previous assignment with their neighborhood are packable when all other stations are fixed to previous assignment.");

        final StationPackingInstance SATboundInstance = new StationPackingInstance(reducedDomains, previousAssignment);

        log.debug("Going off to SAT solver...");
        final SolverResult SATboundResult = fSolver.solve(SATboundInstance, aTerminationCriterion, aSeed);
        log.debug("Back from SAT solver... SAT bound result was {}", SATboundResult.getResult());

        final SolverResult result;
        if (SATboundResult.getResult().equals(SATResult.SAT)) {
            log.debug("Stations not in previous assignment can be packed with their neighborhood when all other stations are fixed to their previous assignment..");
            result = SolverResult.relabelTimeAndSolvedBy(SATboundResult, watch.getElapsedTime(), SolverResult.SolvedBy.SAT_PRESOLVER);
        } else {
            result = SolverResult.createTimeoutResult(watch.getElapsedTime());
        }
        return result;
    }

    @Override
    public void notifyShutdown() {
        fSolver.notifyShutdown();
    }

    @Override
    public void interrupt() {
        fSolver.interrupt();
    }
}
