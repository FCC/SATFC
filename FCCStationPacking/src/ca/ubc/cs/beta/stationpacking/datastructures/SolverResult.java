package ca.ubc.cs.beta.stationpacking.datastructures;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


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
		fResult = aResult;
		fRuntime = aRuntime;
		fAssignment = new HashMap<Integer,Set<Station>>(aAssignment);
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
	
	/*
	private String toStringAssignment(HashMap<Integer,HashSet<Station>> aAssignment)
	{
		String aOutput = "";
		Iterator<Integer> aAssignmentChannelIterator = aAssignment.keySet().iterator();
		while(aAssignmentChannelIterator.hasNext())
		{
			Integer aAssignmentChannel = aAssignmentChannelIterator.next();
			aOutput+=aAssignmentChannel+"(";
			
			HashSet<Station> aAssignedStations = aAssignment.get(aAssignmentChannel);
			Iterator<Station> aAssignedStationsIterator = aAssignedStations.iterator();
			while(aAssignedStationsIterator.hasNext())
			{
				Station aAssignedStation = aAssignedStationsIterator.next();
				aOutput += aAssignedStation.getID();
				if(aAssignedStationsIterator.hasNext())
				{
					aOutput += "-";
				}
			}
			
			aOutput += ")";
			if(aAssignmentChannelIterator.hasNext())
			{
				aOutput += ";";
			}
		}
		return aOutput;
		
	}
	*/
	
	@Override
	public String toString()
	{
		return fResult+","+fRuntime+","+fAssignment;//toStringAssignment(fAssignment);
	}
	
	/*
	public SolverResult valueOf(String aString)
	{
		SATResult aSATResult = SATResult.valueOf(aString.split(",")[0]);
		Double aRuntime = Double.valueOf(aString.split(",")[1]);
		
		HashMap<Integer,HashSet<Station>> aAssignment = new HashMap<Integer,HashSet<Station>>();
		for(String aChannelAssignment : Arrays.asList(aString.split(",")[2].split(";")))
		{
			Integer aChannel = Integer.valueOf(aChannelAssignment.split("(")[0]);
			HashSet<Station> aStations = new HashSet<Station>();
			for(String aStationID : Arrays.asList(aChannelAssignment.split("(")[1].replace(")", "").split("-")))
			{
				aStations.add(new Station(Integer.valueOf(aStationID)));
			}
		}
	}
	*/
}
