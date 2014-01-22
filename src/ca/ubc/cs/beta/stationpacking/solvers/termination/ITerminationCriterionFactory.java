package ca.ubc.cs.beta.stationpacking.solvers.termination;

/**
 * Factory for termination criteria. Useful when an object can be asked to solve an instance many time in its life, and thus needs to create a new
 * termination criterion each time.
 * @author afrechet
 */
public interface ITerminationCriterionFactory {

	public ITerminationCriterion getTerminationCriterion();
	
}
