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
package ca.ubc.cs.beta.stationpacking.facade;

import lombok.Data;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.CNFSaverSolverDecorator;

@Data
public class SATFCFacadeParameter {

	private final String claspLibrary;
	private final boolean initializeLogging;
	private final String resultFile;
	private final SolverChoice solverChoice;
    private final boolean presolve;
    private final boolean underconstrained;
    private final boolean decompose;
    private final CNFSaverSolverDecorator.ICNFSaver CNFSaver;
    private final String serverURL;

	public static enum SolverChoice
	{
		SATFC,
		MIPFC,
        CNF,
        CACHING_SOLVER_FULL_INSTANCES,
        CACHING_SOLVER_COMPONENTS,
        CACHE_EVERYTHING
	}

}

