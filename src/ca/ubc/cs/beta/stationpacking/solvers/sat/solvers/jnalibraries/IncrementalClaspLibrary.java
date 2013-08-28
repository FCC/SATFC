package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries;

import com.sun.jna.Callback;

public interface IncrementalClaspLibrary extends ClaspLibrary
{
	
	interface jnaIncRead extends Callback
	{
		String read();
	}

	/**
	 * used to test the library by calling a dummy C function.
	 */
	void test(jnaIncRead fn);
}
