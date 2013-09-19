package ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.DACConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.NoGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.sat.CompressedSATBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ClaspSATSolver;

public class ClaspSATSolverSelectorBundle implements ISolverBundle{
	
	private static Logger log = LoggerFactory.getLogger(ClaspSATSolverSelectorBundle.class);
	
	private final IStationManager fStationManager;
	private final IConstraintManager fConstraintManager;
	
	private final ISolver fClaspGeneral;
	private final ISolver fClaspHVHF;
	
	public ClaspSATSolverSelectorBundle(String aClaspLibraryPath, IStationManager aStationManager, IConstraintManager aConstraintManager)
	{
		fStationManager = aStationManager;
		fConstraintManager = aConstraintManager;
		
		SATCompressor aEncoder = new SATCompressor(fStationManager, aConstraintManager);
		IComponentGrouper aGrouper = new NoGrouper();
		
		AbstractCompressedSATSolver aUHFClaspSATsolver =  new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.ORIGINAL_CONFIG_03_13);
		fClaspGeneral = new CompressedSATBasedSolver(aUHFClaspSATsolver, aEncoder, aConstraintManager, aGrouper);
		
		AbstractCompressedSATSolver aHVHFClaspSATsolver = new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.HVHF_CONFIG_09_13);
		fClaspHVHF = new CompressedSATBasedSolver(aHVHFClaspSATsolver, aEncoder, aConstraintManager, aGrouper);
	}
	
	@Override
	public ISolver getSolver(StationPackingInstance aInstance) {
		
		//Return the right solver based on the channels in the instance.
		if(DACConstraintManager.HVHF_CHANNELS.containsAll(aInstance.getChannels()))
		{
			log.info("Returning clasp configured for HVHF.");
			return fClaspHVHF;
		}
		else
		{
			log.info("Returning general configured clasp.");
			return fClaspGeneral;
		}
	}

	@Override
	public IStationManager getStationManager() {
		return fStationManager;
	}

	@Override
	public IConstraintManager getConstraintManager() {
		return fConstraintManager;
	}

	@Override
	public void notifyShutdown() {
		fClaspGeneral.notifyShutdown();
		fClaspHVHF.notifyShutdown();
	}



}
