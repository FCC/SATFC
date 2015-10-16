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
package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental;

import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;

import com.google.common.base.Preconditions;

/**
 * Clasp library result.
 * @author gsauln
 */
public class ClaspResult {

	private final SATResult fSATResult;
	private final int[] fAssignment;
	private final double fRuntime;
	
	public ClaspResult(SATResult satResult, int[] assignment, double runtime)
	{
        Preconditions.checkState(runtime >= 0, "Runtime must be >= 0 (was " + runtime + " )");
		fSATResult = satResult;
		fAssignment = assignment;
		fRuntime = runtime;
	}
	
	public SATResult getSATResult() {
		return fSATResult;
	}
	public int[] getAssignment() {
		return fAssignment;
	}
	public double getRuntime() {
		return fRuntime;
	}
	
	
	
}
