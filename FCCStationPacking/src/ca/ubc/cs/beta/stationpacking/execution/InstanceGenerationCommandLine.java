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
import ca.ubc.cs.beta.stationpacking.execution.deliverable.TAESolverDeliverable;
import ca.ubc.cs.beta.stationpacking.execution.parameters.InstanceGenerationParameterParser;
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
				"/Users/narnosti/Documents/FCCOutput/toy_stations.txt",
				"-DOMAINS_FILE",
				"/Users/narnosti/Documents/FCCOutput/toy_domains.txt",
				"-CONSTRAINTS_FILE",
				"/Users/narnosti/Documents/FCCOutput/toy_constraints.txt",
				"-EXPERIMENT_NAME",
				"TestExperiment",
				"-EXPERIMENT_DIR",
				"/Users/narnosti/Documents/fcc-station-packing/FCCStationPacking/ExperimentDir",
				"-PACKING_CHANNELS",
				"14,15,16",
				};
		
		args = aPaxosTargetArgs;
		
	
		/**
		 * 
		**/
		//TAE Options
		Map<String,AbstractOptions> aAvailableTAEOptions = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
		
		//Parse the command line arguments in a parameter object.
		InstanceGenerationParameterParser aExecParameters = new InstanceGenerationParameterParser();
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
				
				//NA - this is temporary to allow communication with solver
				TAESolverDeliverable aTAE = new TAESolverDeliverable(new String[02]);
				SolverResult aRunResult = aTAE.receiveMessage(aCurrentStationIDs,aChannels);
				aExperimentReporter.report(aInstance, aRunResult);
				if(!aRunResult.getResult().equals(SATResult.SAT)){
					log.info("Instance was UNSAT, removing station.");
					aCurrentStationIDs.remove(aID);
				} else {
					aInstance.addStation(aStationManager.get(aID));
				}
				
				//SolverResult aRunResult = SEND MESSAGE TO SOLVER with aCurrentStations, aChannels
				//log.info("Result: {}",aRunResult);
				//aExperimentReporter.report(aInstance, aRunResult);
				//if(!aRunResult.getResult().equals(SATResult.SAT)){
					//log.info("Instance was UNSAT, removing station.");
					//aCurrentStationIDs.remove(aID);
				//} else {
					//aInstance.addStation(aStationManager.get(aID));
				//}
			} catch (Exception e){ 
					e.printStackTrace();
			}
		} 
	}
}
