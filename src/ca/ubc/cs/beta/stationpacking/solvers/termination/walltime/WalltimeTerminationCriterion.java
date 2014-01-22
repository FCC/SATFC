package ca.ubc.cs.beta.stationpacking.solvers.termination.walltime;

import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

public class WalltimeTerminationCriterion implements ITerminationCriterion {

	private final long fEndTimeMilli;
	
	/**
	 * Create a wallclock time termination criterion starting immediately and running for the provided duration (s).
	 * @param aWalltimeLimit - wallclock duration (s).
	 */
	public WalltimeTerminationCriterion(double aWalltimeLimit)
	{
		this(System.currentTimeMillis(),aWalltimeLimit);
	}
	
	/**
	 * Create a wallclock time termination criterion starting at the given time (ms) and running for the provided duration (s).
	 * @param aApplicationStartTimeMilli - starting time (ms).
	 * @param aWalltimeLimit - wallclock duration (s).
	 */
	private WalltimeTerminationCriterion(long aApplicationStartTimeMilli, double aWalltimeLimit)
	{
		this.fEndTimeMilli = aApplicationStartTimeMilli +  (long) (aWalltimeLimit *1000);
		
	}
	@Override
	public boolean hasToStop() {
		return (System.currentTimeMillis() >= fEndTimeMilli);
		
	}

	@Override
	public double getRemainingTime() {
		return (fEndTimeMilli-System.currentTimeMillis())/1000.0;
	}
	@Override
	public void notifyEvent(double aTime) {
		//Do not need to account for any additional (parallel) time with walltime.
		
	}
	

	
	
}
