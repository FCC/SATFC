package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental;

import com.sun.jna.Pointer;

public class IncrementalClaspProblem {

	private final double fCutoffTime;
	private final Pointer fProblemPointer;
	private final long fSeed;
	
	public IncrementalClaspProblem(Pointer problemPointer, double cutoffTime, long seed )
	{
		fCutoffTime = cutoffTime;
		fProblemPointer = problemPointer;
		fSeed = seed;
	}
	
	public double getCutoffTime() {
		return fCutoffTime;
	}

	public Pointer getProblemPointer() {
		return fProblemPointer;
	}
	
	public long getSeed()
	{
		return fSeed;
	}

}
