package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorBuilder;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.IStationManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.execution.parameters.datamanagement.DataManagementParameters;
import ca.ubc.cs.beta.stationpacking.execution.parameters.validator.ImplementedSolverParameterValidator;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.execution.parameters.repackingdata.RepackingDataParameters;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.CNFEncoder;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.solver.cnfwriter.CNFStringWriter;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.TAESolver;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.cnflookup.ICNFResultLookup;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionOptions;

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
	public AlgorithmExecutionOptions AlgorithmExecutionOptions = new AlgorithmExecutionOptions();

	@Parameter(names = "-SOLVER", description = "SAT solver to use (from the implemented list of SAT solvers - can be circumvented by fully defining a valid TAE).", required=true, validateWith = ImplementedSolverParameterValidator.class)
	private String Solver;
	
	public ISolver getSolver()
	{
		AlgorithmExecutionOptions.paramFileDelegate.paramFile = AlgorithmExecutionOptions.algoExecDir+File.separatorChar+"sw_parameterspaces"+File.separatorChar+"sw_"+Solver+".txt";
		//Ugly parameter config extraction thanks to Steve's programming.
		TargetAlgorithmEvaluator aTAE = TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(AlgorithmExecutionOptions.taeOpts, AlgorithmExecutionOptions.getAlgorithmExecutionConfig(null), false, AvailableTAEOptions);
		
		return getTAESolver(aTAE);
	}

	
	private TAESolver getTAESolver(TargetAlgorithmEvaluator aTAE)
	{
		Logger log = LoggerFactory.getLogger(TAESolverParameters.class);
	
		
		IStationManager aStationManager = RepackingDataParameters.getDACStationManager();
	
		Set<Station> aStations = aStationManager.getStations();
		IConstraintManager aConstraintManager = RepackingDataParameters.getDACConstraintManager(aStations);
		
		log.info("Creating CNF encoder...");
		ICNFEncoder aCNFEncoder = new CNFEncoder(aStations);
		
		log.info("Creating CNF lookup...");
		ICNFResultLookup aCNFLookup = DataManagementParameters.getHybridCNFResultLookup();
		
		log.info("Creating constraint grouper...");
		IComponentGrouper aGrouper = new ConstraintGrouper();
		
		log.info("Creating solver...");
		TAESolver aSolver = new TAESolver(aConstraintManager, aCNFEncoder, aCNFLookup, aGrouper, new CNFStringWriter(), aTAE, AlgorithmExecutionOptions.getAlgorithmExecutionConfig(null));
		
		return aSolver;
	}
	

}
