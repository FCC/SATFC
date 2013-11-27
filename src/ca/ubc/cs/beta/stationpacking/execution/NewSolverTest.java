package ca.ubc.cs.beta.stationpacking.execution;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionOptions;
import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.options.UsageSection;
import ca.ubc.cs.beta.aclib.options.ConfigToLaTeX;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorBuilder;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.instancegeneration.InstanceGenerationParameters;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.experiment.InstanceGeneration;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.NoGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.sat.SATBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ISATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental.IncrementalClaspSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.ClaspSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.TAESATSolver;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class NewSolverTest {

	private static Logger log = LoggerFactory.getLogger(NewSolverTest.class);
	
	/**
	 * @param args
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) {

		//Parse the command line arguments in a parameter object.
		InstanceGenerationParameters aInstanceGenerationParameters = new InstanceGenerationParameters();
		JCommander aParameterParser = JCommanderHelper.getJCommander(aInstanceGenerationParameters, TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators());
		try
		{
			aParameterParser.parse(args);
		}
		catch (ParameterException aParameterException)
		{
			List<UsageSection> sections = ConfigToLaTeX.getParameters(aInstanceGenerationParameters, TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators());
			
			boolean showHiddenParameters = false;
			
			//A much nicer usage screen than JCommander's 
			ConfigToLaTeX.usage(sections, showHiddenParameters);
			
			log.error(aParameterException.getMessage());
			return;
		}
		
		InstanceGeneration aInstanceGeneration = null;
		try
		{
			
			IStationManager aStationManager = aInstanceGenerationParameters.RepackingDataParameters.getDACStationManager();
			IConstraintManager aConstraintManager = aInstanceGenerationParameters.RepackingDataParameters.getDACConstraintManager(aStationManager);
			
			
			String aCNFDir = aInstanceGenerationParameters.SolverParameters.TAESolverParameters.CNFDirectory;
			String aSolver = aInstanceGenerationParameters.SolverParameters.TAESolverParameters.Solver;
			AlgorithmExecutionOptions aAlgorithmExecutionOptions = aInstanceGenerationParameters.SolverParameters.TAESolverParameters.AlgorithmExecutionOptions;
			
			aAlgorithmExecutionOptions.paramFileDelegate.paramFile = aAlgorithmExecutionOptions.algoExecDir+File.separatorChar+"sw_parameterspaces"+File.separatorChar+"sw_"+aSolver+".txt";
			
			/*
			 * SET YOUR SAT SOLVER RIGHT HERE MY FRIEND.
			 * 
			 * I used some of the TAE options from the parameter object to set up a TAE based SAT solver, you should set up your "library" based SAT solver.
			 */
			
			ISATSolver aTAESATSolver = new TAESATSolver(TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(aAlgorithmExecutionOptions.taeOpts, aAlgorithmExecutionOptions.getAlgorithmExecutionConfig(null), false, aInstanceGenerationParameters.SolverParameters.TAESolverParameters.AvailableTAEOptions),
					aAlgorithmExecutionOptions.getAlgorithmExecutionConfig(null).getParamFile().getDefaultConfiguration(),
					aCNFDir);
			
			String aAlexLibPath = "/ubc/cs/project/arrow/afrechet/git/FCCStationPacking/SATsolvers/clasp/jna/libjnaclasp.so";
			String aSantaLibPath = "/home/gsauln/workspace/FCC-Station-Packing/SATsolvers/clasp/jna/libjnaclasp.so";
			
			String usePath = aSantaLibPath;
			
			AbstractCompressedSATSolver aClaspSATSolver = new ClaspSATSolver(usePath, ClaspLibSATSolverParameters.ORIGINAL_CONFIG_03_13);
			
			SATEncoder aEncoder = new SATEncoder(aStationManager, aConstraintManager);
			
			AbstractSATSolver incClaspSATSolver = new IncrementalClaspSATSolver(usePath, ClaspLibSATSolverParameters.ORIGINAL_CONFIG_03_13, 1);
			
			/*
			 * 
			 */
			
			ISolver aSATBasedSolver = new SATBasedSolver(incClaspSATSolver, aEncoder , aConstraintManager, new NoGrouper());
			
		
			aInstanceGeneration = new InstanceGeneration(aSATBasedSolver,aInstanceGenerationParameters.getExperimentReporter());
			
			aInstanceGeneration.run(aInstanceGenerationParameters.getStartingStations(), aInstanceGenerationParameters.getStationIterator(), aInstanceGenerationParameters.getPackingChannels(), aInstanceGenerationParameters.Cutoff, aInstanceGenerationParameters.Seed);

		}
		finally{
			if(aInstanceGeneration!=null)
			{
				aInstanceGeneration.getSolver().notifyShutdown();
			}
		}
		
		
		

	}

}
