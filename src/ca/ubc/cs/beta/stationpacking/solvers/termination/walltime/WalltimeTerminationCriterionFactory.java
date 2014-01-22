package ca.ubc.cs.beta.stationpacking.solvers.termination.walltime;

import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterionFactory;

public class WalltimeTerminationCriterionFactory implements ITerminationCriterionFactory{

	private final double fWalltimeLimit;
	
	public WalltimeTerminationCriterionFactory(double aWalltimeLimit)
	{
		fWalltimeLimit = aWalltimeLimit;
	}
	
	@Override
	public WalltimeTerminationCriterion getTerminationCriterion() {
		return new WalltimeTerminationCriterion(fWalltimeLimit);
	}



}
