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
import ca.ubc.cs.beta.stationpacking.solvers.certifierpresolvers.cgneighborhood.ConstraintGraphNeighborhoodPresolver;
import ca.ubc.cs.beta.stationpacking.solvers.certifierpresolvers.cgneighborhood.StationSubsetSATCertifier;
import ca.ubc.cs.beta.stationpacking.solvers.certifierpresolvers.cgneighborhood.StationSubsetUNSATCertifier;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.composites.SequentialSolversComposite;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.CNFSaverSolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.ResultSaverSolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.UnderconstrainedStationRemoverSolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.sat.CompressedSATBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.ClaspSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.termination.cputime.CPUTimeTerminationCriterionFactory;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;

/**
 * SATFC solver bundle that lines up pre-solving and main solver.
 * @author afrechet
 */
public class SATFCSolverBundle extends ASolverBundle{

	private static Logger log = LoggerFactory.getLogger(SATFCSolverBundle.class);
	
	private final ISolver fUHFSolver;
	private final ISolver fVHFSolver;
	
	/**
	 * Create a SATFC solver bundle.
	 * @param aClaspLibraryPath - library for the clasp to use.
	 * @param aStationManager - station manager.
	 * @param aConstraintManager - constraint manager.
	 * @param aCNFDirectory - directory in which CNFs should be saved (optional).
	 * @param aResultFile - file to which results should be written (optional).
	 */
	public SATFCSolverBundle(
			String aClaspLibraryPath,
			IStationManager aStationManager,
			IConstraintManager aConstraintManager,
			String aCNFDirectory,
			String aResultFile
			) {
		
		super(aStationManager, aConstraintManager);
		log.debug("SATFC solver bundle.");
		
		SATCompressor aCompressor = new SATCompressor(this.getConstraintManager());
		
		log.debug("Decomposing intances into connected components using constraint graph.");
		IComponentGrouper aGrouper = new ConstraintGrouper();
		
		log.debug("Initializing base configured clasp solvers.");
		
		AbstractCompressedSATSolver aUHFClaspSATsolver = new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.ALL_CONFIG_11_13);
		ISolver UHFClaspBasedSolver = new CompressedSATBasedSolver(aUHFClaspSATsolver, aCompressor,  this.getConstraintManager(), aGrouper);
		
		AbstractCompressedSATSolver aHVHFClaspSATsolver = new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.HVHF_CONFIG_09_13);
		ISolver VHFClaspBasedSolver = new CompressedSATBasedSolver(aHVHFClaspSATsolver, aCompressor,  this.getConstraintManager(), aGrouper);
		
		//Chain pre-solving and main solver.
		final double UNSATcertifiercutoff = 5;
		final double SATcertifiercutoff = 5;
		
		ISolver UHFsolver;
		ISolver VHFsolver;
		
		log.debug("Adding neighborhood presolvers.");
		UHFsolver = new SequentialSolversComposite(
				Arrays.asList(
						new ConstraintGraphNeighborhoodPresolver(aConstraintManager, 
								Arrays.asList(
										new StationSubsetSATCertifier(UHFClaspBasedSolver,new CPUTimeTerminationCriterionFactory(SATcertifiercutoff)),
										new StationSubsetUNSATCertifier(UHFClaspBasedSolver,new CPUTimeTerminationCriterionFactory(UNSATcertifiercutoff))
								)),
								UHFClaspBasedSolver
						)
				);
		
		VHFsolver = new SequentialSolversComposite(
				Arrays.asList(
						new ConstraintGraphNeighborhoodPresolver(aConstraintManager, 
								Arrays.asList(
										new StationSubsetSATCertifier(VHFClaspBasedSolver,new CPUTimeTerminationCriterionFactory(SATcertifiercutoff)),
										new StationSubsetUNSATCertifier(VHFClaspBasedSolver,new CPUTimeTerminationCriterionFactory(UNSATcertifiercutoff))
								)),
								VHFClaspBasedSolver
						)
				);
		
		//Decorate solvers.
		
		//Remove unconstrained stations.
		/**
		log.debug("Decorate solver to first remove underconstrained stations.");
		UHFsolver = new UnderconstrainedStationRemoverSolverDecorator(UHFsolver, aConstraintManager);
		VHFsolver = new UnderconstrainedStationRemoverSolverDecorator(VHFsolver, aConstraintManager);
		**/
		
		
		//Save CNFs, if needed.
		if(aCNFDirectory != null)
		{
			log.debug("Decorate solver to save CNFs.");
			UHFsolver = new CNFSaverSolverDecorator(UHFsolver, getConstraintManager(), aCNFDirectory);
			VHFsolver = new CNFSaverSolverDecorator(VHFsolver, getConstraintManager(), aCNFDirectory);
		}
		
		//Save results, if needed.
		if(aResultFile != null)
		{
			log.debug("Decorate solver to save results.");
			UHFsolver = new ResultSaverSolverDecorator(UHFsolver, aResultFile);
			VHFsolver = new ResultSaverSolverDecorator(VHFsolver, aResultFile);
		}
		
		fUHFSolver = UHFsolver;
		fVHFSolver = VHFsolver;
		
	}

	@Override
	public ISolver getSolver(StationPackingInstance aInstance) {
		
		//Return the right solver based on the channels in the instance.
		
		if(StationPackingUtils.HVHF_CHANNELS.containsAll(aInstance.getAllChannels()) || StationPackingUtils.LVHF_CHANNELS.containsAll(aInstance.getAllChannels()))
		{
			log.debug("Returning clasp configured for VHF (September 2013) with Ilya's station subset pre-solving.");
			return fVHFSolver;
		}
		else
		{
			log.debug("Returning clasp configured for UHF (November 2013) with Ilya's station subset pre-solving.");
			return fUHFSolver;
		}
	}

	@Override
	public void close() {
		fUHFSolver.notifyShutdown();
		fVHFSolver.notifyShutdown();
	}




}
