package ca.ubc.cs.beta.stationpacking.datastructures;

/**
 * Container object for the result of a solver executed on a problem instance.
 * @author afrechet
 *
 */
public class SolverResult {
	
	private SATResult fResult;
	private double fRuntime;
	
	public SolverResult(SATResult aResult, double aRuntime)
	{
		fResult = aResult;
		fRuntime = aRuntime;
	}
	
	public SATResult getResult(){
		return fResult;
	}
	
	public double getRuntime()
	{
		return fRuntime;
	}
	
	@Override
	public String toString()
	{
		return fResult+","+fRuntime;
	}
	
}
