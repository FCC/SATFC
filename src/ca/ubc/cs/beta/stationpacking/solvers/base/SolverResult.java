package ca.ubc.cs.beta.stationpacking.solvers.base;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;


/**
 * Container object for the result of a solver executed on a problem instance.
 * @author afrechet
 *
 */
public class SolverResult implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private SATResult fResult;
	private double fRuntime;
	private Map<Integer,Set<Station>> fAssignment;
	
	public SolverResult(SATResult aResult, double aRuntime, Map<Integer,Set<Station>> aAssignment)
	{
		if(aRuntime<=0)
		{
			throw new IllegalArgumentException("Cannot create a solver result with negative runtime (runtime = "+aRuntime+").");
		}
		
		fResult = aResult;
		fRuntime = aRuntime;
		fAssignment = new HashMap<Integer,Set<Station>>(aAssignment);
	}
	
	public static SolverResult createTimeoutResult(double aRuntime)
	{
		return new SolverResult(SATResult.TIMEOUT,aRuntime,new HashMap<Integer,Set<Station>>());
	}
	
	public SATResult getResult(){
		return fResult;
	}
	
	public double getRuntime()
	{
		return fRuntime;
	}
	
	public Map<Integer,Set<Station>> getAssignment()
	{
		return new HashMap<Integer,Set<Station>>(fAssignment); 
	}
	
	
	@Override
	public String toString()
	{
		return fResult+","+fRuntime+","+fAssignment;//toStringAssignment(fAssignment);
	}
	
	public String toParsableString()
	{
		String aOutput = fResult.toString()+","+fRuntime+",";
		
		Iterator<Integer> aChannelIterator = fAssignment.keySet().iterator();
		while(aChannelIterator.hasNext())
		{
			Integer aChannel = aChannelIterator.next();
			
			aOutput += aChannel+"-";
			
			Iterator<Station> aAssignedStationIterator = fAssignment.get(aChannel).iterator();
			
			while(aAssignedStationIterator.hasNext())
			{
				Station aAssignedStation = aAssignedStationIterator.next();
				
				aOutput += aAssignedStation.getID();
				
				if(aAssignedStationIterator.hasNext())
				{
					aOutput += "_";
				}
				
			}
			
			if(aChannelIterator.hasNext())
			{
				aOutput +=";";
			}
			
		}
		
		
		return aOutput;
	}

}
