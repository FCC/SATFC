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
package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.ConstraintGraphNeighborhoodPresolver;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.StationSubsetSATCertifier;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.StationSubsetUNSATCertifier;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.NoGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.composites.SequentialSolversComposite;
import ca.ubc.cs.beta.stationpacking.solvers.sat.CompressedSATBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.ClaspSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.termination.cputime.CPUTimeTerminationCriterionFactory;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;

/**
 * Solver bundles that only performs SATFC pre-solving.
 * @author afrechet
 */
public class PreSolvingOnlySolverBundle extends ASolverBundle {

	private static Logger log = LoggerFactory.getLogger(PreSolvingOnlySolverBundle.class);
	
	private final ISolver fUHFSolver;
	private final ISolver fVHFSolver;
	
	/**
	 * Create a pre-solving only solver bundle.
	 * @param aClaspLibraryPath - clasp library to use in pre-solvers.
	 * @param aStationManager - station manager.
	 * @param aConstraintManager - constraint manager.
	 */
	public PreSolvingOnlySolverBundle(String aClaspLibraryPath,
			IStationManager aStationManager,
			IConstraintManager aConstraintManager) {
		super(aStationManager, aConstraintManager);
		
		log.info("Solver selector: PRE-SOLVING ONLY.");
		
		//Initialize clasp.
		log.warn("Initializing clasps with internal configurations.");
		
		SATCompressor aCompressor = new SATCompressor(this.getConstraintManager());
		
		log.debug("Initializing clasp solvers.");
		
		AbstractCompressedSATSolver aUHFClaspSATsolver = new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.ALL_CONFIG_11_13);
		ISolver UHFClaspBasedSolver = new CompressedSATBasedSolver(aUHFClaspSATsolver, aCompressor,  this.getConstraintManager());
		
		AbstractCompressedSATSolver aHVHFClaspSATsolver = new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.HVHF_CONFIG_09_13);
		ISolver VHFClaspBasedSolver = new CompressedSATBasedSolver(aHVHFClaspSATsolver, aCompressor,  this.getConstraintManager());
		
		final double UNSATcertifiercutoff = 30;
		final double SATcertifiercutoff = 30;
		
		//Chain pre-solving and main solver.
		fUHFSolver = new SequentialSolversComposite(
				Arrays.asList(
						(ISolver)new ConstraintGraphNeighborhoodPresolver(aConstraintManager, 
								Arrays.asList(
										new StationSubsetUNSATCertifier(UHFClaspBasedSolver,new CPUTimeTerminationCriterionFactory(UNSATcertifiercutoff)),
										new StationSubsetSATCertifier(UHFClaspBasedSolver,new CPUTimeTerminationCriterionFactory(SATcertifiercutoff))
								))
						)
				);
		
		fVHFSolver = new SequentialSolversComposite(
				Arrays.asList(
						(ISolver)new ConstraintGraphNeighborhoodPresolver(aConstraintManager, 
								Arrays.asList(
										new StationSubsetUNSATCertifier(VHFClaspBasedSolver,new CPUTimeTerminationCriterionFactory(UNSATcertifiercutoff)),
										new StationSubsetSATCertifier(VHFClaspBasedSolver,new CPUTimeTerminationCriterionFactory(SATcertifiercutoff))
								))
						)
				);
		
		
	}
	
	@Override
	public ISolver getSolver(StationPackingInstance aInstance) {
		
		//Return the right solver based on the channels in the instance.
		log.debug("Returning Ilya's station subset pre-solving only.");
		
		if(StationPackingUtils.HVHF_CHANNELS.containsAll(aInstance.getAllChannels()) || StationPackingUtils.LVHF_CHANNELS.containsAll(aInstance.getAllChannels()))
		{
			
			return fVHFSolver;
		}
		else
		{
			return fUHFSolver;
		}
	}

	@Override
	public void close() {
		fUHFSolver.notifyShutdown();
		fVHFSolver.notifyShutdown();
	}



}
