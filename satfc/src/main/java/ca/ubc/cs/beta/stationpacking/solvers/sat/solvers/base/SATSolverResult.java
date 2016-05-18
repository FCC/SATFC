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
package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base;

import java.io.Serializable;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult.SolvedBy;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;
import lombok.Getter;

public class SATSolverResult implements Serializable {

	private final SATResult fResult;
	private final double fRuntime;
	private final ImmutableSet<Literal> fAssignment;
    @Getter
    private final SolverResult.SolvedBy solvedBy;
	@Getter
	private final String nickname;

	public SATSolverResult(SATResult aResult, double aRuntime, Set<Literal> aAssignment, SolverResult.SolvedBy solvedBy) {
		this(aResult, aRuntime, aAssignment, solvedBy, null);
	}

    public SATSolverResult(SATResult aResult, double aRuntime, Set<Literal> aAssignment, SolverResult.SolvedBy solvedBy, String nickname)
	{
        Preconditions.checkArgument(aRuntime >= 0, "Cannot create a " + getClass().getSimpleName() + " with negative runtime: " + aRuntime);
		this.nickname = nickname;
		fResult = aResult;
		fRuntime = aRuntime;
		fAssignment = ImmutableSet.copyOf(aAssignment);
        this.solvedBy = aResult.isConclusive() ? solvedBy : SolvedBy.UNSOLVED;
	}
	
	public SATResult getResult(){
		return fResult;
	}
	
	public double getRuntime()
	{
		return fRuntime;
	}
	
	public ImmutableSet<Literal> getAssignment() {
		return fAssignment;
	}
	
	@Override
	public String toString()
	{
		return fResult+","+fRuntime+","+fAssignment;
	}

    public static SATSolverResult timeout(double time) {
        return new SATSolverResult(SATResult.TIMEOUT, time, ImmutableSet.of(), SolverResult.SolvedBy.UNSOLVED);
    }

}
