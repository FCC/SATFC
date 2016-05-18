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
package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries;

import com.sun.jna.Pointer;

/**
 * Created by pcernek on 7/29/15.
 */
public interface UBCSATTestHelperLibrary extends UBCSATLibrary {

    /**
     * Gets the number of variables in the current SAT problem according to UBCSAT.
     *  Must be called after {@link #initProblem(Pointer, String)}.
     * @return the number of variables in the current SAT problem
     */
    int getNumVars();

    /**
     * Gets the number of clauses in the current SAT problem according to UBCSAT.
     *  Must be called after {@link #initProblem(Pointer, String)}.
     * @return the number of clauses in the current SAT problem
     */
    int getNumClauses();

    /**
     * Get the current assignment (True or False) of the given variable.
     * @param varNumber the number of the variable for which to get the assignment.
     * @return the given variable's assignment.
     */
    boolean getVarAssignment(int varNumber);

    /**
     * Runs the triggers associated with the InitData event point in UBCSAT.
     * Must be run after initProblem.
     */
    void runInitData();
}
