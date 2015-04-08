/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
 *
 * This file is part of satfc.
 *
 * satfc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * satfc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with satfc.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
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
