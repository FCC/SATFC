package ca.ubc.cs.beta.stationpacking.execution.parameters;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.options.AlgorithmExecutionOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorBuilder;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.IStationManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.execution.parameters.validator.ImplementedSolverParameterValidator;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.CNFEncoder2;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.ICNFEncoder2;
import ca.ubc.cs.beta.stationpacking.solver.cnfwriter.CNFStringWriter;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.TAESolver;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.cnflookup.ICNFResultLookup;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.componentgrouper.IComponentGrouper;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

@UsageTextField(title="FCC Station Packing Packing ACLib's TAE Solver Options",description="Parameters defining a TAE based feasibility checker.")
public class TAESolverParameters extends AbstractOptions {
	
	/*DON'T MAKE THIS A PARAMETER.*/
	public final Map<String,AbstractOptions> AvailableTAEOptions = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
	
	//(Global) Data parameters
	@ParametersDelegate
	public RepackingDataParameters RepackingDataParameters = new RepackingDataParameters();
	
	//Data management parameters
	@ParametersDelegate
	public DataManagementParameters DataManagementParameters = new DataManagementParameters();
	
	@ParametersDelegate
	private AlgorithmExecutionOptions AlgorithmExecutionOptions = new AlgorithmExecutionOptions();
	public AlgorithmExecutionOptions getAlgorithmExecutionOptions() {
		return AlgorithmExecutionOptions;
	}

	@Parameter(names = "-SOLVER", description = "SAT solver to use (from the implemented list of SAT solvers - can be circumvented by fully defining a valid TAE).", required=true, validateWith = ImplementedSolverParameterValidator.class)
	private String Solver;
	
	public ISolver getTAESolver() throws Exception
	{
		Logger log = LoggerFactory.getLogger(TAESolverParameters.class);
		
		AlgorithmExecutionOptions.paramFileDelegate.paramFile = AlgorithmExecutionOptions.algoExecDir+File.separatorChar+"sw_parameterspaces"+File.separatorChar+"sw_"+Solver+".txt";
		
		TargetAlgorithmEvaluator aTAE = TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(AlgorithmExecutionOptions.taeOpts, AlgorithmExecutionOptions.getAlgorithmExecutionConfig(), false, AvailableTAEOptions);
		
		IStationManager aStationManager = RepackingDataParameters.getDACStationManager();
		Set<Station> aStations = aStationManager.getStations();
		IConstraintManager aConstraintManager = RepackingDataParameters.getDACConstraintManager(aStations);
		
		log.info("Creating CNF encoder...");
		ICNFEncoder2 aCNFEncoder = new CNFEncoder2(aStations);
		
		log.info("Creating CNF lookup...");
		ICNFResultLookup aCNFLookup = DataManagementParameters.getHybridCNFResultLookuo();
		
		log.info("Creating constraint grouper...");
		IComponentGrouper aGrouper = new ConstraintGrouper();
		
		log.info("Creating solver...");
		ISolver aSolver = new TAESolver(aConstraintManager, aCNFEncoder, aCNFLookup, aGrouper, new CNFStringWriter(), aTAE, AlgorithmExecutionOptions.getAlgorithmExecutionConfig());
		
		return aSolver;
	}
}
