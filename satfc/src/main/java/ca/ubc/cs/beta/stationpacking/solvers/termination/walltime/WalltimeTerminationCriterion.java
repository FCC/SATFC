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
package ca.ubc.cs.beta.stationpacking.solvers.termination.walltime;

import org.apache.commons.math.util.FastMath;

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
	    fEndTimeMilli = aApplicationStartTimeMilli +  (long)(aWalltimeLimit *1000);
	}
	@Override
	public boolean hasToStop() {
		return getRemainingTime()<=0;
	}

	@Override
	public double getRemainingTime() {
	    final long currentTime = System.currentTimeMillis();
	    final double remainingTime = (fEndTimeMilli-currentTime)/1000.0; 
		return FastMath.max(remainingTime, 0);
	}

	@Override
	public void notifyEvent(double aTime) {
		//Do not need to account for any additional (parallel) time with walltime.
		
	}
	

	
	
}
