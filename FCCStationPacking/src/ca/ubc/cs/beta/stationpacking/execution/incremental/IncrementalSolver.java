package ca.ubc.cs.beta.stationpacking.execution.incremental;

import java.util.HashSet;
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
import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.data.manager.*;
import ca.ubc.cs.beta.stationpacking.execution.incremental.ClauseAdder.*;
import ca.ubc.cs.beta.stationpacking.execution.incremental.SATLibraries.GlueMiniSatLibrary;
import ca.ubc.cs.beta.stationpacking.experiment.instance.*;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SolverResult;

public class IncrementalSolver {
	
	IStationManager fStationManager;
	IClauseAdder fClauseAdder;
	private static Logger log = LoggerFactory.getLogger(IncrementalSolver.class);

	
	public IncrementalSolver(String[] args) throws Exception{
		String[] aPaxosTargetArgs = {"-STATIONS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/stations2.csv",
				"-DOMAINS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/NewDACData/Domain-041813.csv",
				"-CONSTRAINTS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/NewDACData/Interferences-041813.csv",
				"-SOLVER",
				"glueminisat",
				/*
				"--paramFile",
				"SATsolvers/sw_parameterspaces/sw_tunedclasp.txt",
				*/
				"--cutoffTime",
				"1800",
				"--numDummyVars",
				"1000"
				};
		
		args = aPaxosTargetArgs;
		
	
		//NA - I don't understand much of what follows immediately; also, don't currently use the cutoffTime
		//TAE Options
		Map<String,AbstractOptions> aAvailableTAEOptions = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
		//Parse the command line arguments in a parameter object.
		IncrementalSolverParameterParser aExecParameters = new IncrementalSolverParameterParser();
		JCommander aParameterParser = new JCommander(aExecParameters);
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
		fStationManager = new DACStationManager(aExecParameters.getRepackingDataParameters().getStationFilename(),aExecParameters.getRepackingDataParameters().getDomainFilename());

		log.info("Getting constraint information...");
		IConstraintManager aConstraintManager = new DACConstraintManager2(fStationManager.getStations(),aExecParameters.getRepackingDataParameters().getConstraintFilename());
	
				
		log.info("Creating solver...");		
		fClauseAdder = new DummyVarClauseAdder(aConstraintManager,new GlueMiniSatLibrary(),aExecParameters.getNumDummyVars());
		//fClauseAdder = new MemoryCopyingClauseAdder(new GlueMiniSatLibrary(),fConstraintManager)
		
		//while(true){}
		
	}

	//We'd want to be able to send a "quit" message in addition to new instances
	//Also catch errors if the message isn't in right form
	public SolverResult receiveMessage(Set<Integer> aStationIDs, Set<Integer> aChannels,Boolean priceCheck) throws Exception{
		Set<Station> aStations = new HashSet<Station>();
		Station aStation;
		for(Integer aID : aStationIDs){
			if((aStation=fStationManager.get(aID))!=null) aStations.add(aStation);
		}
		IInstance aInstance;
		if(aStations.size()>0&&aChannels.size()>0){
			aInstance = new Instance(aStations,aChannels);
		} else {
			throw new Exception("Invalid Instance: recognized station set is: "+aStations+", channels are: "+aChannels);
		}
		return fClauseAdder.updateAndSolve(aInstance, priceCheck);

	}
}
