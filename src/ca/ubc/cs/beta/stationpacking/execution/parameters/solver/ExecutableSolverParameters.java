package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.repackingdata.RepackingDataParameters;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;

import com.beust.jcommander.ParametersDelegate;

/**
 * Parameters for an executable solver.
 * @author afrechet
 *
 */
@UsageTextField(title="FCC StationPacking Main Solver Options",description="Parameters required to solve a single instance using a stand-alone stateless solver.")
public class ExecutableSolverParameters extends AbstractOptions {	
	
	//Solver parameters
	@ParametersDelegate
	public TAESolverParameters SolverParameters = new TAESolverParameters();

	//Problem instance parameters.
	@ParametersDelegate
	public InstanceParameters ProblemInstanceParameters = new InstanceParameters();

	//(Global) Data parameters
	@ParametersDelegate
	public RepackingDataParameters RepackingDataParameters = new RepackingDataParameters();
	
	public StationPackingInstance getInstance()
	{
		Logger log = LoggerFactory.getLogger(ExecutableSolverParameters.class);
		log.info("Getting instance...");
		
		Set<Integer> aPackingChannels = ProblemInstanceParameters.getPackingChannels();
		
		IStationManager aStationManager = RepackingDataParameters.getDACStationManager();
		Set<Integer> aStationIDs = ProblemInstanceParameters.getPackingStationIDs();
		
		Set<Station> aPackingStations = new HashSet<Station>();
		
		for(Integer aStationID : aStationIDs)
		{
			aPackingStations.add(aStationManager.getStationfromID(aStationID));
		}
		
		return new StationPackingInstance(aPackingStations,aPackingChannels);	
	}
	
	public ISolver getSolver()
	{
		Logger log = LoggerFactory.getLogger(ExecutableSolverParameters.class);
		log.info("Getting solver...");
		
		IStationManager aStationManager = RepackingDataParameters.getDACStationManager();
		IConstraintManager aConstraintManager = RepackingDataParameters.getDACConstraintManager(aStationManager);
		
		return SolverParameters.getSolver(aStationManager,aConstraintManager);
	}
	

	
}