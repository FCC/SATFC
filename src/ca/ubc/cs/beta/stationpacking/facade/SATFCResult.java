package ca.ubc.cs.beta.stationpacking.facade;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;

public class SATFCResult
{
	private final HashMap<Integer,Integer> fWitnessAssignment;
	private final SATResult fResult;
	private final double fRuntime;
	
	public SATFCResult(SATResult aResult, double aRuntime, Map<Integer,Integer> aWitnessAssignment)
	{
		fResult = aResult;
		fRuntime = aRuntime;
		fWitnessAssignment = new HashMap<Integer,Integer>(aWitnessAssignment);
	}
	
	public SATResult getResult()
	{
		return fResult;
	}
	
	public double getRuntime()
	{
		return fRuntime;
	}
	
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