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
package ca.ubc.cs.beta.stationpacking.solvers.decorators.consistency;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.consistency.AC3Enforcer;
import ca.ubc.cs.beta.stationpacking.consistency.AC3Output;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.metrics.SATFCMetrics;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.ASolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by pcernek on 5/8/15.
 * Enforce arc consistency on the problem domains with the hope of shrinking them
 */
@Slf4j
public class ArcConsistencyEnforcerDecorator extends ASolverDecorator {

    private final AC3Enforcer ac3Enforcer;

    /**
     * @param aSolver           - decorated ISolver.
     * @param constraintManager
     */
    public ArcConsistencyEnforcerDecorator(ISolver aSolver, IConstraintManager constraintManager) {
        super(aSolver);
        ac3Enforcer = new AC3Enforcer(constraintManager);
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed) {
        final Watch watch = Watch.constructAutoStartWatch();
        final AC3Output ac3Output = ac3Enforcer.AC3(aInstance, aTerminationCriterion);
        SATFCMetrics.postEvent(new SATFCMetrics.TimingEvent(aInstance.getName(), SATFCMetrics.TimingEvent.ARC_CONSISTENCY, watch.getElapsedTime()));
        if (ac3Output.isNoSolution()) {
            return SolverResult.createNonSATResult(SATResult.UNSAT, watch.getElapsedTime(), SolverResult.SolvedBy.ARC_CONSISTENCY);
        } else {
            log.debug("Removed {} channels", ac3Output.getNumReducedChannels());
            final StationPackingInstance reducedInstance = new StationPackingInstance(ac3Output.getReducedDomains(), aInstance.getPreviousAssignment(), aInstance.getMetadata());
            return SolverResult.relabelTime(fDecoratedSolver.solve(reducedInstance, aTerminationCriterion, aSeed), watch.getElapsedTime());
        }
    }

}