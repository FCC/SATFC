package ca.ubc.cs.beta.stationpacking.solvers.termination;


import org.apache.commons.math3.util.FastMath;

import com.google.common.util.concurrent.AtomicDouble;

import ca.ubc.cs.beta.aclib.misc.cputime.CPUTime;

public class CPUTimeTerminationCriterion implements ITerminationCriterion 
{
	private final double fStartingCPUTime;
	private AtomicDouble fExternalEllapsedCPUTime;
	private final double fCPUTimeLimit;

	/**
	 * Create a CPU time termination criterion starting immediately and running for the provided duration (s).
	 * @param aCPUTimeLimit - CPU time duration (s).
	 */
	public CPUTimeTerminationCriterion(double aCPUTimeLimit)
	{
		fExternalEllapsedCPUTime = new AtomicDouble(0.0);
		fCPUTimeLimit = aCPUTimeLimit;
		fStartingCPUTime = getCPUTime();
	}

	private double getCPUTime()
	{
		return CPUTime.getCPUTime()+fExternalEllapsedCPUTime.get();
	}
	
	private double getEllapsedCPUTime()
	{
		return getCPUTime() - fStartingCPUTime;
	}
	
	@Override
	public boolean hasToStop()
	{
		return (getEllapsedCPUTime() >= fCPUTimeLimit);
	}

	@Override
	public double getRemainingTime() {
		return FastMath.max(fCPUTimeLimit-getEllapsedCPUTime(), 0.0);
	}

	@Override
	public void notifyEvent(double aTime) {
		fExternalEllapsedCPUTime.addAndGet(aTime);
	}

	

	
}
