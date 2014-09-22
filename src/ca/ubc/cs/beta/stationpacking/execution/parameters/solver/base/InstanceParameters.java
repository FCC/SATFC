package ca.ubc.cs.beta.stationpacking.execution.parameters.solver.base;

import java.util.HashMap;
import java.util.Set;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.converters.PreviousAssignmentConverter;
import ca.ubc.cs.beta.stationpacking.execution.parameters.converters.StationDomainsConverter;

import com.beust.jcommander.Parameter;

/**
 * Defines a station packing problem instance from basic values.
 * @author afrechet
 */
@UsageTextField(title="FCC Station Packing Packing Problem Instance Options",description="Parameters defining a single station packing problem.")
public class InstanceParameters extends AbstractOptions {

	
	@Parameter(names = "-DOMAINS", description = "Map taking station IDs to reduced channel domains in which to pack them (e.g. 1:14,15,16;2:14,15 means pack station 1 into channels 14,15 and 16 and station 2 into channels 14 and 15.)", converter=StationDomainsConverter.class, required=true)
	private HashMap<Integer,Set<Integer>> fDomains;
	/**
	 * @return the channel domain on which to pack each station.
	 */
	public HashMap<Integer,Set<Integer>> getDomains()
	{
		return fDomains;
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
	

}
