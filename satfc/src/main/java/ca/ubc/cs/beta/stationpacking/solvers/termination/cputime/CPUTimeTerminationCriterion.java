/**
 * Copyright 2016, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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
package ca.ubc.cs.beta.stationpacking.solvers.termination.cputime;


import org.apache.commons.math3.util.FastMath;

import com.google.common.util.concurrent.AtomicDouble;

import ca.ubc.cs.beta.aeatk.misc.cputime.CPUTime;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

public class CPUTimeTerminationCriterion implements ITerminationCriterion 
{
	private final double fStartingCPUTime;
	private AtomicDouble fExternalEllapsedCPUTime;
	private final double fCPUTimeLimit;
    // Note that this will only measure time for JVM threads. Threads in native code DO NOT COUNT TOWARDS CPU TIME.
	private final CPUTime fTimer;
	
	/**
	 * Create a CPU time termination criterion starting immediately and running for the provided duration (s).
	 * @param aCPUTimeLimit - CPU time duration (s).
	 */
	public CPUTimeTerminationCriterion(double aCPUTimeLimit)
	{
		fTimer = new CPUTime();
		
		fExternalEllapsedCPUTime = new AtomicDouble(0.0);
		fCPUTimeLimit = aCPUTimeLimit;
		fStartingCPUTime = getCPUTime();
	}

	private double getCPUTime()
	{
		return fTimer.getCPUTime()+fExternalEllapsedCPUTime.get();
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
