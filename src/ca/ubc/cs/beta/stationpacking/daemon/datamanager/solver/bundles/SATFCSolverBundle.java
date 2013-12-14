package ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.DACConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.SequentialSolversComposite;
import ca.ubc.cs.beta.stationpacking.solvers.certifierpresolvers.cgneighborhood.ConstraintGraphNeighborhoodPresolver;
import ca.ubc.cs.beta.stationpacking.solvers.certifierpresolvers.cgneighborhood.StationSubsetSATCertifier;
import ca.ubc.cs.beta.stationpacking.solvers.certifierpresolvers.cgneighborhood.StationSubsetUNSATCertifier;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.NoGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.sat.CompressedSATBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.ClaspSATSolver;

public class SATFCSolverBundle extends ASolverBundle{

	private static Logger log = LoggerFactory.getLogger(SATFCSolverBundle.class);
	
	private final ISolver fUHFSolver;
	private final ISolver fVHFSolver;
	
	public SATFCSolverBundle(String aClaspLibraryPath, IStationManager aStationManager,
			IConstraintManager aConstraintManager) {
		
		super(aStationManager, aConstraintManager);
		
		log.info("Solver selector: PRE-SOLVING WITH CLASP AS MAIN SOLVER.");
		
		//Initialize clasp.
		log.warn("Initializing clasps with internal configurations.");
		
		SATCompressor aCompressor = new SATCompressor(this.getStationManager(), this.getConstraintManager());
		IComponentGrouper aGrouper = new NoGrouper();
		
		log.debug("Initializing clasp solvers.");
		
		AbstractCompressedSATSolver aUHFClaspSATsolver = new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.ALL_CONFIG_11_13);
		ISolver UHFClaspBasedSolver = new CompressedSATBasedSolver(aUHFClaspSATsolver, aCompressor,  this.getConstraintManager(), aGrouper);
		
		AbstractCompressedSATSolver aHVHFClaspSATsolver = new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.HVHF_CONFIG_09_13);
		ISolver VHFClaspBasedSolver = new CompressedSATBasedSolver(aHVHFClaspSATsolver, aCompressor,  this.getConstraintManager(), aGrouper);
		
		//Chain pre-solving and main solver.
		final double UNSATcertifiercutoff = 10;
		final double SATcertifiercutoff = 10;
		
		fUHFSolver = new SequentialSolversComposite(
				Arrays.asList(
						new ConstraintGraphNeighborhoodPresolver(aConstraintManager, 
								Arrays.asList(
										new StationSubsetUNSATCertifier(UHFClaspBasedSolver,UNSATcertifiercutoff),
										new StationSubsetSATCertifier(UHFClaspBasedSolver,SATcertifiercutoff)
								)),
								UHFClaspBasedSolver
						)
				);
		
		fVHFSolver = new SequentialSolversComposite(
				Arrays.asList(
						new ConstraintGraphNeighborhoodPresolver(aConstraintManager, 
								Arrays.asList(
										new StationSubsetUNSATCertifier(VHFClaspBasedSolver,UNSATcertifiercutoff),
										new StationSubsetSATCertifier(VHFClaspBasedSolver,SATcertifiercutoff)
								)),
								VHFClaspBasedSolver
						)
				);
		
	}

	@Override
	public ISolver getSolver(StationPackingInstance aInstance) {
		
		//Return the right solver based on the channels in the instance.
		
		if(DACConstraintManager.HVHF_CHANNELS.containsAll(aInstance.getChannels()) || DACConstraintManager.LVHF_CHANNELS.containsAll(aInstance.getChannels()))
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
	public void notifyShutdown() {
		fUHFSolver.notifyShutdown();
		fVHFSolver.notifyShutdown();
	}




}
