package ca.ubc.cs.beta.stationpacking.execution.parameters.solver.base;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;

import com.beust.jcommander.Parameter;

@UsageTextField(title="FCC Station Packing Packing Problem Instance Options",description="Parameters defining a single station packing problem.")
public class InstanceParameters extends AbstractOptions {
	
	@Parameter(names = "-PACKING_CHANNELS", description = "List of channels to pack into.", required = true)
	private List<String> fPackingChannels = Arrays.asList("14" ,"15" ,"16" ,"17" ,"18" ,"19" ,"20" ,"21" ,"22" ,"23" ,"24" ,"25" ,"26" ,"27" ,"28" ,"29" ,"30");
	public HashSet<Integer> getPackingChannels()
	{
		Logger log = LoggerFactory.getLogger(InstanceParameters.class);
		log.info("Getting packing channels...");
		
		HashSet<Integer> aPackingChannels = new HashSet<Integer>();
		for(String aChannel : fPackingChannels)
		{
			aPackingChannels.add(Integer.valueOf(aChannel));
		}
		return aPackingChannels;
	}
	
	@Parameter(names = "-PACKING_STATIONS", description = "List of stations to pack.", required = true)
	private List<String> fPackingStations;
	public HashSet<Integer> getPackingStationIDs()
	{
		
		Logger log = LoggerFactory.getLogger(InstanceParameters.class);
		log.info("Getting packing stations...");
		
		HashSet<Integer> aPackingStations = new HashSet<Integer>();
		for(String aStationID : fPackingStations)
		{
			aPackingStations.add(Integer.valueOf(aStationID));
		}
		return aPackingStations;
	}
	
	@Parameter(names = "-CUTOFF", description = "Time allowed to the feasibility checker (in seconds).")
	public double Cutoff = 1800.0;
	
	@Parameter(names = "-SEED", description = "(Random) seed given to the feasibility checker.")
	public long Seed = 1;

}
