/**
 * Copyright 2014, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
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

/**
 * Composes a list of solvers and executes each one ata time.
 * @author afrechet
 */
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
		
		for(int i=0;i<fSolvers.size();i++)
		{
		    if(aTerminationCriterion.hasToStop())
		    {
		        log.trace("All time spent.");
		        break;
		    }
		    
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
