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
package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers;

import java.util.Map;

import ca.ubc.cs.beta.stationpacking.solvers.decorators.ISATFCInterruptible;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * Interface for a SAT solver.
 * @author afrechet
 */
public interface ISATSolver extends ISATFCInterruptible {

	/**
	 * @param aCNF - a CNF to solve.
	 * @param aTerminationCriterion - the criterion dictating when to stop execution of solver.
	 * @param aSeed - the seed for the execution.
	 */
	SATSolverResult solve(CNF aCNF, ITerminationCriterion aTerminationCriterion, long aSeed);

	default SATSolverResult solve(CNF aCNF, Map<Long, Boolean> aPreviousAssignment, ITerminationCriterion aTerminationCriterion, long aSeed) {
		return solve(aCNF, aTerminationCriterion, aSeed);
	}

	void notifyShutdown();
	
	default void interrupt() {};

}
