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
package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.factories;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.sat.CompressedSATBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;

/**
 * Creates an ISolver factory that will be using the given SAT Solver, encoder and grouper.  Note
 * that the same instance of SAT solver, encoder and grouper will be used in all the ISolvers created 
 * (i.e. calls to create).
 */
public class CompressedSATBasedSolverFactory implements ISolverFactory {

	private AbstractCompressedSATSolver fSATSolver;
	
	/**
	 * Creates a new factory that will use the given solver and grouper to create ISolvers.
	 * @param solver solver to be used to create ISolvers.
	 * @param grouper grouper to be used to create ISolvers.
	 */
	public CompressedSATBasedSolverFactory(AbstractCompressedSATSolver solver, IComponentGrouper grouper)
	{
		fSATSolver = solver;
	}
	
	@Override
	public ISolver create(IStationManager stationManager, IConstraintManager constraintManager) {
		SATCompressor encoder = new SATCompressor(constraintManager);
		ISolver solver = new CompressedSATBasedSolver(fSATSolver, encoder, constraintManager);
		return solver;
	}

}
