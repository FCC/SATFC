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

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * This is the Java half of the JNA bridge used to run any legal configuration of UBCSAT/SATenstein from SATFC.
 *
 * ***WARNING***: Due to conventions involving the use of global variables in UBCSAT, it is NOT
 *  safe to instantiate multiple configurations in succession that use different algorithms of UBCSAT
 *  (i.e. they pass different values to the "-alg" command-line parameter).
 *
 *  If one wants to run multiple different algorithms, these should be instantiated separately
 *  via multiple calls to {@link com.sun.jna.Native#loadLibrary}, passing the
 *  {@link ca.ubc.cs.beta.stationpacking.utils.NativeUtils#NATIVE_OPTIONS} argument each time.
 *
 *  Example: <code>Native.loadLibrary(pathToUBCSATLibrary, UBCSATLibrary.class, NativeUtils.NATIVE_OPTIONS);</code>
 *
 * @author pcernek on 7/27/15.
 */
public interface UBCSATLibrary extends Library {

     /**
     * First step in solving a problem. The reference returned here is used in all subsequent methods.
     * This should be called for every problem you want to solve - don't re-use the pointer returned across problems!
     * @param params The arguments to UBCSAT
     * @return A pointer to a JNAProblem
     */
    Pointer initConfig(final String params);

    /**
     Pass a problem to the solver. Does not start solving the problem.
     * @param jnaProblemPointer A pointer from having previously called {@link #initConfig(String)} method
     * @param problemString CNF of the problem to solve
     * @return True if returned error free
     */
    boolean initProblem(Pointer jnaProblemPointer, final String problemString);

    /**
     * Set the values of the variables for a given problem.
     * Must be called after {@link #initProblem(Pointer, String)}
     * @param jnaProblemPointer A pointer from having previously called {@link #initConfig(String)} method
     * @param initialAssignment reference to an array of longs, where the magnitude of each number corresponds
     *                          to a variable id, and the sign corresponds to the assignment for that variable.
     *                          Positive vars get initialized to True, negative vars to False.
     *                          Note that this can be a partial initial assignment, so any variable not mentioned
     *                          in this initial assignment gets randomly initiated by default. (Random initial
     *                          assignment happens in {@link #initProblem(Pointer, String)}
     * @return True if returned error free
     */
    boolean initAssignment(Pointer jnaProblemPointer, long[] initialAssignment, int numberOfVars);

    /**
     * Actually solve the problem. Must have previously called {@link #initProblem(com.sun.jna.Pointer, String)} or this has undefined behavior
     * @param jnaProblemPointer A pointer from having previously called {@link #initConfig(String)} method
     * @param timeoutTime How long to try solving the problem for before returning a timeout (in seconds)
     * @return True if returned error free
     */
    boolean solveProblem(Pointer jnaProblemPointer, double timeoutTime);

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
    void interrupt(Pointer jnaProblemPointer);

    /**
     * @param jnaProblemPointer A pointer from having previously called {@link #initConfig(String)} method
     * @return 1 if SAT, 2 if TIMEOUT, 3 if INTERRUPTED, 4 otherwise
     */
    int getResultState(Pointer jnaProblemPointer);

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
    String getErrorMessage(Pointer jnaProblemPointer);

}
