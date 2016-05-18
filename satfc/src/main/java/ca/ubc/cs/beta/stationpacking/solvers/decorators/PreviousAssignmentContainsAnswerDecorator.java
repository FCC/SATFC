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
package ca.ubc.cs.beta.stationpacking.solvers.decorators;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by newmanne on 19/01/16.
 */
@Slf4j
public class PreviousAssignmentContainsAnswerDecorator extends ASolverDecorator {

    private final IConstraintManager constraintManager;

    /**
     * @param aSolver - decorated ISolver.
     */
    public PreviousAssignmentContainsAnswerDecorator(ISolver aSolver, IConstraintManager constraintManager) {
        super(aSolver);
        this.constraintManager = constraintManager;
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed) {
        final Watch watch = Watch.constructAutoStartWatch();
        final HashMultimap<Integer, Station> assignment = HashMultimap.create();
        aInstance.getPreviousAssignment().entrySet().stream().forEach(entry -> {
            if (aInstance.getStations().contains(entry.getKey()) && aInstance.getDomains().get(entry.getKey()).contains(entry.getValue())) {
                assignment.put(entry.getValue(), entry.getKey());
            }
        });
        if (assignment.values().size() == aInstance.getStations().size() && assignment.values().containsAll(aInstance.getStations())) {
            final Map<Integer, Set<Station>> integerSetMap = Multimaps.asMap(assignment);
            if (constraintManager.isSatisfyingAssignment(integerSetMap)) {
                log.debug("Previous solution directly solves this problem");
                return new SolverResult(SATResult.SAT, watch.getElapsedTime(), integerSetMap, SolverResult.SolvedBy.PREVIOUS_ASSIGNMENT);
            }
        }
        return super.solve(aInstance, aTerminationCriterion, aSeed);
    }
}
