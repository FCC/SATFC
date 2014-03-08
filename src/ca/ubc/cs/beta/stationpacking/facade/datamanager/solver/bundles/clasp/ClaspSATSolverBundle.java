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

public class ClaspSATSolverBundle extends ASolverBundle{
	
	private static Logger log = LoggerFactory.getLogger(ClaspSATSolverBundle.class);
	
	private final ISolver fClaspGeneral;
	private final ISolver fClaspHVHF;
	private final ISolver fClaspUHF;
	
	public ClaspSATSolverBundle(String aClaspLibraryPath, IStationManager aStationManager, IConstraintManager aConstraintManager)
	{
		super(aStationManager,aConstraintManager);
		
		log.debug("Initializing clasp selector bundle.");
		
		SATCompressor aCompressor = new SATCompressor(this.getStationManager(), this.getConstraintManager());
		IComponentGrouper aGrouper = new NoGrouper();
		
//		log.info("Creating incremental library clasp.");
//		SATEncoder aSATEncoder = new SATEncoder(fStationManager,aConstraintManager);
//		AbstractSATSolver aIncrementalClaspSATsolver = new IncrementalClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.ORIGINAL_CONFIG_03_13, 1);
//		fClaspGeneral = new SATBasedSolver(aIncrementalClaspSATsolver, aSATEncoder, aConstraintManager, aGrouper);
		
		log.debug("Initializing clasp solvers.");
		AbstractCompressedSATSolver aClaspSATsolver =  new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.ALL_CONFIG_11_13);
		fClaspGeneral = new CompressedSATBasedSolver(aClaspSATsolver, aCompressor,  this.getConstraintManager(), aGrouper);
		
		AbstractCompressedSATSolver aUHFClaspSATsolver = new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.ALL_CONFIG_11_13);
		fClaspUHF = new CompressedSATBasedSolver(aUHFClaspSATsolver, aCompressor,  this.getConstraintManager(), aGrouper);
		
		AbstractCompressedSATSolver aHVHFClaspSATsolver = new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.HVHF_CONFIG_09_13);
		fClaspHVHF = new CompressedSATBasedSolver(aHVHFClaspSATsolver, aCompressor,  this.getConstraintManager(), aGrouper);
	}
	
	@Override
	public ISolver getSolver(StationPackingInstance aInstance) {
		
		//Return the right solver based on the channels in the instance.
		if(StationPackingUtils.HVHF_CHANNELS.containsAll(aInstance.getChannels()) || StationPackingUtils.LVHF_CHANNELS.containsAll(aInstance.getChannels()))
		{
			log.debug("Returning clasp configured for VHF (September 2013).");
			return fClaspHVHF;
		}
		else if(StationPackingUtils.UHF_CHANNELS.containsAll(aInstance.getChannels()))
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
	public void notifyShutdown() {
		fClaspGeneral.notifyShutdown();
		fClaspHVHF.notifyShutdown();
	}



}
