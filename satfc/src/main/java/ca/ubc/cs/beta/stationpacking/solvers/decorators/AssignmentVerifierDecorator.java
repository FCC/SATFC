/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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
package ca.ubc.cs.beta.stationpacking.solvers.decorators;


import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Verifies the assignments returned by decorated solver for satisfiability.
 *
 * @author afrechet
 */
@Slf4j
public class AssignmentVerifierDecorator extends ASolverDecorator {

    private final IConstraintManager fConstraintManager;

    public AssignmentVerifierDecorator(ISolver aSolver, IConstraintManager aConstraintManager) {
        super(aSolver);
        fConstraintManager = aConstraintManager;
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed) {
        final SolverResult result = fDecoratedSolver.solve(aInstance, aTerminationCriterion, aSeed);
        if (result.getResult().equals(SATResult.SAT)) {
            log.debug("Independently verifying the veracity of returned assignment");

            //Check that the assignment assigns every station to a channel
            final int assignmentSize = result.getAssignment().keySet().stream().mapToInt(channel -> result.getAssignment().get(channel).size()).sum();
            Preconditions.checkState(assignmentSize == aInstance.getStations().size(), "Merged station assignment doesn't assign exactly the stations in the instance.");

            // Check that the every station is on its domain
            final Map<Station, Integer> stationToChannel = StationPackingUtils.stationToChannelFromChannelToStation(result.getAssignment());
            final ImmutableMap<Station, Set<Integer>> domains = aInstance.getDomains();
            for (Map.Entry<Station, Integer> entry : stationToChannel.entrySet()) {
                final Station station = entry.getKey();
                final int channel = entry.getValue();
                Preconditions.checkState(domains.get(station).contains(channel), "Station %s is assigned to channel %s which is not in its domain %s", station, channel, domains.get(station));
            }

            Preconditions.checkState(fConstraintManager.isSatisfyingAssignment(result.getAssignment()), "Solver returned SAT, but assignment is not satisfiable.");
            log.debug("Assignment was independently verified to be satisfiable.");
        }
        return result;
    }

}