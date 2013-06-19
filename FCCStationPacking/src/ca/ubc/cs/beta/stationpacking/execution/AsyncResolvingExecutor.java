package ca.ubc.cs.beta.stationpacking.execution;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorBuilder;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;

import ca.ubc.cs.beta.stationpacking.datamanagers.DACConstraintManager2;
import ca.ubc.cs.beta.stationpacking.datamanagers.DACStationManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.execution.parameters.InstanceGenerationParameters;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.AsynchronousLocalExperimentReporter;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.CNFEncoder2;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.ICNFEncoder2;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.AsyncTAESolver;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.CNFStringWriter;


public class AsyncResolvingExecutor {

	private static Logger log = LoggerFactory.getLogger(AsyncResolvingExecutor.class);
	
	public static void main(String[] args) throws Exception {
			
		/**
		 * Test arguments to use, instead of compiling and using command line.
		 * 
		 * 
		**/
		
		/*
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
				"ResolvingTestExperiment",
				"-EXPERIMENT_DIR",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Results/TestExperiment",
				"-TAE_CONC_EXEC_NUM",
				"1",
				"-REPORT_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Results/TestExperiment/TestExperiment.csv"
				//"-PACKING_CHANNELS",
				//"14,15,16"
				};
		*/
		
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
				"ResolvingTestExperiment",
				"-EXPERIMENT_DIR",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Results/TestExperiment",
				"-REPORT_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Results/TestExperiment/TestExperiment.csv",
				"--execDir",
				"SATsolvers",
				"--algoExec",
				"python solverwrapper.py",
				"--cutoffTime",
				"1800"
				};
		
		args = aPaxosTargetArgs;
		
	
		/**
		 * 
		**/
		//Available TAE Options
		Map<String,AbstractOptions> aAvailableTAEOptions = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
		
		//Parse the command line arguments in a parameter object.
		InstanceGenerationParameters aExecParameters = new InstanceGenerationParameters();
		JCommander aParameterParser = new JCommander(aExecParameters);
		try
		{
			aParameterParser.parse(args);
		}
		catch (ParameterException aParameterException)
		{
			System.out.println(aParameterException.getMessage());
			aParameterParser.usage();
		}
		
		//Use the parameters to instantiate the experiment.
		log.info("Getting data...");
		DACStationManager aStationManager = new DACStationManager(aExecParameters.getRepackingDataParameters().getStationFilename(),aExecParameters.getRepackingDataParameters().getDomainFilename());
	    Set<Station> aStations = aStationManager.getStations();
		DACConstraintManager2 dCM = new DACConstraintManager2(aStations,aExecParameters.getRepackingDataParameters().getConstraintFilename());
		ICNFEncoder2 aCNFEncoder = new CNFEncoder2(aStations);
				
		
		
		log.info("Creating experiment reporter...");
		AsynchronousLocalExperimentReporter aAsynchronousReporter = new AsynchronousLocalExperimentReporter(aExecParameters.getExperimentDir(), aExecParameters.getExperimentName());
		aAsynchronousReporter.startWritingReport();
		
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
			
			AsyncTAESolver aAsyncSolver = new AsyncTAESolver(dCM, aCNFEncoder, new CNFStringWriter(),aExecParameters.getCNFDirectory(), aTAE, aTAEExecConfig, aExecParameters.getSeed());
			
			//Get all instances, and solve each instance.
			log.info("Getting all instances from report file...");
			ArrayList<Pair<HashSet<Integer>,HashSet<Integer>>> aInstanceIDs = aExecParameters.getReportParser().getInstanceIDs();
			for(Pair<HashSet<Integer>,HashSet<Integer>> aInstanceID : aInstanceIDs)
			{
				HashSet<Integer> aInstanceChannels = aInstanceID.getKey();
				HashSet<Integer> aInstanceStationsIDs = aInstanceID.getValue();
				HashSet<Station> aInstanceStations = new HashSet<Station>();
				for(Station aStation : aStations)
				{
					if(aInstanceStationsIDs.contains(aStation.getID()))
					{
						aInstanceStations.add(aStation);
					}
				}
				Instance aInstance = new Instance(aInstanceStations, aInstanceChannels);
				log.info("Solving "+aInstance.toString());
				aAsyncSolver.solve(aInstance, aTAEExecConfig.getAlgorithmCutoffTime(), aAsynchronousReporter);
				
			}
			
			//Wait for completion and die.
			log.info("Waiting for completion of the runs...");
			aAsyncSolver.waitForFinish();
			
			//Kill report writing process
			log.info("Done! Shutting down EVERYTHING!");
			aAsynchronousReporter.stopWritingReport();
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
