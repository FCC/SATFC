package ca.ubc.cs.beta.stationpacking.execution;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.data.manager.HRStationManager;
import ca.ubc.cs.beta.stationpacking.data.manager.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.data.manager.IStationManager;
import ca.ubc.cs.beta.stationpacking.data.manager.NoFixedHRConstraintManager;
import ca.ubc.cs.beta.stationpacking.experiment.InstanceGeneration;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.IExperimentReporter;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.LocalExperimentReporter;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.IInstanceEncoder;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.InstanceEncoder;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder.StaticCNFEncoder;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnflookup.DirCNFLookup;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnflookup.ICNFLookup;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.componentgrouper.ConstraintGraphComponentGrouper;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.experiment.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.experiment.solver.TAESolver;
import ca.ubc.cs.beta.stationpacking.experiment.stationiterators.InversePopulationStationIterator;

public class AlexMain {

	private static Logger log = LoggerFactory.getLogger(AlexMain.class);
	
	public static void main(String[] args) throws Exception {
		
		Random aRandomizer = new Random(2);

		log.info("Getting data...");
		IStationManager aStationManager = new HRStationManager("/Users/MightyByte/Documents/data/FCCStationPackingData/stations.csv");
		IConstraintManager aConstraintManager = new NoFixedHRConstraintManager("/Users/MightyByte/Documents/data/FCCStationPackingData/AllowedChannels.csv", "/Users/MightyByte/Documents/data/FCCStationPackingData/PairwiseConstraints.csv",aStationManager.getFixedStations());
		
		log.info("Encoding data...");
		ICNFEncoder aCNFEncoder = new StaticCNFEncoder(aConstraintManager.getStationDomains(), aConstraintManager.getPairwiseConstraints());
		log.info("Making station groups...");
		IComponentGrouper aComponentGrouper = new ConstraintGraphComponentGrouper(aStationManager.getUnfixedStations(),aConstraintManager.getPairwiseConstraints());
		log.info("Creating cnf lookup...");
		String aCNFDir = "/Users/MightyByte/Documents/data/FCCStationPackingData/CNFs";
		ICNFLookup aCNFLookup = new DirCNFLookup(aCNFDir);
		
		log.info("Creating instance encoder...");
		IInstanceEncoder aInstanceEncoder = new InstanceEncoder(aCNFEncoder, aCNFLookup, aComponentGrouper, aCNFDir);
		
		log.info("Creating solver...");
		String aParamConfigurationSpaceLocation = "SATsolvers/sw_parameterspaces/sw_picosat.txt";
		String aAlgorithmExecutable = "python solverwrapper.py";
		String aExecDir = "SATsolvers/";
		int aMaximumConcurrentExecution = 4;
		ISolver aSolver = new TAESolver(aParamConfigurationSpaceLocation, aAlgorithmExecutable, aExecDir, "CLI",aMaximumConcurrentExecution);
		
		log.info("Creating experiment reporter...");
		IExperimentReporter aExperimentReporter = new LocalExperimentReporter("/Users/MightyByte/Documents/data/FCCStationPackingData", "test");
		
		log.info("Creating instance generation experiment...");
		InstanceGeneration aInstanceGeneration = new InstanceGeneration(aInstanceEncoder, aSolver, aExperimentReporter);
		
		log.info("Creating station iterator...");
		Iterator<Station> aStationIterator = new InversePopulationStationIterator(aStationManager.getUnfixedStations(), aStationManager.getStationPopulation(), aRandomizer);
		log.info("Getting starting stations...");
		Set<Station> aStartingStations = new HashSet<Station>();
		
		double aCutoff = 1800;
		
		aInstanceGeneration.run(aStartingStations, aStationIterator,aCutoff);
		
	
		
	}

}
