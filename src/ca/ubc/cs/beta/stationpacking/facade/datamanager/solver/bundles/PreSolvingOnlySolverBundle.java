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
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.NoGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.composites.SequentialSolversComposite;
import ca.ubc.cs.beta.stationpacking.solvers.sat.CompressedSATBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.ClaspSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.termination.cputime.CPUTimeTerminationCriterionFactory;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;

public class PreSolvingOnlySolverBundle extends ASolverBundle {

	private static Logger log = LoggerFactory.getLogger(PreSolvingOnlySolverBundle.class);
	
	private final ISolver fUHFSolver;
	private final ISolver fVHFSolver;
	
	public PreSolvingOnlySolverBundle(String aClaspLibraryPath,
			IStationManager aStationManager,
			IConstraintManager aConstraintManager) {
		super(aStationManager, aConstraintManager);
		
		log.info("Solver selector: PRE-SOLVING ONLY.");
		
		//Initialize clasp.
		log.warn("Initializing clasps with internal configurations.");
		
		SATCompressor aCompressor = new SATCompressor(this.getConstraintManager());
		IComponentGrouper aGrouper = new NoGrouper();
		
		log.debug("Initializing clasp solvers.");
		
		AbstractCompressedSATSolver aUHFClaspSATsolver = new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.ALL_CONFIG_11_13);
		ISolver UHFClaspBasedSolver = new CompressedSATBasedSolver(aUHFClaspSATsolver, aCompressor,  this.getConstraintManager(), aGrouper);
		
		AbstractCompressedSATSolver aHVHFClaspSATsolver = new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.HVHF_CONFIG_09_13);
		ISolver VHFClaspBasedSolver = new CompressedSATBasedSolver(aHVHFClaspSATsolver, aCompressor,  this.getConstraintManager(), aGrouper);
		
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
		
		if(StationPackingUtils.HVHF_CHANNELS.containsAll(aInstance.getChannels()) || StationPackingUtils.LVHF_CHANNELS.containsAll(aInstance.getChannels()))
		{
			
			return fVHFSolver;
		}
		else
		{
			return fUHFSolver;
		}
	}

	@Override
	public void notifyShutdown() {
		fUHFSolver.notifyShutdown();
		fVHFSolver.notifyShutdown();
	}



}
