package ca.ubc.cs.beta.stationpacking.experiment;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import ca.ubc.cs.beta.stationpacking.datamanagers.DACStationManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.SATResult;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.execution.MainSolver;
import ca.ubc.cs.beta.stationpacking.execution.parameters.parsers.InstanceGenerationParameterParser;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.IExperimentReporter;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.LocalExperimentReporter;


public class InstanceGenerationCommandLine {
		
	private static Logger log = LoggerFactory.getLogger(InstanceGenerationCommandLine.class);

	public static void main(String[] args) throws Exception {
		
		/**
		 * Test arguments to use, instead of compiling and using command line.
		**/
		String[] aNArnostiArgs = {"-STATIONS_FILE",
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
				/*
				"--algoExec",
				"python solverwrapper.py",
				"--cutoffTime",
				"1800",
				"--execDir",
				"SATsolvers",
				"-SOLVER",
				"picosat",s
				*/
				"-SEED",
				"123"
				};
		
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
				/*
				"-PACKING_CHANNELS",
				"14,15,16",
				"--algoExec",
				"python solverwrapper.py",
				"--cutoffTime",
				"1800",
				"--execDir",
				"SATsolvers",
				"-SOLVER",
				"picosat",s
				*/
				"-SEED",
				"123",
				"-STARTING_STATIONS",
				"24914"
				};
		
		
		String[] aPaxosArgs = {"-STATIONS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/stations2.csv",
				"-DOMAINS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/NewDACData/Domain-041813.csv",
				"-CONSTRAINTS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/NewDACData/Interferences-041813.csv",
				"-EXPERIMENT_NAME",
				"TestExperiment",
				"-EXPERIMENT_DIR",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Results/TestExperiment"
				};
		
		args = aNArnostiRealArgs;
		
	
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
	
	    log.info("Creating solver...");
	    //NA - this is temporary to allow communication with solver
		MainSolver aTAE = new MainSolver(args);
	    
	    
	    log.info("Creating experiment reporter...");
		IExperimentReporter aExperimentReporter = new LocalExperimentReporter(aExecParameters.getExperimentDir(), aExecParameters.getExperimentName());
			
		HashSet<Integer> aConsideredStationIDs = aExecParameters.getConsideredStationsIDs();
		HashSet<Integer> aCurrentStationIDs = aExecParameters.getStartingStationsIDs();
		HashSet<Station> aToConsiderStations = new HashSet<Station>();
		HashSet<Integer> aChannels = aExecParameters.getPackingChannels();
		log.info("Packing channels are "+aChannels);
		
		log.info("Beginning experiment...");
		HashSet<Station> aStartingStations = new HashSet<Station>();
		for(Station aStation : aStations){
			if(aCurrentStationIDs.contains(aStation.getID())){
				aStartingStations.add(aStation);
			}
			if(!aConsideredStationIDs.contains(aStation.getID())){
				aToConsiderStations.add(aStation);
			}
		}
		Instance aInstance = new Instance(aStartingStations,aChannels);
		Iterator<Station> aStationIterator = new InversePopulationStationIterator(aToConsiderStations, aExecParameters.getSeed());
		while(aStationIterator.hasNext()) {
			Station aStation = aStationIterator.next();
			log.info("Trying to add {} to current set.",aStation);
			aCurrentStationIDs.add(aStation.getID());
			try {
				log.info("Solving instance of size {}.",aCurrentStationIDs.size());
				
				SolverResult aRunResult = aTAE.receiveMessage(aCurrentStationIDs,aChannels,1800.0);
				aExperimentReporter.report(aInstance, aRunResult);
				if(!aRunResult.getResult().equals(SATResult.SAT)){
					log.info("Instance was UNSAT, removing "+aStation);
					aCurrentStationIDs.remove(aStation.getID());
				} else {
					log.info("Instance was SAT, with assignment "+aRunResult.getAssignment());
					aInstance.addStation(aStationManager.get(aStation.getID()));
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
