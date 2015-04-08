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
package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental;

import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

import com.sun.jna.Pointer;

public class IncrementalClaspProblem {

	private final ITerminationCriterion fTerminationCriterion;
	private final Pointer fProblemPointer;
	private final long fSeed;
	
	public IncrementalClaspProblem(Pointer problemPointer, ITerminationCriterion terminationCriterion, long seed )
	{
		fTerminationCriterion = terminationCriterion;
		fProblemPointer = problemPointer;
		fSeed = seed;
	}
	
	public ITerminationCriterion getTerminationCriterion() {
		return fTerminationCriterion;
	}

	public Pointer getProblemPointer() {
		return fProblemPointer;
	}
	
	public long getSeed()
	{
		return fSeed;
	}

}
