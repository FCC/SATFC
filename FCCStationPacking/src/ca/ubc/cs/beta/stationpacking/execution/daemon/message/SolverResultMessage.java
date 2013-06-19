package ca.ubc.cs.beta.stationpacking.execution.daemon.message;

import java.util.HashMap;
import java.util.HashSet;

import ca.ubc.cs.beta.stationpacking.datastructures.SATResult;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;

/**
 * Message containing solver result. Note the need to change the representation of the solver result in a mememto object to reduce
 * the size of the message.
 * @author afrechet
 *
 */
public class SolverResultMessage implements IMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final HashMap<Integer,HashSet<Integer>> fAssignment;
	private final double fRuntime;
	private final SATResult fSATResult;
	
	/**
	 * Message containing solver result information.
	 * @param aSolverResult
	 */
	public SolverResultMessage(SolverResult aSolverResult)
	{
		fAssignment = new HashMap<Integer,HashSet<Integer>>();
		for(Integer aChannel : aSolverResult.getAssignment().keySet())
		{
			HashSet<Integer> aAssignedStations = new HashSet<Integer>();
			for(Station aStation : aSolverResult.getAssignment().get(aChannel))
			{
				aAssignedStations.add(aStation.getID());
			}
			fAssignment.put(aChannel, aAssignedStations);
		}
		fRuntime = aSolverResult.getRuntime();
		fSATResult = aSolverResult.getResult();
	}
	
	public HashMap<Integer,HashSet<Integer>> getAssignment()
	{
		return fAssignment;
	}
	public double getRuntime()
	{
		return fRuntime;
	}
	public SATResult getSATresult()
	{
		return fSATResult;
	}
	
	@Override
	public String toString(){
		return "Solver result message - "+fSATResult+","+fRuntime+","+fAssignment;
	}
	
	
}
