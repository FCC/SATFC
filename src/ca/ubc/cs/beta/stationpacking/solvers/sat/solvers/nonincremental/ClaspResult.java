package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental;

import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;

public class ClaspResult {

	private final SATResult fSATResult;
	private final int[] fAssignment;
	private final double fRuntime;
	
	public ClaspResult(SATResult satResult, int[] assignment, double runtime)
	{
		fSATResult = satResult;
		fAssignment = assignment;
		fRuntime = runtime;
	}
	
	public SATResult getSATResult() {
		return fSATResult;
	}
	public int[] getAssignment() {
		return fAssignment;
	}
	public double getRuntime() {
		return fRuntime;
	}
	
	
	
}
