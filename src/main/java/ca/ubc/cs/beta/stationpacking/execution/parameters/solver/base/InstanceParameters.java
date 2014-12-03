package ca.ubc.cs.beta.stationpacking.execution.parameters.solver.base;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.execution.parameters.converters.PreviousAssignmentConverter;
import ca.ubc.cs.beta.stationpacking.execution.parameters.converters.StationDomainsConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.Parameter;

/**
 * Defines a station packing problem instance from basic values.
 * @author afrechet
 */
@UsageTextField(title="FCC Station Packing Packing Problem Instance Options",description="Parameters defining a single station packing problem.")
public class InstanceParameters extends AbstractOptions {
	
	@Parameter(names = "-PACKING-CHANNELS", description = "List of channels to pack into.")
	private List<String> fPackingChannels = null;
	/**
	 * @return the instance's packing channels.
	 */
	public HashSet<Integer> getPackingChannels()
	{
		Logger log = LoggerFactory.getLogger(InstanceParameters.class);
		log.debug("Getting packing channels...");
		
		HashSet<Integer> aPackingChannels = new HashSet<Integer>();
		
		if(fPackingChannels != null)
		{
			for(String aChannel : fPackingChannels)
			{
				aPackingChannels.add(Integer.valueOf(aChannel));
			}
		}
		else
		{
			for(Integer stationID : fDomains.keySet())
			{
				aPackingChannels.addAll(fDomains.get(stationID));
			}
		}
		return aPackingChannels;
	}
	
	@Parameter(names = "-PACKING-STATIONS", description = "List of stations to pack.")
	private List<String> fPackingStations = null;
	/**
	 * @return the IDs of the packing stations.
	 */
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
	/**
	 * @return the channel domain on which to pack each station.
	 */
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
	/**
	 * @return the valid previous assignment.
	 */
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

	/**
     * The instance's cutoff time (s).
     */
	@Parameter(names = "-CUTOFF", description = "Time allowed to the feasibility checker (in seconds).")
	public double Cutoff = 60.0;

    /**
     * The instance's seed.
     */	
	@Parameter(names = "-SEED", description = "(Random) seed given to the feasibility checker.")
	public long Seed = 1;

    /**
     * The instance station config foldername.
     */	
	@Parameter(names = "-DATA-FOLDERNAME",description = "station config data folder name", required=true)
    public String fDataFoldername;
	
	@Override
	public String toString()
	{
		return "("+getDomains().toString()+","+getPreviousAssignment().toString()+","+Cutoff+","+Seed+","+fDataFoldername+")";
	}
	

}