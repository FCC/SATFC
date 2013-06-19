package ca.ubc.cs.beta.stationpacking.experiment;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.options.UsageSection;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.options.ConfigToLaTeX;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorBuilder;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.datamanagers.DACConstraintManager2;
import ca.ubc.cs.beta.stationpacking.datamanagers.DACStationManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.execution.parameters.InstanceGenerationParameters;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.IExperimentReporter;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.LocalExperimentReporter;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.CNFEncoder2;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.ICNFEncoder2;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.CNFStringWriter;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.TAESolver;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.cnflookup.HybridCNFResultLookup;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.cnflookup.ICNFResultLookup;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.componentgrouper.IComponentGrouper;


public class InstanceGenerationExecutor {
	
	private static Logger log = LoggerFactory.getLogger(InstanceGenerationExecutor.class);

	public static void main(String[] args) throws Exception {
		
		/**
		 * Test arguments to use, instead of compiling and using command line.
		 * 
		 * 
		**/
		
		/*
		 * Deprecated arguments.
		 * 
		String[] aPaxosTargetArgs_old = {"-STATIONS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/stations2.csv",
				"-DOMAINS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/NewDACData/Domain-041813.csv",
				"-CONSTRAINTS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/NewDACData/Interferences-041813.csv",
				"-CNF_DIR",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/CNFs",
				"-SOLVER",
				"tunedclasp",
				"-EXPERIMENT_NAME",
				"TestExperiment",
				"-EXPERIMENT_DIR",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Results/TestExperiment",
				"-TAE_CONC_EXEC_NUM",
				"1"
				};
		*/
		
		String[] aNArnostiRealArgs = {"-STATIONS_FILE",
				"/Users/narnosti/Documents/FCCOutput/stations.csv",
				"-DOMAINS_FILE",
				"/Users/narnosti/Dropbox/Alex/2013 04 New Data/Domain 041813.csv",
				"-CONSTRAINTS_FILE",
				"/Users/narnosti/Dropbox/Alex/2013 04 New Data/Interferences 041813.csv",
				"-EXPERIMENT_NAME",
				"TestExperiment",
				"-EXPERIMENT_DIR",
				"/Users/narnosti/Documents/fcc-station-packing/FCCStationPacking/ExperimentDir",
				"-CNF_DIR",
				"/Users/narnosti/Documents/FCCOutput/CNFs",
				/*
				"-PACKING_CHANNELS",
				"14,15,16",
				*/
				"--algoExec",
				"python solverwrapper.py",
				"--cutoffTime",
				"1800",
				"--execDir",
				"SATsolvers",
				"-SOLVER",
				"picosat",
				/*
				"-SEED",
				"123",
				"-STARTING_STATIONS",
				"24914"
				*/
				};
		
		String[] aPaxosTargetArgs = {"-STATIONS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/stations2.csv",
				"-DOMAINS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/NewDACData/Domain-041813.csv",
				"-CONSTRAINTS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/NewDACData/Interferences-041813.csv",
				"-CNF_DIR",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/CNFs",
				"-SOLVER",
				"tunedclasp",
				"-EXPERIMENT_NAME",
				"TestExperiment",
				"-EXPERIMENT_DIR",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Results/TestExperiment",
				/*
				"-PACKING_CHANNELS",
				"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49",
				*/
				"--execDir",
				"SATsolvers",
				/*
				"--paramFile",
				"SATsolvers/sw_parameterspaces/sw_tunedclasp.txt",
				*/
				"--algoExec",
				"python solverwrapper.py",
				"--cutoffTime",
				"1800"
				/*
				"--logAllCallStrings",
				"true",
				"--logAllProcessOutput",
				"true"
				*/
				};
		
		args = aNArnostiRealArgs;
		
	
		/**
		 * 
		**/
		//TAE Options
		Map<String,AbstractOptions> aAvailableTAEOptions = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
		
		//Parse the command line arguments in a parameter object.
		InstanceGenerationParameters aExecParameters = new InstanceGenerationParameters();
		JCommander aParameterParser = JCommanderHelper.getJCommander(aExecParameters, aAvailableTAEOptions);
		try
		{
			aParameterParser.parse(args);
		}
		catch (ParameterException aParameterException)
		{
			List<UsageSection> sections = ConfigToLaTeX.getParameters(aExecParameters, aAvailableTAEOptions);
			
			boolean showHiddenParameters = false;
			
			//A much nicer usage screen than JCommander's 
			ConfigToLaTeX.usage(sections, showHiddenParameters);
			
			log.error(aParameterException.getMessage());
			return;
		}
		
		//Use the parameters to instantiate the experiment.
		log.info("Getting data...");
		DACStationManager aStationManager = new DACStationManager(aExecParameters.getRepackingDataParameters().getStationFilename(),aExecParameters.getRepackingDataParameters().getDomainFilename());
	    Set<Station> aStations = aStationManager.getStations();
		DACConstraintManager2 dCM = new DACConstraintManager2(aStations,aExecParameters.getRepackingDataParameters().getConstraintFilename());
	
		log.info("Creating constraint grouper...");
		IComponentGrouper aGrouper = new ConstraintGrouper();
		
		log.info("Creating CNF encoder...");
		ICNFEncoder2 aCNFEncoder = new CNFEncoder2();
		
		log.info("Creating CNF lookup...");
		ICNFResultLookup aCNFLookup = new HybridCNFResultLookup(aExecParameters.getCNFDirectory(), aExecParameters.getCNFOutputName());
		
		log.info("Creating solver...");
		//Logs the available target algorithm evaluators
		for(String name : aAvailableTAEOptions.keySet())
		{
			log.info("Target Algorithm Evaluator Available: {} ", name);
		}
		//Fix config space file based on solver
		aExecParameters.getAlgorithmExecutionOptions().paramFileDelegate.paramFile = aExecParameters.getAlgorithmExecutionOptions().algoExecDir+File.separatorChar+"sw_parameterspaces"+File.separatorChar+"sw_"+aExecParameters.getSolver()+".txt";
		
		AlgorithmExecutionConfig aTAEExecConfig = aExecParameters.getAlgorithmExecutionOptions().getAlgorithmExecutionConfig();
		TargetAlgorithmEvaluator aTAE = null;
		try {
			
			aTAE = TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(aExecParameters.getAlgorithmExecutionOptions().taeOpts, aTAEExecConfig, false, aAvailableTAEOptions);
			ISolver aSolver = new TAESolver(dCM, aCNFEncoder, aCNFLookup, aGrouper, new CNFStringWriter(), aTAE, aTAEExecConfig,aExecParameters.getSeed());
			
			log.info("Creating experiment reporter...");
			IExperimentReporter aExperimentReporter = new LocalExperimentReporter(aExecParameters.getExperimentDir(), aExecParameters.getExperimentName());
			
			log.info("Creating instance generation and beginning experiment...");
			HashSet<Integer> aConsideredStationIDs = aExecParameters.getConsideredStationsIDs();
			HashSet<Integer> aStartingStationsIDs = aExecParameters.getStartingStationsIDs();
			
			HashSet<Station> aStartingStations = new HashSet<Station>();
			HashSet<Station> aToConsiderStations = new HashSet<Station>();
			for(Station aStation : aStations)
			{
				if(aStartingStationsIDs.contains(aStation.getID()))
				{
					aStartingStations.add(aStation);
				}
				if(!aConsideredStationIDs.contains(aStation.getID()))
				{
					aToConsiderStations.add(aStation);
				}
			}
			
			Iterator<Station> aStationIterator = new InversePopulationStationIterator(aToConsiderStations, aExecParameters.getSeed());
			InstanceGeneration aInstanceGeneration = new InstanceGeneration(aSolver, aExperimentReporter);
			aInstanceGeneration.run(aStartingStations, aStationIterator,aExecParameters.getPackingChannels(),aTAEExecConfig.getAlgorithmCutoffTime());	
			aCNFLookup.writeToFile();
			
		} 
		finally
		{
			//We need to tell the TAE we are shutting down
			//Otherwise the program may not exit 
			if(aTAE != null)
			{
				aTAE.notifyShutdown();
			}
		}
	}
}
