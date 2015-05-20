/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
 *
 * This file is part of SATFC.
 *
 * SATFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SATFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
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
	
	/**
	 * @param aComponentResults
	 * @return the combined the results of solving a single station packing instance in different ways.
	 */
	public static SolverResult combineResults(Collection<SolverResult> aComponentResults)
	{
		double runtime = 0.0;
		HashSet<SATResult> SATresults = new HashSet<SATResult>();
		
		for(SolverResult solverResult : aComponentResults)
		{
			runtime += solverResult.getRuntime();
			SATresults.add(solverResult.getResult());
		}
		
		SATResult SATresult = SATResult.CRASHED;
		//Combine SAT results.
		if(SATresults.isEmpty())
		{
			SATresult = SATResult.TIMEOUT;
		}
		else if(SATresults.size()==1)
		{
			SATresult = SATresults.iterator().next();
		}
		else if(SATresults.contains(SATResult.SAT))
		{
			SATresult = SATResult.SAT;
		}
		else if(SATresults.contains(SATResult.UNSAT))
		{
			SATresult = SATResult.UNSAT;
		}
		else if(SATresults.contains(SATResult.INTERRUPTED))
		{
			SATresult = SATResult.INTERRUPTED;
		}
		else if(SATresults.contains(SATResult.CRASHED))
		{
			SATresult = SATResult.CRASHED;
		}
		else if(SATresults.contains(SATResult.TIMEOUT) || SATresults.contains(SATResult.KILLED))
		{
			SATresult = SATResult.TIMEOUT;
		}
		
		if(SATresult.equals(SATResult.KILLED))
		{
			SATresult = SATResult.TIMEOUT;
		}
		
		//Find assignment.
		Map<Integer,Set<Station>> assignment = new HashMap<Integer,Set<Station>>();
		if(SATresult.equals(SATResult.SAT))
		{
			for(SolverResult solverResult : aComponentResults)
			{
				if(solverResult.getResult().equals(SATResult.SAT))
				{
					assignment = solverResult.getAssignment();
					break;
				}
			}
		}
		
		return new SolverResult(SATresult,runtime,assignment);
		
	}
	
	
	/**
	 * @param aComponentResults
	 * @return the merged the results of solving multiple disconnected components of a same station packing problem.
	 */
	public static SolverResult mergeComponentResults(Collection<SolverResult> aComponentResults){

		//Merge runtimes as sum of times.
		HashSet<SATResult> aSATResults = new HashSet<SATResult>();
		double runtime = 0.0;
		for(SolverResult aSolverResult : aComponentResults)
		{
			aSATResults.add(aSolverResult.getResult());
			runtime += aSolverResult.getRuntime();
		}
		
		//Merge SAT results		
		SATResult aSATResult = SATResult.CRASHED;
		
		if(aSATResults.isEmpty())
		{
			aSATResult = SATResult.TIMEOUT;
		}
		else if(aSATResults.size()==1)
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
		
				
		return new SolverResult(aSATResult,runtime,aAssignment);
	}
	
}
