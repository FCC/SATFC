package ca.ubc.cs.beta.stationpacking.execution;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.data.*;
import ca.ubc.cs.beta.stationpacking.data.manager.DACConstraintManager2;
import ca.ubc.cs.beta.stationpacking.data.manager.DACStationManager;
import ca.ubc.cs.beta.stationpacking.data.manager.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.data.manager.IStationManager;

import ca.ubc.cs.beta.stationpacking.experiment.NInstanceGeneration;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.IExperimentReporter;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.LocalExperimentReporter;

import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder.NickCNFEncoder;

import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnflookup.ICNFLookup;

import ca.ubc.cs.beta.stationpacking.experiment.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.experiment.solver.NTAESolver;

import ca.ubc.cs.beta.stationpacking.experiment.*;

public class NickMain {

	//NA - TODO set random seed to get reproducible results.
	private static Logger log = LoggerFactory.getLogger(NickMain.class);
	private static final long fSeed = 12345;
	
	public static void main(String[] args) throws Exception {
		//String dacDomainsFile = "/Users/narnosti/Documents/fcc-station-packing/Output/toy_domains.txt";
		//String dacConstraintFile = "/Users/narnosti/Documents/fcc-station-packing/Output/toy_constraints.txt";
		//String pairwiseConstraintsFile = "/Users/narnosti/Documents/fcc-station-packing/Output/Alex_pairwise_constraints.txt";

		/*
		String newDomainsFile = "/Users/narnosti/Documents/fcc-station-packing/Output/DAC_domains.txt";
		String newConstraintsFile = "/Users/narnosti/Documents/fcc-station-packing/Output/DAC_constraints.txt";
        */
		String StationsFile = "/Users/narnosti/Documents/FCCOutput/stations.csv";
		String DomainsFile = "/Users/narnosti/Dropbox/Alex/2013 04 New Data/Domain 041813.csv";
		String ConstraintsFile = "/Users/narnosti/Dropbox/Alex/2013 04 New Data/Interferences 041813.csv";
		
		IStationManager aSM = new DACStationManager(StationsFile,DomainsFile);
		IConstraintManager aCM = new DACConstraintManager2(aSM.getStations(),ConstraintsFile);
		
		/*
		
		Set<Integer> aChannels = new HashSet<Integer>();
		for(int i = 14; i <=30; i++) aChannels.add(i);
		
		log.info("Getting data...");
		String aStationFileName = "/Users/narnosti/Documents/fcc-station-packing/Output/stations.csv";
		String dacDomainsFile = "/Users/narnosti/Documents/fcc-station-packing/Input/Domains.csv";
		DACStationManager aStationManager = new DACStationManager(aStationFileName,dacDomainsFile);
        Set<Station> aStations = aStationManager.getStations();
		String dacConstraintFile = "/Users/narnosti/Documents/fcc-station-packing/Input/Interferences-read-note-please.csv";
		DACConstraintManager2 dCM = new DACConstraintManager2(aStations,dacConstraintFile);
		

		ICNFEncoder aCNFEncoder = new NickCNFEncoder();
		
		log.info("Creating cnf lookup...");
 
		String aCNFDir = "/Users/narnosti/Documents/fcc-station-packing/Output/CNFs";
		ICNFLookup aCNFLookup = new NDirCNFLookup(aCNFDir);
				
		log.info("Creating solver...");
        String aParamConfigurationSpaceLocation = "/Users/narnosti/Documents/fcc-station-packing/FCCStationPacking/SATsolvers/sw_parameterspaces/sw_picosat.txt";
		String aAlgorithmExecutable = "python solverwrapper.py";
        String aExecDir = "/Users/narnosti/Documents/fcc-station-packing/FCCStationPacking/SATsolvers/";
		int aMaximumConcurrentExecution = 4;
		ISolver aSolver = new NTAESolver(dCM, aCNFLookup, aCNFEncoder, aParamConfigurationSpaceLocation, aAlgorithmExecutable, aExecDir, "CLI",aMaximumConcurrentExecution);
		
		log.info("Creating experiment reporter...");
        String testFolder = "/Users/narnosti/Documents/fcc-station-packing/FCCStationPacking/ExperimentDir";
		IExperimentReporter aExperimentReporter = new LocalExperimentReporter(testFolder, "test");
		
		Iterator<Station> aStationIterator = new NInversePopulationStationIterator(aStationManager.getStations(), fSeed);
		
		log.info("Creating instance generation and beginning experiment...");
		Set<Station> aStartingStations = new HashSet<Station>();
		double aCutoff = 1800;
		NInstanceGeneration aInstanceGeneration = new NInstanceGeneration(aSolver, aExperimentReporter);
		aInstanceGeneration.run(aStartingStations, aStationIterator,aChannels,aCutoff);	
		aCNFLookup.writeToFile();

		*/
	}
}
