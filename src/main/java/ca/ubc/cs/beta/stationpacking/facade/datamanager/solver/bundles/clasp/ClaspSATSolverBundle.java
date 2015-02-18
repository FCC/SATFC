/**
 * Copyright 2014, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
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
package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.clasp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ASolverBundle;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.NoGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.sat.CompressedSATBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.ClaspSATSolver;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;

/**
 * Solver bundle for a clasp SAT solver.
 * @author afrechet
 */
public class ClaspSATSolverBundle extends ASolverBundle{
	
	private static Logger log = LoggerFactory.getLogger(ClaspSATSolverBundle.class);
	
	private final ISolver fClaspGeneral;
	private final ISolver fClaspHVHF;
	private final ISolver fClaspUHF;
	
	/**
	 * @param aClaspLibraryPath - clasp library path.
	 * @param aStationManager - station manager to create instances.
	 * @param aConstraintManager - constraint manager to create instances.
	 */
	public ClaspSATSolverBundle(String aClaspLibraryPath, IStationManager aStationManager, IConstraintManager aConstraintManager)
	{
		super(aStationManager,aConstraintManager);
		
		log.debug("Initializing clasp selector bundle.");
		
		SATCompressor aCompressor = new SATCompressor(this.getConstraintManager());
		
		log.debug("Initializing clasp solvers.");
		AbstractCompressedSATSolver aClaspSATsolver =  new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.ALL_CONFIG_11_13);
		fClaspGeneral = new CompressedSATBasedSolver(aClaspSATsolver, aCompressor,  this.getConstraintManager());
		
		AbstractCompressedSATSolver aUHFClaspSATsolver = new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.ALL_CONFIG_11_13);
		fClaspUHF = new CompressedSATBasedSolver(aUHFClaspSATsolver, aCompressor,  this.getConstraintManager());
		
		AbstractCompressedSATSolver aHVHFClaspSATsolver = new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.HVHF_CONFIG_09_13);
		fClaspHVHF = new CompressedSATBasedSolver(aHVHFClaspSATsolver, aCompressor,  this.getConstraintManager());
	}
	
	@Override
	public ISolver getSolver(StationPackingInstance aInstance) {
		
		//Return the right solver based on the channels in the instance.
		if(StationPackingUtils.HVHF_CHANNELS.containsAll(aInstance.getAllChannels()) || StationPackingUtils.LVHF_CHANNELS.containsAll(aInstance.getAllChannels()))
		{
			log.debug("Returning clasp configured for VHF (September 2013).");
			return fClaspHVHF;
		}
		else if(StationPackingUtils.UHF_CHANNELS.containsAll(aInstance.getAllChannels()))
		{
			log.debug("Returning clasp configured for UHF (November 2013).");
			return fClaspUHF;
		}
		else
		{
			log.debug("Returning general configured clasp (November 2013).");
			return fClaspGeneral;
		}
	}


    @Override
    public void close() throws Exception {
        fClaspGeneral.notifyShutdown();
        fClaspHVHF.notifyShutdown();
    }

}
