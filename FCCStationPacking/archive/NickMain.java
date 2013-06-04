package ca.ubc.cs.beta.stationpacking.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.data.*;
import ca.ubc.cs.beta.stationpacking.data.manager.DACConstraintManager2;
import ca.ubc.cs.beta.stationpacking.data.manager.DACStationManager;
import ca.ubc.cs.beta.stationpacking.data.manager.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.data.manager.IStationManager;

import ca.ubc.cs.beta.stationpacking.experiment.InstanceGeneration;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.IExperimentReporter;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.LocalExperimentReporter;

import ca.ubc.cs.beta.stationpacking.experiment.instance.IInstance;
import ca.ubc.cs.beta.stationpacking.experiment.instance.Instance;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder.CNFEncoder;

import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfresultlookup.HybridCNFResultLookup;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfresultlookup.ICNFResultLookup;

import ca.ubc.cs.beta.stationpacking.experiment.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.experiment.solver.TAESolver;

import ca.ubc.cs.beta.stationpacking.experiment.*;

public class NickMain {

	//NA - TODO set random seed to get reproducible results.
	private static Logger log = LoggerFactory.getLogger(NickMain.class);
	private static final long fSeed = 13232;
	
	public static void main(String[] args) throws Exception {
		//String dacDomainsFile = "/Users/narnosti/Documents/fcc-station-packing/Output/toy_domains.txt";
		//String dacConstraintFile = "/Users/narnosti/Documents/fcc-station-packing/Output/toy_constraints.txt";
		//String pairwiseConstraintsFile = "/Users/narnosti/Documents/fcc-station-packing/Output/Alex_pairwise_constraints.txt";

		/*
		String newDomainsFile = "/Users/narnosti/Documents/fcc-station-packing/Output/DAC_domains.txt";
		String newConstraintsFile = "/Users/narnosti/Documents/fcc-station-packing/Output/DAC_constraints.txt";
        */
		/*
		String StationsFile = "/Users/narnosti/Documents/FCCOutput/stations.csv";
		String DomainsFile = "/Users/narnosti/Dropbox/Alex/2013 04 New Data/Domain 041813.csv";
		String ConstraintsFile = "/Users/narnosti/Dropbox/Alex/2013 04 New Data/Interferences 041813.csv";
		*/
		
		String StationsFile = "/Users/narnosti/Documents/FCCOutput/toy_stations.txt";
		String DomainsFile = "/Users/narnosti/Documents/FCCOutput/toy_domains.txt";
		String ConstraintsFile = "/Users/narnosti/Documents/FCCOutput/toy_constraints.txt";
		
		IStationManager aSM = new DACStationManager(StationsFile,DomainsFile);
		IConstraintManager aCM = new DACConstraintManager2(aSM.getStations(),ConstraintsFile);
		

		//Set<Station> aStationSet = new HashSet<Station>();
		
		Set<Integer> aChannels = new HashSet<Integer>();
		for(int i = 14; i <=16; i++){
			aChannels.add(i);
		}
		


		/*
		log.info("Getting data...");
		String aStationFileName = "/Users/narnosti/Documents/fcc-station-packing/Output/stations.csv";
		String dacDomainsFile = "/Users/narnosti/Documents/fcc-station-packing/Input/Domains.csv";
		DACStationManager aStationManager = new DACStationManager(aStationFileName,dacDomainsFile);
        Set<Station> aStations = aStationManager.getStations();
		String dacConstraintFile = "/Users/narnosti/Documents/fcc-station-packing/Input/Interferences-read-note-please.csv";
		DACConstraintManager2 dCM = new DACConstraintManager2(aStations,dacConstraintFile);
		*/
		
		ICNFEncoder aCNFEncoder = new CNFEncoder();
		//System.out.println(aCNFEncoder.decode(new NInstance(aStationSet,aChannels),FileUtils.readFileToString(new File("/Users/narnosti/Documents/fcc-station-packing/CNFs/7ff9afd8a241da50dd85dc361ab701183c18e66445.out"))));
		
		
		log.info("Creating cnf lookup...");
 
		String aCNFDir = "/Users/narnosti/Documents/fcc-station-packing/CNFs";
		ICNFResultLookup aCNFLookup = new HybridCNFResultLookup(aCNFDir,"CNFOutput");
				
		log.info("Creating solver...");
        String aParamConfigurationSpaceLocation = "/Users/narnosti/Documents/fcc-station-packing/FCCStationPacking/SATsolvers/sw_parameterspaces/sw_picosat.txt";
		String aAlgorithmExecutable = "python solverwrapper.py";
        String aExecDir = "/Users/narnosti/Documents/fcc-station-packing/FCCStationPacking/SATsolvers/";
		int aMaximumConcurrentExecution = 4;
		ISolver aSolver = new TAESolver(aCM, aCNFLookup, aCNFEncoder, aParamConfigurationSpaceLocation, aAlgorithmExecutable, aExecDir, "CLI",aMaximumConcurrentExecution, fSeed);
		
		log.info("Creating experiment reporter...");
        String testFolder = "/Users/narnosti/Documents/fcc-station-packing/FCCStationPacking/ExperimentDir";
		IExperimentReporter aExperimentReporter = new LocalExperimentReporter(testFolder, "test");
		
		Iterator<Station> aStationIterator = new InversePopulationStationIterator(aSM.getStations(), fSeed);
		
		log.info("Creating instance generation and beginning experiment...");
		Set<Station> aStartingStations = new HashSet<Station>();
		double aCutoff = 1800;
		InstanceGeneration aInstanceGeneration = new InstanceGeneration(aSolver, aExperimentReporter);
		aInstanceGeneration.run(aStartingStations, aStationIterator,aChannels,aCutoff);	
		aCNFLookup.writeToFile();
		File folder = new File(aCNFDir);
		File[] aCNFFiles = folder.listFiles();
		for(int i = 0; i < aCNFFiles.length; i++){
			String aFileName = aCNFFiles[i].getName();
			if(aFileName.endsWith("cnfoutput")){
				System.out.println(aFileName);
				IInstance aInstance = readInstanceFromCNFFile(aCNFDir+"/"+aFileName.substring(0,aFileName.indexOf("output")),new HashSet<Station>(aSM.getStations()));
				Map<Integer,Set<Station>> aStationAssignment = aCNFEncoder.decode(aInstance, FileUtils.readFileToString(aCNFFiles[i]));
				System.out.println(aStationAssignment);
				if(!aCM.isSatisfyingAssignment(aStationAssignment)){
					throw new Exception(aStationAssignment+" is not a valid assignment for instance "+aInstance);
				}
			}
		}
		
	}


private static IInstance readInstanceFromCNFFile(String aFileName, Set<Station> aStationSet) throws Exception{
	BufferedReader aReader = new BufferedReader(new FileReader(aFileName));
	Set<Station> aStations = new HashSet<Station>();
	Set<Integer> aChannels = new HashSet<Integer>();
	while(aReader.ready()){
		String aLine = aReader.readLine();
		if(aLine.startsWith("c Stations: ")){
			String[] aSplitString = aLine.substring(aLine.indexOf(":")+1).split(" ");
			for(int i = 0; i < aSplitString.length; i++){
				//System.out.print(aSplitString[i]+" ");
				if(aSplitString[i].length() > 0){
					Integer aID = Integer.parseInt(aSplitString[i]);
					aStations.add(new Station(aID, aChannels,0));
				}
			}
			aStationSet.retainAll(aStations);
			if(aStationSet.size()!=aStations.size()){
				aReader.close();
				throw new Exception("When decoding: some stations not found in stations file");
			}
		} else if(aLine.startsWith("c Channels: ")){
			String[] aSplitString = aLine.substring(aLine.indexOf(":")+1).split(" ");
			for(int i = 0; i < aSplitString.length; i++){
				if(aSplitString[i].length() > 0){
					aChannels.add(Integer.parseInt(aSplitString[i]));
				}
			}
		}
	}
	aReader.close();
	IInstance aInstance = new Instance(aStationSet,aChannels);
	return aInstance;
}
}

