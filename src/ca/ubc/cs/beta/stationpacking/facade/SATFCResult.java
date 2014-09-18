package ca.ubc.cs.beta.stationpacking.facade;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;

/**
 * Container for the result returned by a SATFC facade.
 * @author afrechet
 */
public class SATFCResult
{
	private final HashMap<Integer,Integer> fWitnessAssignment;
	private final SATResult fResult;
	private final double fRuntime;
	
	/**
	 * @param aResult - the satisfiability result.
	 * @param aRuntime - the time (s) it took to get to such result.
	 * @param aWitnessAssignment - the witness assignment
	 */
	public SATFCResult(SATResult aResult, double aRuntime, Map<Integer,Integer> aWitnessAssignment)
	{
		fResult = aResult;
		fRuntime = aRuntime;
		fWitnessAssignment = new HashMap<Integer,Integer>(aWitnessAssignment);
	}
	
	/**
	 * @return the satisfiability result.
	 */
	public SATResult getResult()
	{
		return fResult;
	}
	
	/**
	 * @return the runtime.
	 */
	public double getRuntime()
	{
		return fRuntime;
	}
	
	/**
	 * @return the witness assignment (only non-empty if result is SAT).
	 */
	public Map<Integer,Integer> getWitnessAssignment()
	{
		return Collections.unmodifiableMap(fWitnessAssignment);
	}
	
	@Override
	public String toString()
	{
		return fRuntime+","+fResult+","+fWitnessAssignment.toString();
	}
}