package ca.ubc.cs.beta.stationpacking.solvers.base;

import java.io.Serializable;
import java.util.HashSet;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Litteral;

public class SATSolverResult implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private SATResult fResult;
	private double fRuntime;
	private HashSet<Litteral> fAssignment;
	
	public SATSolverResult(SATResult aResult, double aRuntime, HashSet<Litteral> aAssignment)
	{
		fResult = aResult;
		fRuntime = aRuntime;
		fAssignment = new HashSet<Litteral>(aAssignment);
	}
	
	public SATResult getResult(){
		return fResult;
	}
	
	public double getRuntime()
	{
		return fRuntime;
	}
	
	public HashSet<Litteral> getAssignment()
	{
		return new HashSet<Litteral>(fAssignment); 
	}
	
	
	@Override
	public String toString()
	{
		return fResult+","+fRuntime+","+fAssignment;
	}

}
