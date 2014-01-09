package ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.simplebounderpresolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.ASolverBundle;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.certifierpresolvers.SimpleBounderPresolver;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;

public class SimpleBounderPresolverBundle extends ASolverBundle {

	private static Logger log = LoggerFactory.getLogger(SimpleBounderPresolverBundle.class);
	
	private final SimpleBounderPresolver UHFpresolver;
	private final SimpleBounderPresolver HVHFpresolver;
	private final SimpleBounderPresolver LVHFpresolver;
	
	public SimpleBounderPresolverBundle(IStationManager aStationManager, IConstraintManager aConstraintManager, ISolverBundle aISolverBundle)
	{
		super(aStationManager,aConstraintManager);
		
		log.debug("Initializing the UHF, LHVHF, HVHF simple bounder pre-solvers.");
		StationPackingInstance UHFinstance = new StationPackingInstance(aStationManager.getStations(), StationPackingUtils.UHF_CHANNELS);
		UHFpresolver = new SimpleBounderPresolver(aISolverBundle.getSolver(UHFinstance), 
				this.getConstraintManager());
		
		StationPackingInstance HVHFinstance = new StationPackingInstance(aStationManager.getStations(), StationPackingUtils.HVHF_CHANNELS);
		HVHFpresolver = new SimpleBounderPresolver(aISolverBundle.getSolver(HVHFinstance), 
				this.getConstraintManager());
		
		StationPackingInstance LVHFinstance = new StationPackingInstance(aStationManager.getStations(), StationPackingUtils.LVHF_CHANNELS);
		LVHFpresolver = new SimpleBounderPresolver(aISolverBundle.getSolver(LVHFinstance), 
				this.getConstraintManager());
		
	}
	
	
	@Override
	public ISolver getSolver(StationPackingInstance aInstance) {
		if(StationPackingUtils.HVHF_CHANNELS.containsAll(aInstance.getChannels()))
		{
			return HVHFpresolver;
		}
		else if(StationPackingUtils.LVHF_CHANNELS.containsAll(aInstance.getChannels()))
		{
			return LVHFpresolver;
		}
		else if(StationPackingUtils.UHF_CHANNELS.containsAll(aInstance.getChannels()))
		{
			return UHFpresolver;
		}
		else
		{
			throw new IllegalArgumentException("Instance channel set ("+aInstance.getChannels()+") not part of any recognized band.");
		}
	}

	@Override
	public void notifyShutdown() {
		// No shutdown necessary.
	}

	

}
