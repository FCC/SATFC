package ca.ubc.cs.beta.stationpacking.execution.incremental;

import java.util.Set;

import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SATResult;

public interface IncrementalSATLibrary {

	public boolean addClause(Set<Integer> aVars,Set<Integer> aNegatedVars);
	public SATResult solve();
}
