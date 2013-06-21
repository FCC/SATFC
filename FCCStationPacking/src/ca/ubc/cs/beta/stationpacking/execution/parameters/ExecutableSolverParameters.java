package ca.ubc.cs.beta.stationpacking.execution.parameters;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;

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

	public Instance getInstance() throws Exception
	{
		Logger log = LoggerFactory.getLogger(ExecutableSolverParameters.class);
		log.info("Getting instance...");
		
		Set<Integer> aPackingChannels = ProblemInstanceParameters.getPackingChannels();
		
		Set<Station> aStations = SolverParameters.RepackingDataParameters.getDACStationManager().getStations();
		Set<Integer> aStationIDs = ProblemInstanceParameters.getPackingStationIDs();
		
		Set<Station> aPackingStations = new HashSet<Station>();
		for(Station aStation : aStations)
		{
			if(aStationIDs.contains(aStation.getID()))
			{
				aPackingStations.add(aStation);
			}
		}
		
		return new Instance(aPackingStations,aPackingChannels);	
	}
	
	public ISolver getSolver() throws Exception
	{
		Logger log = LoggerFactory.getLogger(ExecutableSolverParameters.class);
		log.info("Getting solver...");
		
		return SolverParameters.getTAESolver();
	}
	

	
}