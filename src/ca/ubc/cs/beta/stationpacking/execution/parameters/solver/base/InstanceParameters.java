package ca.ubc.cs.beta.stationpacking.execution.parameters.solver.base;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.converters.PreviousAssignmentConverter;
import ca.ubc.cs.beta.stationpacking.execution.parameters.converters.StationDomainsConverter;

import com.beust.jcommander.Parameter;

@UsageTextField(title="FCC Station Packing Packing Problem Instance Options",description="Parameters defining a single station packing problem.")
public class InstanceParameters extends AbstractOptions {
	
	@Parameter(names = "-PACKING-CHANNELS", description = "List of channels to pack into.")
	private List<String> fPackingChannels = Arrays.asList("14" ,"15" ,"16" ,"17" ,"18" ,"19" ,"20" ,"21" ,"22" ,"23" ,"24" ,"25" ,"26" ,"27" ,"28" ,"29" ,"30");
	public HashSet<Integer> getPackingChannels()
	{
		Logger log = LoggerFactory.getLogger(InstanceParameters.class);
		log.debug("Getting packing channels...");
		
		HashSet<Integer> aPackingChannels = new HashSet<Integer>();
		for(String aChannel : fPackingChannels)
		{
			aPackingChannels.add(Integer.valueOf(aChannel));
		}
		return aPackingChannels;
	}
	
	@Parameter(names = "-PACKING-STATIONS", description = "List of stations to pack.")
	private List<String> fPackingStations;
	public HashSet<Integer> getPackingStationIDs()
	{
		
		Logger log = LoggerFactory.getLogger(InstanceParameters.class);
		log.debug("Getting packing stations...");
		
		HashSet<Integer> aPackingStations = new HashSet<Integer>();
		
		if(fPackingStations != null)
		{
			for(String aStationID : fPackingStations)
			{
				aPackingStations.add(Integer.valueOf(aStationID));
			}
		}
		else
		{
			aPackingStations.addAll(fDomains.keySet());
		}
		
		return aPackingStations;
	}
	
	@Parameter(names = "-DOMAINS", description = "Map taking station IDs to reduced domain set (e.g. 1:14,15,16;2:14,15)", converter=StationDomainsConverter.class, required=true)
	private HashMap<Integer,Set<Integer>> fDomains;
	public HashMap<Integer,Set<Integer>> getDomains()
	{
		if(fDomains==null)
		{
			return new HashMap<Integer,Set<Integer>>();
		}
		else
		{
			return fDomains;
		}
	}
	
	@Parameter(names = "-PREVIOUS-ASSIGNMENT", description = "Map taking (some) station IDs to valid previous channel assignment.", converter=PreviousAssignmentConverter.class)
	private HashMap<Integer,Integer> fPreviousAssignment;
	public HashMap<Integer,Integer> getPreviousAssignment()
	{
		if(fPreviousAssignment== null)
		{
			return new HashMap<Integer,Integer>();
		}
		else
		{
			return fPreviousAssignment;
		}
	}
	
	@Parameter(names = "-CUTOFF", description = "Time allowed to the feasibility checker (in seconds).")
	public double Cutoff = 60.0;
	
	@Parameter(names = "-SEED", description = "(Random) seed given to the feasibility checker.")
	public long Seed = 1;

}
