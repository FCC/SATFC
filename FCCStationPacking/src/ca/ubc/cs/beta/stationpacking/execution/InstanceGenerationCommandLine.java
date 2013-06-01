package ca.ubc.cs.beta.stationpacking.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.options.UsageSection;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.options.ConfigToLaTeX;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.data.manager.DACStationManager;
import ca.ubc.cs.beta.stationpacking.execution.InstanceGenerationExecutor;
import ca.ubc.cs.beta.stationpacking.execution.parameters.InstanceGenerationParameters;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.IExperimentReporter;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.LocalExperimentReporter;
import ca.ubc.cs.beta.stationpacking.experiment.instance.IInstance;
import ca.ubc.cs.beta.stationpacking.experiment.instance.Instance;

import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SATResult;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SolverResult;

public class InstanceGenerationCommandLine {
		
	private static Logger log = LoggerFactory.getLogger(InstanceGenerationExecutor.class);

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
				"-EXPERIMENT_NAME",
				"TestExperiment",
				"-EXPERIMENT_DIR",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Results/TestExperiment",
				/*
				"-PACKING_CHANNELS",
				"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49",
				*/
				};
		
		args = aPaxosTargetArgs;
		
	
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
	
	    log.info("Creating experiment reporter...");
		IExperimentReporter aExperimentReporter = new LocalExperimentReporter(aExecParameters.getExperimentDir(), aExecParameters.getExperimentName());
			
		log.info("Creating instance generation and beginning experiment...");
		HashSet<Integer> aConsideredStationIDs = aExecParameters.getConsideredStationsIDs();
		HashSet<Integer> aCurrentStationIDs = aExecParameters.getStartingStationsIDs();
		List<Integer> aToConsiderStations = new ArrayList<Integer>();
		HashSet<Integer> aChannels = aExecParameters.getPackingChannels();
		
		HashSet<Station> aStartingStations = new HashSet<Station>();
		for(Station aStation : aStations){
			if(aCurrentStationIDs.contains(aStation.getID())){
				aStartingStations.add(aStation);
			}
			if(!aConsideredStationIDs.contains(aStation.getID())){
				aToConsiderStations.add(aStation.getID());
			}
		}
		IInstance aInstance = new Instance(aStartingStations,aChannels);
		Collections.shuffle(aToConsiderStations,new Random(aExecParameters.getSeed()));
		Iterator<Integer> aStationIterator = aToConsiderStations.iterator();
		while(aStationIterator.hasNext()) {
			Integer aID = aStationIterator.next();
			log.info("Trying to add {} to current set.",aID);
			aCurrentStationIDs.add(aID);
			try {
				log.info("Solving instance of size {}.",aCurrentStationIDs.size());
				//SolverResult aRunResult = SEND MESSAGE TO SOLVER with aCurrentStations, aChannels
				//log.info("Result: {}",aRunResult);
				//aExperimentReporter.report(aInstance, aRunResult);
				///if(!aRunResult.getResult().equals(SATResult.SAT)){
					//log.info("Instance was UNSAT, removing station.");
					//aCurrentStationIDs.remove(aID);
				//} else {
					//aInstance.addStation(new Station(aID,new HashSet<Integer>(),0));
				//}
			} catch (Exception e){ 
					e.printStackTrace();
			}
		} 
	}
}
