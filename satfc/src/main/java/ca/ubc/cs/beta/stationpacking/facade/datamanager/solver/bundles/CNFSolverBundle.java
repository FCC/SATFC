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
package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.VoidSolver;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.CNFSaverSolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.CNFSaverSolverDecorator.ICNFSaver;

/**
 * Created by newmanne on 24/04/15.
 */
public class CNFSolverBundle extends ASolverBundle {

    private ISolver cnfOnlySolver;

    public CNFSolverBundle(
            IStationManager aStationManager,
            IConstraintManager aConstraintManager,
            ICNFSaver aCNFSaver
    ) {
        super(aStationManager, aConstraintManager);
        cnfOnlySolver = new VoidSolver();

        // save the cnf for the full problem
        cnfOnlySolver = new CNFSaverSolverDecorator(cnfOnlySolver,getConstraintManager(), aCNFSaver, false);
    }

    @Override
    public ISolver getSolver(StationPackingInstance aInstance) {
        return cnfOnlySolver;
    }

    @Override
    public void close() throws Exception {
        cnfOnlySolver.notifyShutdown();
    }

}
