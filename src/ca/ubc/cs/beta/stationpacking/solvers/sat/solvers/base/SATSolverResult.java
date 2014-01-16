package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base;

import java.io.Serializable;
import java.util.HashSet;

import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;

public class SATSolverResult implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private SATResult fResult;
	private double fRuntime;
	private HashSet<Literal> fAssignment;
	
	public SATSolverResult(SATResult aResult, double aRuntime, HashSet<Literal> aAssignment)
	{
		fResult = aResult;
		fRuntime = aRuntime;
		fAssignment = new HashSet<Literal>(aAssignment);
	}
	
	public SATResult getResult(){
		return fResult;
	}
	
	public double getRuntime()
	{
		return fRuntime;
	}
	
	public HashSet<Literal> getAssignment()
	{
		return new HashSet<Literal>(fAssignment); 
	}
	
	
	@Override
	public String toString()
	{
		return fResult+","+fRuntime+","+fAssignment;
	}

}
