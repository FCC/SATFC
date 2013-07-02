package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;

/**
 * Parameter class to merge different types of solvers (TAE and Incremental).
 * @author afrechet
 * TODO for future usage.
 */
@UsageTextField(title="FCC Station Packing Packing Solver Options",description="Parameters defining a feasibility checker SAT solver.")
public class SolverParameters extends AbstractOptions{


	public ISolver getSolver()
	{
		return null;
	}
	
	

}
