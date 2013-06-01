package ca.ubc.cs.beta.stationpacking.execution.incremental.SATLibraries;

import java.util.Set;

import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SATResult;

public interface IIncrementalSATLibrary {

	public boolean addClause(Set<Integer> aVars,Set<Integer> aNegatedVars);
	//Parameters provide assumptions
	public SATResult solve(Set<Integer> aTrueVars,Set<Integer> aFalseVars);
	public SATResult solve(); //should behave identically to solve(new HashSet<Integer>(),new HashSet<Integer>())

}

