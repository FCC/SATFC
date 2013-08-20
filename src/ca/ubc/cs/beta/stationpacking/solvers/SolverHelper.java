package ca.ubc.cs.beta.stationpacking.solvers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;

public class SolverHelper {

	private SolverHelper()
	{
		//Cannot construct a solver helper object.
	}

	public static SolverResult mergeComponentResults(Collection<SolverResult> aComponentResults){
		double aRuntime = 0.0;
		
		//Merge runtimes as sum of times.
		HashSet<SATResult> aSATResults = new HashSet<SATResult>();
		for(SolverResult aSolverResult : aComponentResults)
		{
			aRuntime += aSolverResult.getRuntime();
			aSATResults.add(aSolverResult.getResult());
		}
		
		//Merge SAT results		
		SATResult aSATResult = SATResult.CRASHED;
		
		if(aSATResults.size()==1)
		{
			aSATResult = aSATResults.iterator().next();
		}
		else if(aSATResults.contains(SATResult.UNSAT))
		{
			aSATResult = SATResult.UNSAT;
		}
		else if(aSATResults.contains(SATResult.INTERRUPTED))
		{
			aSATResult = SATResult.INTERRUPTED;
		}
		else if(aSATResults.contains(SATResult.CRASHED))
		{
			aSATResult = SATResult.CRASHED;
		}
		else if(aSATResults.contains(SATResult.TIMEOUT) || aSATResults.contains(SATResult.KILLED))
		{
			aSATResult = SATResult.TIMEOUT;
		}
		
		//If all runs were killed, it is because we went over time. 
		if(aSATResult.equals(SATResult.KILLED))
		{
			aSATResult = SATResult.TIMEOUT;
		}
		
		
		//Merge assignments
		Map<Integer,Set<Station>> aAssignment = new HashMap<Integer,Set<Station>>();
		if(aSATResult.equals(SATResult.SAT))
		{
			for(SolverResult aComponentResult : aComponentResults)
			{
				Map<Integer,Set<Station>> aComponentAssignment = aComponentResult.getAssignment();
				
				for(Integer aAssignedChannel : aComponentAssignment.keySet())
				{
					if(!aAssignment.containsKey(aAssignedChannel))
					{
						aAssignment.put(aAssignedChannel, new HashSet<Station>());
					}
					aAssignment.get(aAssignedChannel).addAll(aComponentAssignment.get(aAssignedChannel));
				}
			}
		}
		
				
		return new SolverResult(aSATResult,aRuntime,aAssignment);
	}
	
}
