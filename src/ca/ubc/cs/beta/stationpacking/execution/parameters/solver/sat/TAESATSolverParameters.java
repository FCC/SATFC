package ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat;

import java.io.File;
import java.util.Map;


import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionOptions;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorBuilder;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.execution.parameters.validator.ImplementedSolverParameterValidator;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.TAESATSolver;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

@UsageTextField(title="FCC Station Packing Packing ACLib's TAE SAT Solver Options",description="Parameters defining a TAE based SAT solver.")
public class TAESATSolverParameters extends AbstractOptions implements ISATSolverParameters{
	
	/*DON'T MAKE THIS A PARAMETER.*/
	public final Map<String,AbstractOptions> AvailableTAEOptions = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
	
	@ParametersDelegate
	public AlgorithmExecutionOptions AlgorithmExecutionOptions = new AlgorithmExecutionOptions();

	@Parameter(names = "--solver", description = "SAT solver to use (from the implemented list of SAT solvers - can be circumvented by fully defining a valid TAE).", required=true, validateWith = ImplementedSolverParameterValidator.class)
	public String Solver;
	
	@Parameter(names = "--cnf_dir", description = "Directory location where to write CNFs. Will be created if inexistant.",required=true)
	public String CNFDirectory;

	@Override
	public AbstractCompressedSATSolver getSATSolver() {
		
		AlgorithmExecutionOptions.paramFileDelegate.paramFile = AlgorithmExecutionOptions.algoExecDir+File.separatorChar+"sw_parameterspaces"+File.separatorChar+"sw_"+Solver+".txt";
		
		return new TAESATSolver(
				TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(AlgorithmExecutionOptions.taeOpts,
						false,
						AvailableTAEOptions),
				AlgorithmExecutionOptions.getAlgorithmExecutionConfig(null).getParamFile().getDefaultConfiguration(),
				AlgorithmExecutionOptions.getAlgorithmExecutionConfig(null),
				CNFDirectory);
	}

	

}
