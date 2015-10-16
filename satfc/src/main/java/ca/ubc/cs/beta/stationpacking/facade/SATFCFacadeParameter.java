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

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Builder;
import ca.ubc.cs.beta.stationpacking.execution.parameters.smac.SATFCHydraParams;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.CNFSaverSolverDecorator;
import ch.qos.logback.classic.Level;

import java.util.List;

@Value
@Builder
public class SATFCFacadeParameter {

    // public options
	private final String claspLibrary;
	private final String resultFile;
	private final SolverChoice solverChoice;
    private final String serverURL;
    private final int parallelismLevel;
    private final boolean cacheResults;
    private Level logLevel;

    // developer options
    private final boolean presolve;
    private final boolean underconstrained;
    private final boolean decompose;
    private final CNFSaverSolverDecorator.ICNFSaver CNFSaver;
    private final SATFCHydraParams hydraParams;
    private final DataManager dataManager;

	public enum SolverChoice
	{
		// Public:
		SATFC_SEQUENTIAL,
        SATFC_PARALLEL,
        // Developer:
		MIPFC,
        CNF,
        CACHING_SOLVER_FULL_INSTANCES,
        CACHING_SOLVER_COMPONENTS,
        STATS,
        HYDRA;
    }

}

