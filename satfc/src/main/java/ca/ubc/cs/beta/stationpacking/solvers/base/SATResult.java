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
package ca.ubc.cs.beta.stationpacking.solvers.base;

import java.io.Serializable;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;

/**
 * Enum for the result type of a SAT solver on a SAT instance.
 * @author afrechet
 */
public enum SATResult implements Serializable{
	
    /**
     * The problem is satisfiable. 
     */
    SAT,
    /**
     * The problem is unsatisfiable. 
     */
    UNSAT,
    /**
     * A solution to the problem could not be found in the allocated time.
     */
    TIMEOUT,
    /**
     * Run crashed while solving problem.
     */
    CRASHED,
    /**
     * Run was killed while solving problem.
     */
    KILLED,
    /**
     * Run was interrupted while solving problem.
     */
    INTERRUPTED;

    public boolean isConclusive() {
        return this == SAT || this == UNSAT;
    }
	
	/**
	 * @param aRunResult - a runresult.
	 * @return the given RunStatus converted to a SATResult.
	 */
	public static SATResult fromRunResult(RunStatus aRunResult)
	{	
		return SATResult.valueOf(aRunResult.toString());
	}
}


