package ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.SATSolver;


import ca.ubc.cs.beta.stationpacking.solver.SATResult;
import ca.ubc.cs.beta.stationpacking.solver.sat.Clause_old;


/* This interface should provide a unified way to interact with the SATSolver.
 * Hopefully, if/when we turn to other incremental SAT libraries, they can simply implement 
 * this interface, and none of the existing code will have to change.
 * 
 * Possible methods to add to the library:
 * -Something that returns the result of the last call to solve()
 * @authors: narnosti, gsauln
 */

public interface IIncrementalSATSolver {

	/* Tries to add the specified clause to the SAT problem, returns true upon success.
	 */
	public boolean addClause(Clause_old aClause);
	
	/* Asks whether the current problem is satisfiable in the context of the specified assumptions.
	 * aAssumptions.getVars() is the set of variables that should be assumed true
	 * aAssumptions.getNegatedVars() is the set of variables that should be assumed false
	 * aCutOff cut off time for the call.
	 */
	public SATResult solve(Clause_old aAssumptions, double aCutOff);

	/* Solve the current problem without assumptions; should behave identically to solve(new Clause());
	 * aCutOff cut off time for the call.
	 */
	public SATResult solve(double aCutOff);

	/* If the last call to solve() returned SAT, this function returns the corresponding assignment.
	 * If the last call to solve() did not return SAT, returns an empty Clause.
	 */
	public Clause_old getAssignment();

	/* Resets the internal state, so that the current problem contains no variables and no clauses.
	 */
	public void clear();
}
