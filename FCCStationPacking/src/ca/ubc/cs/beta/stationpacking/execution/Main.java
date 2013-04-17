package ca.ubc.cs.beta.stationpacking.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;



import ca.ubc.cs.beta.stationpacking.data.*;
import ca.ubc.cs.beta.stationpacking.data.manager.DACConstraintManager;
import ca.ubc.cs.beta.stationpacking.data.manager.HRConstraintManager;
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
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder.NickCNFEncoder;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder.StaticCNFEncoder;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnflookup.CachedCNFLookup;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnflookup.DirCNFLookup;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnflookup.ICNFLookup;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.componentgrouper.ConstraintGraphComponentGrouper;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.experiment.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.experiment.solver.TAESolver;
import ca.ubc.cs.beta.stationpacking.experiment.stationiterators.InversePopulationStationIterator;
*/


public class Main {

	private static Logger log = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) throws Exception {
		
        //NA - yes, I know; terrible, lazy names.
        /*
        String s1 = "/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/stations.csv";
        String s2 = "/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/AllowedChannels.csv";
        String s3 = "/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/PairwiseConstraints.csv";
        */
		
		//String s1 = "/Users/narnosti/Documents/fcc-station-packing/Output/toy_stations.txt";
		log.info("Assigning string literals.");
		/*
        String s1 = "/Users/narnosti/Documents/fcc-station-packing/Output/stations.csv";
        String s2 = "/Users/narnosti/Documents/fcc-station-packing/Input/AllowedChannels.csv";
        String s3 = "/Users/narnosti/Documents/fcc-station-packing/Output/PairwiseConstraints.csv";
        */
		//String dacDomainsFile = "/Users/narnosti/Documents/fcc-station-packing/Output/toy_domains.txt";
		//String dacConstraintFile = "/Users/narnosti/Documents/fcc-station-packing/Output/toy_constraints.txt";
		//String pairwiseConstraintsFile = "/Users/narnosti/Documents/fcc-station-packing/Output/Alex_pairwise_constraints.txt";

		/*
        String stationsFile = "/Users/narnosti/Documents/fcc-station-packing/Output/stations.csv";
        String channelsFile = "/Users/narnosti/Documents/fcc-station-packing/Input/AllowedChannels.csv";
        String constraintsFile = "/Users/narnosti/Documents/fcc-station-packing/Output/PairwiseConstraints.csv";
        
		String dacDomainsFile = "/Users/narnosti/Documents/fcc-station-packing/Input/Domains.csv";
		String dacConstraintFile = "/Users/narnosti/Documents/fcc-station-packing/Input/Interferences-read-note-please.csv";

		String newDomainsFile = "/Users/narnosti/Documents/fcc-station-packing/Output/DAC_domains.txt";
		String newConstraintsFile = "/Users/narnosti/Documents/fcc-station-packing/Output/DAC_constraints.txt";
        
		IStationManager aStationManager = new HRStationManager(stationsFile);
		IConstraintManager aCM = new NoFixedHRConstraintManager(channelsFile, constraintsFile ,aStationManager.getFixedStations());
        		
		DACConstraintManager dCM = new DACConstraintManager(dacDomainsFile,dacConstraintFile);
		DACConstraintManager dCM2 = new DACConstraintManager(aCM.getStationDomains(),aCM.getPairwiseConstraints());
		DACConstraintManager dCM3 = new DACConstraintManager(dCM.getStationDomains(),dCM.getPairwiseConstraints());
		dCM.writeConstraints(newConstraintsFile);
		dCM.writeDomains(newDomainsFile);
		DACConstraintManager dCM4 = new DACConstraintManager(newDomainsFile,newConstraintsFile);

		System.out.println(dCM2.matchesConstraints(aCM));
		System.out.println(dCM2.matchesDomains(aCM));
		System.out.println(dCM.matchesConstraints(dCM2));
		System.out.println(dCM.matchesDomains(dCM2));
		System.out.println(dCM.matchesConstraints(dCM3));
		System.out.println(dCM.matchesDomains(dCM3));
		System.out.println(dCM.matchesConstraints(dCM4));
		System.out.println(dCM.matchesDomains(dCM4));
		 */
		//dCM2.writePairwiseConstraints(pairwiseConstraintsFile);
		/*


		 */
        /*
        try{
        	String bigString = "s SATISFIABLE\n v -1 -2 -3 -4 -5 -6 -7 -8 -9 -10 -11 -12 -13 14 -15 -16 -17 -18 -19 -20 -21 -22\n v -23 -24 -25 -26 -27 -28 -29 -30 31 0\n CPU time (s): 0.24854516983";
        	System.out.println(stringToAssignment(bigString));
        } catch(Exception e){
        	e.printStackTrace();
        }
        */
        /*
        int bigNum = 100000;
        long startTime = System.currentTimeMillis();
        String a = "";
        for(int i = 1; i < bigNum; i++){
        	a += new Integer(i);
        }
        long middleTime = System.currentTimeMillis();
        StringBuilder aBuilder = new StringBuilder();
        for(int i = 1; i < bigNum; i++){
        	aBuilder.append(new Integer(i));
        }
        a = aBuilder.toString();
        long finalTime = System.currentTimeMillis();
        System.out.println("First approach took "+(middleTime-startTime)+" milliseconds, second took "+(finalTime-middleTime)+" milliseconds.");
         */
        
        /*
		Random aRandomizer = new Random(2);

		log.info("Getting data...");
		IStationManager aStationManager = new HRStationManager(s1);
		IConstraintManager aConstraintManager = new NoFixedHRConstraintManager(s2, s3 ,aStationManager.getFixedStations());
		
		log.info("Encoding data...");
		ICNFEncoder aCNFEncoder = new StaticCNFEncoder(aConstraintManager.getStationDomains(), aConstraintManager.getPairwiseConstraints(new HashSet<Integer>()));
		log.info("Making station groups...");
		IComponentGrouper aComponentGrouper = new ConstraintGraphComponentGrouper(aStationManager.getUnfixedStations(),aConstraintManager.getPairwiseConstraints(new HashSet<Integer>()));
		log.info("Creating cnf lookup...");
 
		//String aCNFDir = "/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/CNFs";
		String aCNFDir = "/Users/narnosti/Documents/fcc-station-packing/Output/CNFs";
		ICNFLookup aCNFLookup = new DirCNFLookup(aCNFDir);
		
		log.info("Creating instance encoder...");
		IInstanceEncoder aInstanceEncoder = new InstanceEncoder(aCNFEncoder, aCNFLookup, aComponentGrouper, aCNFDir);
		
		log.info("Creating solver...");
		//String aParamConfigurationSpaceLocation = "/ubc/cs/home/a/afrechet/arrow-space/git/fcc-station-packing/FCCStationPacking/SATsolvers/sw_parameterspaces/sw_picosat.txt";
        String aParamConfigurationSpaceLocation = "/Users/narnosti/Documents/fcc-station-packing/FCCStationPacking/SATsolvers/sw_parameterspaces/sw_picosat.txt";
		String aAlgorithmExecutable = "python solverwrapper.py";
		//String aExecDir = "/ubc/cs/home/a/afrechet/arrow-space/git/fcc-station-packing/FCCStationPacking/SATsolvers/";
        String aExecDir = "/Users/narnosti/Documents/fcc-station-packing/FCCStationPacking/SATsolvers/";
		int aMaximumConcurrentExecution = 4;
		ISolver aSolver = new TAESolver(aParamConfigurationSpaceLocation, aAlgorithmExecutable, aExecDir, "CLI",aMaximumConcurrentExecution);
		
		log.info("Creating experiment reporter...");
        //String testFolder = "/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir";
        String testFolder = "/Users/narnosti/Documents/fcc-station-packing/FCCStationPacking/ExperimentDir";
		IExperimentReporter aExperimentReporter = new LocalExperimentReporter(testFolder, "test");
		
		log.info("Creating instance generation experiment...");
		InstanceGeneration aInstanceGeneration = new InstanceGeneration(aInstanceEncoder, aSolver, aExperimentReporter);
		
		log.info("Creating station iterator...");
		Iterator<Station> aStationIterator = new InversePopulationStationIterator(aStationManager.getUnfixedStations(), aStationManager.getStationPopulation(), aRandomizer);
		log.info("Getting starting stations...");
		Set<Station> aStartingStations = new HashSet<Station>();
		
		double aCutoff = 1800;
		
		aInstanceGeneration.run(aStartingStations, aStationIterator,aCutoff);	
		*/
		
	}
/*
	private static Map<Integer,Boolean> stringToAssignment(String aCNFAssignment) throws Exception{
		Map<Integer,Boolean> aAssignment = new HashMap<Integer,Boolean>();
		String[] a = aCNFAssignment.substring(aCNFAssignment.indexOf(" v "),aCNFAssignment.indexOf("CPU")).split(" |\n");
		for(int i = 0; i < a.length; i++){
			if(!(a[i].equals("v")||a[i].isEmpty())){
				if(a[i].startsWith("-")) {
					if(aAssignment.put(Integer.parseInt(a[i].substring(1)),false)!=null)
						throw new Exception("Variable assigned to multiple truth values.");
				} else 
					if(aAssignment.put(Integer.parseInt(a[i]),true)!=null)
						throw new Exception("Variable assigned to multiple truth values.");
			}
		}
		if(aAssignment.remove(0)==null) throw new Exception("No terminating 0 in CNF string.");
		return aAssignment;
	}
	*/
}
