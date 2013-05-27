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
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnflookup.ICNFLookup;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnflookup.ResultWritingCNFLookup;
import ca.ubc.cs.beta.stationpacking.experiment.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.experiment.solver.NTAESolver;

public class Executor {
	
	private static Logger log = LoggerFactory.getLogger(Executor.class);

	public static void main(String[] args) throws Exception {
		
		/**
		 * Test arguments to used instead of compiling and using command line.
		 * 
		 * 
		**/
		
		String[] Alextestargs = {"-STATIONS_FILE",
				"/Users/MightyByte/Documents/data/FCCStationPackingData/ProblemData/stations.csv",
				"-DOMAINS_FILE",
				"/Users/MightyByte/Documents/data/FCCStationPackingData/ProblemData/Domains.csv",
				"-CONSTRAINTS_FILE",
				"/Users/MightyByte/Documents/data/FCCStationPackingData/ProblemData/Interferences.csv",
				"-CNF_DIR",
				"/Users/MightyByte/Documents/data/FCCStationPackingData/CNFs",
				"-SOLVER",
				"tunedclasp",
				"-EXPERIMENT_NAME",
				"TestExperiment",
				"-EXPERIMENT_DIR",
				"/Users/MightyByte/Documents/data/FCCStationPackingData/TestExperiment"
				};
		args = Alextestargs;
		
		
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
		ICNFLookup aCNFLookup = new ResultWritingCNFLookup(aExecParameters.getCNFDirectory(), aExecParameters.getCNFOutputName());
				
		log.info("Creating solver...");
		ISolver aSolver = new NTAESolver(dCM, aCNFLookup, aCNFEncoder, aExecParameters.getTAEParamConfigSpace(), aExecParameters.getTAEExecutable(), aExecParameters.getTAEExecDirectory(), aExecParameters.getTAEType(),aExecParameters.getTAEMaxConcurrentExec());
		
		log.info("Creating experiment reporter...");
		IExperimentReporter aExperimentReporter = new LocalExperimentReporter(aExecParameters.getExperimentDir(), aExecParameters.getExperimentName());
		
		Iterator<Station> aStationIterator = new NInversePopulationStationIterator(aStationManager.getStations(), aExecParameters.getSeed());
		
		log.info("Creating instance generation and beginning experiment...");
		HashSet<Integer> aStartingStationsIDs = aExecParameters.getStartingStationsIDs();
		HashSet<Station> aStartingStations = new HashSet<Station>();
		for(Station aStation : aStations)
		{
			if(aStartingStationsIDs.contains(aStation.getID()))
			{
				aStartingStations.add(aStation);
			}
		}
		
		NInstanceGeneration aInstanceGeneration = new NInstanceGeneration(aSolver, aExperimentReporter);
		aInstanceGeneration.run(aStartingStations, aStationIterator,aExecParameters.getPackingChannels(),aExecParameters.getSolverCutoff());	
		aCNFLookup.writeToFile();
	}
}
