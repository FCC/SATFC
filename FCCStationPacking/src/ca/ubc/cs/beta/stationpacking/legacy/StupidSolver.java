package ca.ubc.cs.beta.stationpacking.legacy;



import java.util.Random;

import ca.ubc.cs.beta.stationpacking.experiment.instance.IInstance;
import ca.ubc.cs.beta.stationpacking.experiment.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SATResult;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SolverResult;


/**
 * <b> FOR TESTING PURPOSE</b> <br>
 * Does not solve given problem instance - returns SAT or UNSAT with input probability after waiting
 *  a randomized amount of time around input wait time.
 * @author afrechet
 *
 */
public class StupidSolver implements ISolver{

	private double fSATprob;
	private double fWaitTime;
	
	private Random fRandomizer;
	
	public StupidSolver(double aSATprobability, double aWaitTime, int aSeed)
	{
		fSATprob = aSATprobability;
		fWaitTime = aWaitTime;
		fRandomizer = new Random(aSeed);
	}
	
	public StupidSolver(double aSATprobability, double aWaitTime)
	{
		fSATprob = aSATprobability;
		fWaitTime = aWaitTime;
		fRandomizer = new Random(1);
	}
	
	@Override
	public SolverResult solve(IInstance aInstance, double aCutoff) {
		
		double aRunTime = (fWaitTime*1000 + fRandomizer.nextDouble()*2000);
		
		try 
		{
			Thread.sleep((long) aRunTime);
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
		if(fRandomizer.nextDouble()<fSATprob)
		{
			return new SolverResult(SATResult.SAT, aRunTime);
		}
		else
		{
			return new SolverResult(SATResult.UNSAT, aRunTime);
		}
	}

}
