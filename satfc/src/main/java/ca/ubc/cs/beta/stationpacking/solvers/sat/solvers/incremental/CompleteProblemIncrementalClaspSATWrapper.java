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
package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental;

import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.cputime.CPUTimeTerminationCriterion;

public class CompleteProblemIncrementalClaspSATWrapper extends AbstractSATSolver{

	private final static double INITIAL_CUTOFF = 2;
	private IncrementalClaspSATSolver fIncrementalSATSolver;
	
	public CompleteProblemIncrementalClaspSATWrapper(String libraryPath, String parameters, long seed, CNF aCompleteProblem)
	{
		fIncrementalSATSolver = new IncrementalClaspSATSolver(libraryPath, parameters, seed);
		//Set the incremental 
		fIncrementalSATSolver.solve(aCompleteProblem, new CPUTimeTerminationCriterion(INITIAL_CUTOFF), seed);
	}

	@Override
	public SATSolverResult solve(CNF aCNF, ITerminationCriterion aTerminationCriterion, long aSeed) {
		
		
		return fIncrementalSATSolver.solve(aCNF, aTerminationCriterion, aSeed);
	}

	@Override
	public void interrupt() throws UnsupportedOperationException {
		fIncrementalSATSolver.interrupt();
	}

	@Override
	public void notifyShutdown() {
		fIncrementalSATSolver.notifyShutdown();
	}

}
