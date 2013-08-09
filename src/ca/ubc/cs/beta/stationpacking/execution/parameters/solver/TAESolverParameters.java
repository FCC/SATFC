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
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.TAEBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.cnflookup.HybridCNFResultLookup;
import ca.ubc.cs.beta.stationpacking.solvers.cnflookup.ICNFResultLookup;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATEncoder;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

@UsageTextField(title="FCC Station Packing Packing ACLib's TAE Solver Options",description="Parameters defining a TAE based feasibility checker.")
public class TAESolverParameters extends AbstractOptions implements ISolverParameters{
	
	/*DON'T MAKE THIS A PARAMETER.*/
	public final Map<String,AbstractOptions> AvailableTAEOptions = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
	
	@ParametersDelegate
	public AlgorithmExecutionOptions AlgorithmExecutionOptions = new AlgorithmExecutionOptions();

	@Parameter(names = "-SOLVER", description = "SAT solver to use (from the implemented list of SAT solvers - can be circumvented by fully defining a valid TAE).", required=true, validateWith = ImplementedSolverParameterValidator.class)
	public String Solver;
	
	@Parameter(names = "-CNF_DIR", description = "Directory location where to write CNFs. Will be created if inexistant.",required=true)
	public String CNFDirectory;
	
	@Parameter(names = "-KEEP_CNF", description = "True if and only if the CNF files must be preserved.")
	public boolean KeepCNF = false;
	
	@Parameter(names = "-CNFLOOKUP_OUTPUT_FILE", description = "Name of the file to store CNF results (the file is saved in the CNF directory).")
	public String CNFOutputName = "ComponentInstanceResultsArchive.csv";
	
	@Override
	public ISolver getSolver(IStationManager aStationManager, IConstraintManager aConstraintManager)
	{
		Logger log = LoggerFactory.getLogger(TAESolverParameters.class);
		
		AlgorithmExecutionOptions.paramFileDelegate.paramFile = AlgorithmExecutionOptions.algoExecDir+File.separatorChar+"sw_parameterspaces"+File.separatorChar+"sw_"+Solver+".txt";
		//Ugly parameter config extraction thanks to Steve's programming.
		TargetAlgorithmEvaluator aTAE = TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(AlgorithmExecutionOptions.taeOpts, AlgorithmExecutionOptions.getAlgorithmExecutionConfig(null), false, AvailableTAEOptions);
		
		log.info("Creating CNF encoder...");
		ISATEncoder aCNFEncoder = new SATEncoder(aStationManager, aConstraintManager);
		
		log.info("Creating CNF lookup...");
		ICNFResultLookup aCNFLookup = new HybridCNFResultLookup(CNFDirectory, CNFOutputName);
		
		log.info("Creating constraint grouper...");
		IComponentGrouper aGrouper = new ConstraintGrouper();
		
		log.info("Creating solver...");
		TAEBasedSolver aSolver = new TAEBasedSolver(aConstraintManager, aCNFEncoder, aCNFLookup, aGrouper, aTAE, AlgorithmExecutionOptions.getAlgorithmExecutionConfig(null),KeepCNF);
		
		return aSolver;
	}

	

}
