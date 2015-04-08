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
package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

public interface IncrementalClaspLibrary extends ClaspLibrary
{
	/**
	 * Shall implement the read function used by the clasp library callback in order to get the next step in the incremental solving.
	 */
	interface jnaIncRead extends Callback
	{
		Pointer read();
	}

	/**
	 * This function is a callback method used by clasp in order to know if it should stop the solve incremental loop.
	 */
	interface jnaIncContinue extends Callback
	{
		boolean doContinue();
	}
	
	/**
	 * Creates a new problem object that will use the given callback function in order to set the incremental steps of the problems.
	 * @param callback callback function used to set the incremental step during solving.
	 * @return a new problem object that will use the given callback function in order to set the incremental steps of the problems.
	 */
	Pointer createIncProblem(jnaIncRead callback);
	
	/**
	 * Frees the memory used by the problem object.
	 * @param problem problem object to be destroyed.
	 */
	void destroyIncProblem(Pointer problem);
	
	/**
	 * Creates a new incremental control object that will be using the callback function and setting the results correctly when UNSAT.
	 * @param callback callback method to use in order to control the incremental solving.
	 * @param result result object for which to set the state correctly.
	 * @return a new incremental control object that will be using the callback function and setting the results correctly when UNSAT. 
	 */
	Pointer createIncControl(jnaIncContinue callback, Pointer result);
	
	/**
	 * Frees the memory used by the control object.
	 * @param control control object to be destroyed.
	 */
	void destroyIncControl(Pointer control);
	
	/**
	 * Start solving incrementally.  Should be using in a thread as it is a blocking call until incControl returns false.
	 * @param facade object used to call solve and interrupt. 
	 * @param incProblem incremental problem to solve using the callback method to get new input.
	 * @param config configuration to use.
	 * @param incControl control object used to stop the incremental solving loop.
	 * @param result result object used to store results.
	 */
	void jnasolveIncremental(Pointer facade, Pointer incProblem, Pointer config, Pointer incControl, Pointer result);

}
