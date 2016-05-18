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
package ca.ubc.cs.beta.stationpacking.facade;

import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.yaml.ConfigFile;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.CNFSaverSolverDecorator;
import ch.qos.logback.classic.Level;
import lombok.Value;
import lombok.experimental.Builder;

@Value
@Builder
public class SATFCFacadeParameter {

    // public options
	private final String claspLibrary;
	private final String satensteinLibrary;
	private final ConfigFile configFile;
	private final String serverURL;
	
	private final String resultFile;
    private Level logLevel;

    private int numServerAttempts;
    private boolean noErrorOnServerUnavailable;

    private AutoAugmentOptions autoAugmentOptions;

    // developer options
    private final CNFSaverSolverDecorator.ICNFSaver CNFSaver;
    // It's possible to specify a datamanager here so that facade's can be quickly rebuilt without reloading constraints
    private final DataManager dataManager;
    private final SolverChoice solverChoice;

	public enum SolverChoice
	{
        YAML;
    }

}

