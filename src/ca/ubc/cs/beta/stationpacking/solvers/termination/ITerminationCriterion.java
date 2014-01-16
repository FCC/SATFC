package ca.ubc.cs.beta.stationpacking.solvers.termination;

/**
 * <p>
 * In charge of managing time resource and termination.
 * </p>
 * <p>
 * Even though time is measured in seconds, there is no guarantee about what kind of time (<i>e.g.</i> walltime, CPU time, ...) we're dealing with.
 * This depends on the implementation.
 * <p>
 * 
 * @author afrechet
 */
public interface ITerminationCriterion {
	
	/**
	 * <p>
	 * Return how much time (s) is remaining before the termination criterion is met.
	 * </p>
	 * <p>
	 * This is used to allocate time to blocking (synchronous) processes. <br>
	 * </p>
	 * @return how much time (s) is remaining before the termination criterion is met.
	 */
	public double getRemainingTime();
	
	/**
	 * Signals if the termination criterion is met.
	 * @return true if and only if termination criterion is met.
	 */
	public boolean hasToStop();
	
	/**
	 * Notify the termination criterion that an (external) event was completed.
	 * @param aTime - the time the event took to complete.
	 */
	public void notifyEvent(double aTime);
	
}
