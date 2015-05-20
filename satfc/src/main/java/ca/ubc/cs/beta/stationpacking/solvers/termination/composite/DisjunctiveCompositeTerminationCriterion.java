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
package ca.ubc.cs.beta.stationpacking.solvers.termination.composite;

import java.util.Collection;

import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * Composite termination criterion that acts as a disjunction of the provided criteria.
 * @author afrechet
 *
 */
public class DisjunctiveCompositeTerminationCriterion implements ITerminationCriterion{
	
	private static Logger log = LoggerFactory.getLogger(DisjunctiveCompositeTerminationCriterion.class);
	
	private final Collection<ITerminationCriterion> fTerminationCriteria;
	
	public DisjunctiveCompositeTerminationCriterion(Collection<ITerminationCriterion> aTerminationCriteria)
	{
		fTerminationCriteria = aTerminationCriteria;
	}
	
	@Override
	public double getRemainingTime() {
		
		double minTime = Double.POSITIVE_INFINITY;
		
		for(ITerminationCriterion criterion : fTerminationCriteria)
		{
			double criterionTime = criterion.getRemainingTime();
			log.trace("Criterion {} says there is {} s left.",criterion,criterionTime);
			minTime = FastMath.min(minTime, criterionTime);
		}
		
		return minTime;
	}

	@Override
	public boolean hasToStop() {
		for(ITerminationCriterion criterion : fTerminationCriteria)
		{
			if(criterion.hasToStop())
			{
			    log.trace("Criterion {} says we should stop ({} s left).",criterion,criterion.getRemainingTime());
				return true;
			}
		}
		return false;
	}

	@Override
	public void notifyEvent(double aTime) {
		for(ITerminationCriterion criterion : fTerminationCriteria)
		{
			criterion.notifyEvent(aTime);
		}
	}

	

}
