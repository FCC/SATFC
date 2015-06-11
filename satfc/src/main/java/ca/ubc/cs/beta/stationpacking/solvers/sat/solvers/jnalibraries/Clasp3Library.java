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
package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
* Created by newmanne on 15/04/15.
*/
public interface Clasp3Library extends Library {

    /**
     * Begin solving a problem by calling this method. The reference returned here is used in all subsequent methods.
     * This should be called for every problem you want to solve - don't re-use the pointer returned across problems!
     * @param params The arguments to clasp
     * @return A pointer to a JNAProblem
     */
    Pointer initConfig(final String params);

    /**
     * Pass a problem to the solver. Does not start solving the problem
     * @param jnaProblemPointer A pointer from having previously called {@link #initConfig(String)} method
     * @param problemString CNF of the problem to solve
     */
    void initProblem(Pointer jnaProblemPointer, final String problemString);

    /**
     * Actually solve the problem. Must have previously called {@link #initProblem(com.sun.jna.Pointer, String)} or this has undefined behavior
     * @param jnaProblemPointer A pointer from having previously called {@link #initConfig(String)} method
     * @param timeoutTime How long to try solving the problem for before returning a timeout
     */
    void solveProblem(Pointer jnaProblemPointer, double timeoutTime);

    /**
     * Destroy the problem and all associated information. Should always be called to clean up.
     * @param jnaProblemPointer A pointer from having previously called {@link #initConfig(String)} method
     */
    void destroyProblem(Pointer jnaProblemPointer);

    /**
     * Interrupt the problem currently being solved. Should be called when another thread is blocking on {@link #solveProblem(com.sun.jna.Pointer, double)}
     * @param jnaProblemPointer A pointer from having previously called {@link #initConfig(String)} method
     * @return false is the interrupt was successful
     */
    boolean interrupt(Pointer jnaProblemPointer);

    /**
     * @param jnaProblemPointer A pointer from having previously called {@link #initConfig(String)} method
     * @return 0 if UNSAT, 1 if SAT, 2 if TIMEOUT, 3 if INTERRUPTED, 4 otherwise
     */
    int getResultState(Pointer jnaProblemPointer);

    /**
     * @param jnaProblemPointer A pointer from having previously called {@link #initConfig(String)} method
     * @return 0 if unconfigured, 1 if configured, 2 if error
     */
    int getConfigState(Pointer jnaProblemPointer);

    /**
     * Only call this if the problem is SAT
     * @param jnaProblemPointer A pointer from having previously called {@link #initConfig(String)} method
     * @return The assignment returned for the problem. The first element is the size.
     */
    IntByReference getResultAssignment(Pointer jnaProblemPointer);

    /**
     * Only call this method if configState == 2, in which case there was an error in configuration.
     * @param jnaProblemPointer A pointer from having previously called {@link #initConfig(String)} method
     * @return The configuration error message
     */
    String getConfigErrorMessage(Pointer jnaProblemPointer);
}