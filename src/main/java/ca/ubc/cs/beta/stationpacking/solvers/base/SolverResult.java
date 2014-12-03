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
	
	/**
	 * @param aResult - solver result satisfiability.
	 * @param aRuntime - solver result runtime.
	 * @param aAssignment - solver result witness assignment.
	 */
	public SolverResult(SATResult aResult, double aRuntime, Map<Integer,Set<Station>> aAssignment)
	{
		if(aRuntime<0 && Math.abs(aRuntime)!=0.0)
		{
			throw new IllegalArgumentException("Cannot create a solver result with negative runtime (runtime = "+aRuntime+").");
		}
		
		fResult = aResult;
		fRuntime = aRuntime;
		fAssignment = new HashMap<Integer,Set<Station>>(aAssignment);
	}
	
	/**
	 * @param aResult - solver result satisfiability.
	 * @param aRuntime - solver result runtime.
	 */
	public SolverResult(SATResult aResult, double aRuntime)
	{
		if(aResult.equals(SATResult.SAT))
		{
			throw new IllegalArgumentException("Must provide a station assignment when creating a SAT solver result.");
		}
		
		if(aRuntime<0 && Math.abs(aRuntime)!=0.0)
		{
			throw new IllegalArgumentException("Cannot create a solver result with negative runtime (runtime = "+aRuntime+").");
		}
		
		fResult = aResult;
		fRuntime = aRuntime;
		fAssignment = new HashMap<Integer,Set<Station>>();
	}
	
	/**
	 * Create a TIMEOUT result with the given runtime.
	 * @param aRuntime - runtime
	 * @return a TIMEOUT SolverResult with the given runtime.
	 */
	public static SolverResult createTimeoutResult(double aRuntime)
	{
		return new SolverResult(SATResult.TIMEOUT,aRuntime,new HashMap<Integer,Set<Station>>());
	}
	
    /**
     * @param aResult - a solver result.
     * @param aTime - some time in seconds.
     * @return a new solver result with runtime increased by the given amount of time. 
     */
    public static SolverResult addTime(SolverResult aResult, double aTime)
    {
        return new SolverResult(aResult.getResult(), aResult.getRuntime()+aTime,aResult.getAssignment());
    }
    
	
	/**
	 * @return the satisfiabiltity result.
	 */
	public SATResult getResult(){
		return fResult;
	}
	
	/**
	 * @return the runtime (s).
	 */
	public double getRuntime()
	{
		return fRuntime;
	}
	

	/**
	 * @return the witness assignment.
	 */
	public Map<Integer,Set<Station>> getAssignment()
	{
		return new HashMap<Integer,Set<Station>>(fAssignment); 
	}
	
	
	@Override
	public String toString()
	{
		return fResult+","+fRuntime+","+fAssignment;//toStringAssignment(fAssignment);
	}
	
	/**
	 * @return a parseable string version of the result.
	 */
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