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
package ca.ubc.cs.beta.stationpacking.facade;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Container for the result returned by a SATFC facade.
 * @author afrechet
 */
@Data
@Accessors(prefix="f")
public class SATFCResult
{
	private final ImmutableMap<Integer,Integer> fWitnessAssignment;
	private final SATResult fResult;
	private final double fRuntime;
	
	/**
	 * @param aResult - the satisfiability result.
	 * @param aRuntime - the time (s) it took to get to such result.
	 * @param aWitnessAssignment - the witness assignment
	 */
	public SATFCResult(SATResult aResult, double aRuntime, Map<Integer,Integer> aWitnessAssignment)
	{
		fResult = aResult;
		fRuntime = aRuntime;
		fWitnessAssignment = ImmutableMap.copyOf(aWitnessAssignment);
	}
	
	@Override
	public String toString()
	{
		return fRuntime+","+fResult+","+fWitnessAssignment.toString();
	}
}