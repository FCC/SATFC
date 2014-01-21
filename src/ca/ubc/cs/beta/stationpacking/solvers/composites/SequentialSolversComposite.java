package ca.ubc.cs.beta.stationpacking.solvers.composites;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.SolverHelper;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

public class SequentialSolversComposite implements ISolver{

	private static Logger log = LoggerFactory.getLogger(SequentialSolversComposite.class);
	
	private final List<ISolver> fSolvers;
	
	public SequentialSolversComposite(List<ISolver> aSolvers)
	{
		fSolvers = aSolvers;
	}
	
	@Override
	public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion,
			long aSeed) {
		
		Collection<SolverResult> results = new ArrayList<SolverResult>();
		
		for(int i=0;i<fSolvers.size() && !aTerminationCriterion.hasToStop();i++)
		{
			log.debug("Trying solver {}.",i+1);
			
			SolverResult result = fSolvers.get(i).solve(aInstance, aTerminationCriterion, aSeed);
			results.add(result);
			
			if(result.getResult().equals(SATResult.SAT) || result.getResult().equals(SATResult.UNSAT))
			{
				break;
			}
		}
		
		return SolverHelper.combineResults(results);
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
