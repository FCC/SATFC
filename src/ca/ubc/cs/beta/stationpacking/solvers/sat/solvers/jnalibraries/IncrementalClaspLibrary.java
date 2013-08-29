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
		String read();
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
	
	Pointer createIncControl(jnaIncContinue callback);
	
	void destroyIncControl(Pointer control);
	
	void jnasolveIncremental(Pointer facade, Pointer incProblem, Pointer config, Pointer incControl, Pointer result);

	
	/**
	 * Used to test the library by calling a dummy C function.
	 */
	void test(jnaIncRead fn, jnaIncContinue fn1);
}
