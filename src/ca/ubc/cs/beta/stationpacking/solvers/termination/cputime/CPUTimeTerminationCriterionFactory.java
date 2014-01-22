package ca.ubc.cs.beta.stationpacking.solvers.termination.cputime;

import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterionFactory;

public class CPUTimeTerminationCriterionFactory implements ITerminationCriterionFactory{

	private final double fCPUTimeLimit;
	
	public CPUTimeTerminationCriterionFactory(double aCPUTimeLimit)
	{
		fCPUTimeLimit = aCPUTimeLimit;
	}
	
	@Override
	public CPUTimeTerminationCriterion getTerminationCriterion() {
		return new CPUTimeTerminationCriterion(fCPUTimeLimit);
	}

}
