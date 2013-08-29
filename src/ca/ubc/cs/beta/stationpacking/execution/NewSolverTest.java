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
import ca.ubc.cs.beta.stationpacking.experiment.InstanceGeneration;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.SATBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.NoGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ClaspSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ISATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.IncrementalClaspSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.TAESATSolver;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class NewSolverTest {

	private static Logger log = LoggerFactory.getLogger(NewSolverTest.class);
	
	/**
	 * @param args
	 */
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
			//ISATSolver aClaspSATSolver = new ClaspSATSolver(aSantaLibPath, 
			//		 "--eq=0 --trans-ext=all --sat-prepro=0 --sign-def=2 --del-max=10000 --strengthen=local,1 --del-init-r=800,20000 --loops=no --init-watches=0 --heuristic=Berkmin --del-cfl=F,100 --restarts=L,256 --del-algo=basic,0 --deletion=3,66,3.0 --berk-max=256 --del-grow=1.0,100.0,F,128 --update-act --del-glue=4,0 --update-lbd=2 --reverse-arcs=3 --otfs=0 --berk-huang --del-on-restart=50 --contraction=120 --counter-restarts=3 --local-restarts --lookahead=no --save-progress=10 --counter-bump=180");
			ISATSolver incClaspSATSolver = new IncrementalClaspSATSolver(aSantaLibPath,
					"--eq=0 --trans-ext=all --sat-prepro=0 --sign-def=2 --del-max=10000 --strengthen=local,1 --del-init-r=800,20000 --loops=no --init-watches=0 --heuristic=Berkmin --del-cfl=F,100 --restarts=L,256 --del-algo=basic,0 --deletion=3,66,3.0 --berk-max=256 --del-grow=1.0,100.0,F,128 --update-act --del-glue=4,0 --update-lbd=2 --reverse-arcs=3 --otfs=0 --berk-huang --del-on-restart=50 --contraction=120 --counter-restarts=3 --local-restarts --lookahead=no --save-progress=10 --counter-bump=180",
				1);
			
			/*
			 * 
			 */
			
			ISolver aSATBasedSolver = new SATBasedSolver(incClaspSATSolver, new SATEncoder(aStationManager, aConstraintManager), aConstraintManager, new NoGrouper());
			
		
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
