package ca.ubc.cs.beta.stationpacking.execution.deliverable;

import java.io.File;
import java.util.HashSet;
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
import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.data.manager.DACConstraintManager2;
import ca.ubc.cs.beta.stationpacking.data.manager.DACStationManager;
import ca.ubc.cs.beta.stationpacking.execution.deliverable.parameters.TAESolverParameterParser;
import ca.ubc.cs.beta.stationpacking.experiment.instance.IInstance;
import ca.ubc.cs.beta.stationpacking.experiment.instance.Instance;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder.CNFEncoder;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfresultlookup.HybridCNFResultLookup;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfresultlookup.ICNFResultLookup;
import ca.ubc.cs.beta.stationpacking.experiment.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.experiment.solver.TAESolver;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SolverResult;

public class TAESolverDeliverable {
	
	private static Logger log = LoggerFactory.getLogger(TAESolverDeliverable.class);

	public static void main(String[] args) throws Exception {
		
		/**
		 * Test arguments to use, instead of compiling and using command line.
		**/
		
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
				};
		
		args = aPaxosTargetArgs;
		
	
		/**
		 * NA - do we want to remove the TAE options from the arguments specified on the command line?
		**/
		//NA - I don't understand much of what follows immediately; also, don't currently use the cutoffTime
		//TAE Options
		Map<String,AbstractOptions> aAvailableTAEOptions = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
		
		//Parse the command line arguments in a parameter object.
		TAESolverParameterParser aExecParameters = new TAESolverParameterParser();
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
		log.info("Getting station information...");
		DACStationManager aStationManager = new DACStationManager(aExecParameters.getRepackingDataParameters().getStationFilename(),aExecParameters.getRepackingDataParameters().getDomainFilename());
	    
		log.info("Getting constraint information...");
		Set<Station> aStations = aStationManager.getStations();
		DACConstraintManager2 dCM = new DACConstraintManager2(aStations,aExecParameters.getRepackingDataParameters().getConstraintFilename());
	
		log.info("Creating solver components...");

		ICNFEncoder aCNFEncoder = new CNFEncoder();
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
			ISolver aSolver = new TAESolver(dCM, aCNFLookup, aCNFEncoder, aTAE, aTAEExecConfig,aExecParameters.getSeed());			
			while(true){ //There must be a way to do this that puts fewer demands on the processor
				//if(a message has arrived)
					//if(the message encodes an instance) 
						IInstance aInstance = decodeInstance(/*message*/);
						SolverResult aSolverResult = aSolver.solve(aInstance,1800/*we could pass CutoffTime to the solver in its constructor*/);
						//Send aSolverResult back
					//else if(the message tells us to quit)
						//quit
					//else
						//send "message not understood" reply
			}
		} 
		finally
		{
			//We need to tell the TAE we are shutting down, otherwise the program may not exit 
			if(aTAE != null){ aTAE.notifyShutdown();}
		}

	}
	
	//NA - Alex: fill in here!
	private static IInstance decodeInstance(/*message*/){
		return new Instance(new HashSet<Station>(),new HashSet<Integer>());
	}
}


