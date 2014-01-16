package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental;

import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

import com.sun.jna.Pointer;

public class IncrementalClaspProblem {

	private final ITerminationCriterion fTerminationCriterion;
	private final Pointer fProblemPointer;
	private final long fSeed;
	
	public IncrementalClaspProblem(Pointer problemPointer, ITerminationCriterion terminationCriterion, long seed )
	{
		fTerminationCriterion = terminationCriterion;
		fProblemPointer = problemPointer;
		fSeed = seed;
	}
	
	public ITerminationCriterion getTerminationCriterion() {
		return fTerminationCriterion;
	}

	public Pointer getProblemPointer() {
		return fProblemPointer;
	}
	
	public long getSeed()
	{
		return fSeed;
	}

}
