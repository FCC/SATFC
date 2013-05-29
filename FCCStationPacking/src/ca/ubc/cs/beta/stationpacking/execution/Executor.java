package ca.ubc.cs.beta.stationpacking.execution;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.data.manager.DACConstraintManager2;
import ca.ubc.cs.beta.stationpacking.data.manager.DACStationManager;
import ca.ubc.cs.beta.stationpacking.execution.executionparameters.ExperimentParameters;
import ca.ubc.cs.beta.stationpacking.experiment.*;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.IExperimentReporter;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.LocalExperimentReporter;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder.NickCNFEncoder;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnflookup.ICNFResultLookup;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnflookup.HybridCNFResultLookup;
import ca.ubc.cs.beta.stationpacking.experiment.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.experiment.solver.TAESolver;

public class Executor {
	
	private static Logger log = LoggerFactory.getLogger(Executor.class);

	public static void main(String[] args) throws Exception {
		
		/**
		 * Test arguments to use, instead of compiling and using command line.
		 * 
		 * 
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
				"-EXPERIMENT_NAME",
				"TestExperiment",
				"-EXPERIMENT_DIR",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Results/TestExperiment",
				"-TAE_CONC_EXEC_NUM",
				"1"
				//"-REPORT_FILE",
				//"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Results/TestExperiment/TestExperiment.csv"
				
				//"-PACKING_CHANNELS",
				//"14,15,16"
				};
		
		args = aPaxosTargetArgs;
		
		
		/**
		 * 
		**/
		
		//Parse the command line arguments in a parameter object.
		ExperimentParameters aExecParameters = new ExperimentParameters();
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
		DACStationManager aStationManager = new DACStationManager(aExecParameters.getStationFilename(),aExecParameters.getDomainFilename());
	    Set<Station> aStations = aStationManager.getStations();
		DACConstraintManager2 dCM = new DACConstraintManager2(aStations,aExecParameters.getConstraintFilename());
	
		ICNFEncoder aCNFEncoder = new NickCNFEncoder();
		
		log.info("Creating cnf lookup...");
		ICNFResultLookup aCNFLookup = new HybridCNFResultLookup(aExecParameters.getCNFDirectory(), aExecParameters.getCNFOutputName());
				
		log.info("Creating solver...");
		ISolver aSolver = new TAESolver(dCM, aCNFLookup, aCNFEncoder, aExecParameters.getTAEParamConfigSpace(), aExecParameters.getTAEExecutable(), aExecParameters.getTAEExecDirectory(), aExecParameters.getTAEType(),aExecParameters.getTAEMaxConcurrentExec());
		
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
		aInstanceGeneration.run(aStartingStations, aStationIterator,aExecParameters.getPackingChannels(),aExecParameters.getSolverCutoff());	
		aCNFLookup.writeToFile();
	}
}
