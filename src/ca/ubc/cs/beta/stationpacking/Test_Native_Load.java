package ca.ubc.cs.beta.stationpacking;

import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries.GlueMiniSATLibrary;

import com.sun.jna.Native;

public class Test_Native_Load {

	/**
	 * Run this main class to check if you are able to load libraries using jna.
	 * @param args args[0] path to the library you are trying to load with jna.
	 */
	public static void main(String[] args)
	{
		String lib = args[0];
		Native.loadLibrary(lib, GlueMiniSATLibrary.class);
	}
	
}
