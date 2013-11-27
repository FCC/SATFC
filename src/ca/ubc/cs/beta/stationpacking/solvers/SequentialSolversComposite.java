package ca.ubc.cs.beta.stationpacking.solvers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;

public class SequentialSolversComposite implements ISolver{

	private static Logger log = LoggerFactory.getLogger(SequentialSolversComposite.class);
	
	private final List<ISolver> fSolvers;
	
	public SequentialSolversComposite(List<ISolver> aSolvers)
	{
		fSolvers = aSolvers;
	}
	
	@Override
	public SolverResult solve(StationPackingInstance aInstance, double aCutoff,
			long aSeed) {
		double aRemainingCutoff = aCutoff;
		
		Collection<SolverResult> results = new ArrayList<SolverResult>();
		
		for(int i=0;i<fSolvers.size() && aRemainingCutoff>0.1;i++)
		{
			log.info("Trying solver {}.",i);
			
			SolverResult result = fSolvers.get(i).solve(aInstance, aRemainingCutoff, aSeed);
			results.add(result);
			
			aRemainingCutoff -= result.getRuntime();
			if(result.getResult().equals(SATResult.SAT) || result.getResult().equals(SATResult.UNSAT))
			{
				break;
			}
		}
		
		return SolverHelper.mergeComponentResults(results);
	}

	@Override
	public void interrupt() throws UnsupportedOperationException {
		for(ISolver solver : fSolvers)
		{
			solver.interrupt();
		}
	}

	@Override
	public void notifyShutdown() {
		for(ISolver solver : fSolvers)
		{
			solver.notifyShutdown();
		}
		
	}



}
