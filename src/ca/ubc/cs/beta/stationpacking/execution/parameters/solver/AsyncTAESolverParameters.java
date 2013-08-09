package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionOptions;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorBuilder;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.validator.ImplementedSolverParameterValidator;
import ca.ubc.cs.beta.stationpacking.solvers.AsyncTAESolver;
import ca.ubc.cs.beta.stationpacking.solvers.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.cnfencoder.SATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.cnflookup.AsyncCachedCNFLookup;
import ca.ubc.cs.beta.stationpacking.solvers.cnflookup.ICNFResultLookup;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

@UsageTextField(title="FCC Station Packing Packing ACLib's TAE Solver Options",description="Parameters defining a TAE based feasibility checker.")
public class AsyncTAESolverParameters extends AbstractOptions{
	
	/*DON'T MAKE THIS A PARAMETER.*/
	public final Map<String,AbstractOptions> AvailableTAEOptions = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
	
	@ParametersDelegate
	public AlgorithmExecutionOptions AlgorithmExecutionOptions = new AlgorithmExecutionOptions();
	
	@Parameter(names = "-SOLVER", description = "SAT solver to use (from the implemented list of SAT solvers - can be circumvented by fully defining a valid TAE).", required=true, validateWith = ImplementedSolverParameterValidator.class)
	private String Solver;
	
	@Parameter(names = "-CNF_DIR", description = "Directory location where to write CNFs. Will be created if inexistant.",required = true)
	public String CNFDirectory;
	
	public AsyncTAESolver getSolver(IStationManager aStationManager, IConstraintManager aConstraintManager)
	{
		Logger log = LoggerFactory.getLogger(AsyncTAESolverParameters.class);
		
		AlgorithmExecutionOptions.paramFileDelegate.paramFile = AlgorithmExecutionOptions.algoExecDir+File.separatorChar+"sw_parameterspaces"+File.separatorChar+"sw_"+Solver+".txt";
		
		log.info("Creating CNF encoder...");
		ISATEncoder aCNFEncoder = new SATEncoder(aStationManager,aConstraintManager);
		
		log.info("Creating CNF lookup...");
		ICNFResultLookup aCNFLookup = new AsyncCachedCNFLookup(CNFDirectory);
		
		log.info("Creating constraint grouper...");
		IComponentGrouper aGrouper = new ConstraintGrouper();
		
		TargetAlgorithmEvaluator aTAE = TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(AlgorithmExecutionOptions.taeOpts, AlgorithmExecutionOptions.getAlgorithmExecutionConfig(null), false, AvailableTAEOptions);
		
		log.info("Creating solver...");
		AsyncTAESolver aSolver = new AsyncTAESolver(aConstraintManager, aCNFEncoder, aCNFLookup, aGrouper, aTAE, AlgorithmExecutionOptions.getAlgorithmExecutionConfig(null));
		
		return aSolver;
	}
	

}
