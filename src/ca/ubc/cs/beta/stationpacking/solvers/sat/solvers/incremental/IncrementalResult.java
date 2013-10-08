package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental;

import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;

public class IncrementalResult {

	private final SATResult fSATResult;
	private final String fAssignment;
	private final double fRuntime;
	
	public IncrementalResult(SATResult satResult, String assignment, double runtime)
	{
		fSATResult = satResult;
		fAssignment = assignment;
		fRuntime = runtime;
	}
	
	public SATResult getSATResult() {
		return fSATResult;
	}
	public String getAssignment() {
		return fAssignment;
	}
	public double getRuntime() {
		return fRuntime;
	}
	
	
	
}
