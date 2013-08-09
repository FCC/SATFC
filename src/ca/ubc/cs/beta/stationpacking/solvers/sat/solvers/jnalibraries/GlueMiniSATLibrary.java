package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface GlueMiniSATLibrary extends Library
{
	/**
	 * Returns a pointer to a new GlueMiniSAT solver.
	 * @return a pointer to a new GlueMiniSAT solver.
	 */
	public Pointer createSolver();
	
	/**
	 * Destroys the solver (frees the memory).
	 * @param solver solver to destroy (free the memory of).
	 */
	public void destroySolver(Pointer solver);	
	
	/**
	 * Returns a pointer to a vector of literals.
	 * @return a pointer to a vector of literals.
	 */
  	public Pointer createVecLit();
  	
  	/**
  	 * Destroys the vector of literals (free the memory).
  	 * @param vecLiterals vector of literals to destroy.
  	 */
  	public void destroyVecLit(Pointer vecLiterals);
  	
  	/**
  	 * Adds the literal (num, state) to the vector vec.
  	 * @param vec vector to add the literal to.
  	 * @param num id of the literal to add.
  	 * @param state true=positive, false=negative.
  	 */
  	public void addLitToVec(Pointer vec, int num, boolean state);
  	
  	/**
  	 * Creates a new variable that can be used in the solver.
  	 * @param solver solver in which to create the new variable.
  	 * @return the value of the new variable.
  	 */
   	public int newVar(Pointer solver);

   	/**
   	 * Returns the number of variables contained in the solver.
   	 * @param solver solver for which to return the number of variables contained.
   	 * @return the number of variables contained in the solver.
   	 */
   	public int nVars(Pointer solver);

   	/**
   	 * Adds the clause associated with the vector of literals
   	 * @param solver solver in which to add the clause.
   	 * @param vecLiterals vector of literals forming the clause.
   	 * @return true if the clause was successfully added to the solver.
   	 */
  	public boolean addClause(Pointer solver, Pointer vecLiterals);
   	
  	/**
  	 * Solve the CNF contained in the solver according to the assumptions.
  	 * @param solver solver for which to solve the CNF.
  	 * @param vecAssumptions assumptions on the values contained in the CNF.
  	 * @return true if the current CNF is SAT according to the assumptions, false otherwise.
  	 */
  	public boolean solveWithAssumptions(Pointer solver, Pointer vecAssumptions);
  	
  	/**
  	 * Solve the CNF contained in the solver according to the single assumption.
  	 * @param solver solver for which to solve the CNF.
  	 * @param variable variable on which the assumption is held.
  	 * @param state (true = positive literal, false = negative literal) is assumed to be true.
  	 * @return true if the current CNF is SAT according to the single assumption, false otherwise.
  	 */
    int solveWithOneAssumption(Pointer solver, int variable, boolean state);
  	
  	/**
  	 * Returns true if the CNF contained in the solver is SAT, false otherwise.
  	 * @param solver solver for which to solve the CNF.
  	 * @return true if the CNF contained in the solver is SAT, false otherwise.
  	 */
   	public boolean solve(Pointer solver);
   	
   	/**
   	 * Return the value of the variable var in the solution of the CNF.  Is meaningful 
   	 * only is the last call to solve returned true (i.e. SAT). 
   	 * @param solver solver for which to return the value of the variable var.
   	 * @param var var for which to return the value.
   	 * @return the value of the variable var in the solution of the CNF.
   	 */
   	public boolean value(Pointer solver, int var);
   	
   	/**
   	 * Returns false if the solver is in a conflicting state (not ok), true otherwise (ok).
   	 * @param solver solver to check the status for.
   	 * @return false if the solver is in a conflicting state (not ok), true otherwise (ok).
   	 */
   	public boolean okay(Pointer solver);

   	/**
   	 * Set the interrupt flag to true in the GlueMiniSAT solver.  It will then exit at the next check.
   	 * @param solver solver to set the flag for.
   	 */
   	public void interrupt(Pointer solver);
   	
   	/**
   	 * Clears the interrupt flag (set to false) of the GlueMiniSAT solver.
   	 * @param solver solver to clear the interrupt flag for.
   	 */
   	public void clearInterrupt(Pointer solver);
   	
   	/**
   	 * Return the interrupt flag state.
   	 * @param solver solver to return the interrupt flag state for.
   	 */
   	public boolean getInterruptState(Pointer solver);
	
}
