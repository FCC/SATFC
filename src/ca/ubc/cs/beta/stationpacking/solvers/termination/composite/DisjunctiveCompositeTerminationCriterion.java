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
