package ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.SATLibraries;

import java.util.Map;

import ca.ubc.cs.beta.stationpacking.datastructures.Clause;
import ca.ubc.cs.beta.stationpacking.datastructures.SATResult;


public interface IIncrementalSATLibrary {

	public boolean addClause(Clause aClause);
	//Parameters provide assumptions
	public SATResult solve(Clause aAssumptions);
	public SATResult solve(); //should behave identically to solve(new HashSet<Integer>(),new HashSet<Integer>())
	public Clause getAssignment();
	public Map<Integer,Integer> getMap();
	public void clear();
}

