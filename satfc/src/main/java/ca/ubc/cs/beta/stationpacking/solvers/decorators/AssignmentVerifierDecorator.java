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
package ca.ubc.cs.beta.stationpacking.solvers.decorators;


import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * Verifies the assignments returned by decorated solver for satisfiability. 
 * @author afrechet
 */
public class AssignmentVerifierDecorator extends ASolverDecorator
{   
    private static final Logger log = LoggerFactory.getLogger(AssignmentVerifierDecorator.class);
    
    private final IConstraintManager fConstraintManager;

    public AssignmentVerifierDecorator(ISolver aSolver, IConstraintManager aConstraintManager)
    {
        super(aSolver);
        fConstraintManager = aConstraintManager;
    }
    
    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed)
    {
        final SolverResult result = fDecoratedSolver.solve(aInstance, aTerminationCriterion, aSeed);
        if(result.getResult().equals(SATResult.SAT))
        {
            log.debug("Independently verifying the veracity of returned assignment");

            //Check that the assignment assigns every station to a channel
            final int assignmentSize = result.getAssignment().keySet().stream().mapToInt(channel -> result.getAssignment().get(channel).size()).sum();
            checkState(assignmentSize == aInstance.getStations().size(), "Merged station assignment doesn't assign exactly the stations in the instance.");

            final boolean correct = fConstraintManager.isSatisfyingAssignment(result.getAssignment());
            if(!correct)
            {
                throw new IllegalStateException("Solver returned SAT, but assignment is not satisfiable.");
            }
            else
            {
                log.debug("Assignment was independently verified to be satisfiable.");
            }
        }
        return result;
    }

}
