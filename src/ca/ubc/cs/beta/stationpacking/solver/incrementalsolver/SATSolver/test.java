package ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.SATSolver;

import ca.ubc.cs.beta.stationpacking.solver.jnalibraries.GlueMiniSATLibrary;

import com.sun.jna.Native;

public class test {

	public static void main(String[] args)
	{
		String lib = args[2];			
		System.out.println("Loading: "+lib);

		Native.loadLibrary(lib, GlueMiniSATLibrary.class);
		System.out.println("Done.");
	}
	
}
