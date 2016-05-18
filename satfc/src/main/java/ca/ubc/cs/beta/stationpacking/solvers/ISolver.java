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
package ca.ubc.cs.beta.stationpacking.solvers;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.ISATFCInterruptible;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * <p>
 * Solves station packing problem instance.
 * </p>
 * <p>
 * Strongly suggested to use notifyShutdown() once finished with the solver (so that all resources can be released). 
 * </p>
 * @author afrechet
 */
public interface ISolver extends ISATFCInterruptible {
	
	/**
	 * Solve a station packing instance under the provided CPU time cutoff and given seed.
	 * @param aInstance - the instance to solved.
	 * @param aTerminationCriterion - the termination criterion for solver execution (usually cutoff time based).
	 * @param aSeed - the execution seed.
	 * @return
	 */
	SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed);
	
	/**
	 * Ask the solver to shutdown.
	 */
	default void notifyShutdown() {};
	
	default void interrupt() {};

}
