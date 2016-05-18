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
package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.ManagerBundle;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;
import lombok.extern.slf4j.Slf4j;

/**
* Created by newmanne on 01/10/15.
*/
@Slf4j
public abstract class AVHFUHFSolverBundle extends ASolverBundle {

    public AVHFUHFSolverBundle(ManagerBundle managerBundle) {
        super(managerBundle);
    }

    protected abstract ISolver getUHFSolver();
    protected abstract ISolver getVHFSolver();

    @Override
    public ISolver getSolver(StationPackingInstance aInstance) {
        // Return the right solver based on what band the newly added station is in. This doesn't quite work for multi-band problems, but if a VHF problem incorrectly goes to the UHF solver, it will still get solved, so no harm done
        if (StationPackingUtils.HVHF_CHANNELS.containsAll(aInstance.getAllChannels()) || StationPackingUtils.LVHF_CHANNELS.containsAll(aInstance.getAllChannels())) {
            log.debug("Returning solver configured for VHF");
            return getVHFSolver();
        } else {
            log.debug("Returning solver configured for UHF");
            return getUHFSolver();
        }
    }

    @Override
    public void close() throws Exception {
        getUHFSolver().notifyShutdown();
        getVHFSolver().notifyShutdown();
    }
}
