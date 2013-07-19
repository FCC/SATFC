package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

public class IncrementalSolverParameters extends AbstractOptions {


    //ExecutableSolver parameters
    @ParametersDelegate
    public ExecutableSolverParameters ExecutableSolverParameters = new ExecutableSolverParameters();

    @Parameter(names = "-LIBRARY", description = "Path to incremental SAT library.")
	private String fLibraryPath;
	public String getIncrementalLibraryLocation(){
		//Insert check here? I want it to be required IF we're using an incremental solver.
		return fLibraryPath;
	}
	public boolean useIncrementalSolver(){
		//return getSolver().equals("glueminisat-incremental");
		return false;
	}
}
