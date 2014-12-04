/**
 * Copyright 2014, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
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

public interface ClaspLibrary extends Library 
{
	/**
	 * Creates a new configuration object using the given parameters.
	 * @param params parameters with which to create the configuration. Must be the same as 
	 * the ones given to the clasp executable, but must not contain any from the "Basic Options"
	 * @param params_strlen length of the params string.
	 * @param maxArgs maximum number of arguments contained in parameters (i.e. parameters cannot 
	 * have more arguments than maxArgs, arguments are separated by spaces)
	 * @return a new configuration object using the given parameters.
	 */
	Pointer createConfig(String params, int params_strlen, int maxArgs);
	
	/**
	 * Frees the memory used by the configuration object.
	 * @param config configuration object to be destroyed.
	 */
	void destroyConfig(Pointer config);
	
	/**
	 * Returns the status of the configuration object.  (1 = valid configuration, 2 = error ) 
	 * @param config configuration object to get the status for.
	 * @return the status of the configuration object.
	 */
	int getConfigStatus(Pointer config);
	
	/**
	 * Return the configuration error message if it exists.  Not empty if getConfigStatus() == 2.
	 * @param config the configuration object to get the error for.
	 * @return the configuration error message if it exists.
	 */
	String getConfigErrorMessage(Pointer config);
	
	/**
	 * Return the clasp configuration error message if it exists. Not empty if getConfigStatus() == 2.
	 * @param config the configuration object to get the error for.
	 * @return the configuration error message if it exists.
	 */
	String getConfigClaspErrorMessage(Pointer config);

	/**
	 * Create a new problem object using the string in dimacs format.  Variables must be from 1 to n.
	 * @param problem string representing the problem in dimacs format.
	 * @return a new problem object defined by the string.
	 */
	Pointer createProblem(String problem);
	
	/**
	 * Frees the memory used by the problem object.
	 * @param problem problem object to destroy.
	 */
	void destroyProblem(Pointer problem);
	
	/**
	 * Returns the status of the problem instance once solved as been called.  Exists in clasp, but 
	 * I don't know what it means...
	 * @param problem problem to get the status from.
	 * @return the status of the problem instance once solved as been called.
	 */
	int getProblemStatus(Pointer problem);

	/**
	 * Creates a new result object that will contain the results once solve is called.
	 * @return a new result object.
	 */
	Pointer createResult();
	
	/**
	 * Frees the memory used by the result object.
	 * @param result destroy the result object.
	 */
	void destroyResult(Pointer result);
	
	/**
	 * Returns the state of the result: 0 = UNSAT, 1 = SAT, 2 = UNKNOWN.
	 * @param result result object to return the status of the assignment from.
	 * @return the state of the result.
	 */
	int getResultState(Pointer result);
	
	/**
	 * Returns a warning if the configuration used to solve the problem is unsafe/unreasonable w.r.t the current problem.
	 * @param result result object to get the warnings from.
	 * @return a warning if the configuration used to solve the problem is unsafe/unreasonable w.r.t the current problem.
	 */
	String getResultWarning(Pointer result);
	
	/**
	 * Returns a pointer containing the assignment of the literals if the problem is SAT. 
	 * The first value corresponds to the size of the array.
	 * @param result result object to return the assignment for.
	 * @return a pointer containing the assignment of the literals if the problem is SAT.
	 */
	IntByReference getResultAssignment(Pointer result);

	/**
	 * Resets the result object to the default state: UNKNOWN with no warnings and assignments.
	 * @param result result object to reset.
	 */
	void resetResult(Pointer result);
	
	/**
	 * Create a facade that will handle the interrupt calls.
	 * @return a facade that will handle the interrupt calls.
	 */
	Pointer createFacade();
	
	/**
	 * Frees the memory used by the facade.
	 * @param facade destroy the facade object.
	 */
    void destroyFacade(Pointer facade);
    
    /**
     * Tries to terminate the call to solve.
     * @param facade facade used to solve.
     * @return true if the call to solve was interrupted.
     */
    boolean interrupt(Pointer facade);
    
	/**
	 * Solves the problem using the given configuration and stores the results in the given results object.
	 * @param facade facade object used to call solve and interrupt.
	 * @param problem problem to solve.
	 * @param config configuration to use.
	 * @param result result object used to store results.
	 */
	void jnasolve(Pointer facade, Pointer problem, Pointer config, Pointer result);	
}
