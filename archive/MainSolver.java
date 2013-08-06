package ca.ubc.cs.beta.stationpacking.execution;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.options.UsageSection;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.options.ConfigToLaTeX;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorBuilder;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;

import ca.ubc.cs.beta.stationpacking.datamanagers.DACConstraintManager2;
import ca.ubc.cs.beta.stationpacking.datamanagers.DACStationManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.execution.parameters.ExecutableSolverParameters;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.CNFEncoder2;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.ICNFEncoder2;
import ca.ubc.cs.beta.stationpacking.solver.cnfwriter.CNFStringWriter;
import ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.IncrementalSolver;
import ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.SATLibraries.GlueMiniSatLibrary;
import ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.SATLibraries.IIncrementalSATLibrary;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.TAESolver;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.cnflookup.HybridCNFResultLookup;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.cnflookup.ICNFResultLookup;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.componentgrouper.IComponentGrouper;


/**
 * This class should serve as one of the two main methods in the final architecture.
 * Its primary purpose is to parse arguments, determine which solver type to use,
 * create a solver, and then wait for messages.
 * 
 * @author narnosti
 * @deprecated
 */
public class MainSolver  implements ISolver{
	
	private static Logger log = LoggerFactory.getLogger(MainSolver.class);
	private ISolver fSolver;
	private DACStationManager fStationManager;
	
	/*
	public static void main(String[] args){
		String[] aPaxosTargetArgs = {
				"-STATIONS_FILE",
				"/Users/narnosti/Documents/FCCOutput/toy_stations.txt",
				"-DOMAINS_FILE",
				"/Users/narnosti/Documents/FCCOutput/toy_domains.txt",
				"-CONSTRAINTS_FILE",
				"/Users/narnosti/Documents/FCCOutput/toy_constraints.txt",
				"-CNF_DIR",
				"/Users/narnosti/Documents/FCCOutput/CNFs",
				"-SOLVER",
				"picosat",
				"--execDir",
				"SATsolvers",
				//"--paramFile",
				//"SATsolvers/sw_parameterspaces/sw_tunedclasp.txt",
				"--algoExec",
				"python solverwrapper.py",
				"--cutoffTime",
				"1800"
				};
		
		args = aPaxosTargetArgs;
		
		try{
			MainSolver a = new MainSolver(args);
		} catch(Exception e){
			e.printStackTrace();
		}
		
		boolean terminate = false
		while(!terminate){
			//Wait for message, if it's a new instance "decode" it and
			//return aSolver.receiveMessage(,);
			//else if it's a quit message:
			//terminate = true;
		}
		
	}
	*/
	
	
	/*
	 * Currently, we pass args through the constructor.
	 * Eventually, we want this to be a main method (commented out above).
	 */
	public MainSolver(String[] args) throws Exception{
		
		log.info("Parsing parameters...");
		Map<String,AbstractOptions> aAvailableTAEOptions = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
		ExecutableSolverParameters aExecParameters = getParameterParser(args,aAvailableTAEOptions);
		
		log.info("Getting station information...");
		fStationManager = new DACStationManager(aExecParameters.getRepackingDataParameters().getStationFilename(),aExecParameters.getRepackingDataParameters().getDomainFilename());
	    
		log.info("Getting constraint information...");
		Set<Station> aStations = fStationManager.getStations();
		DACConstraintManager2 aConstraintManager = new DACConstraintManager2(aStations,aExecParameters.getRepackingDataParameters().getConstraintFilename());
		Set<Integer> aChannels = aExecParameters.getPackingChannels();

		log.info("Creating solver...");
		ICNFEncoder2 aCNFEncoder = new CNFEncoder2(aStations);
		
		if(aExecParameters.useIncrementalSolver()){ //Create IncrementalSolver
			/* May want more parameters in the future; for example
			 * -Implement more incremental solvers; don't always call GlueMiniSatLibrary below
			 * -Implement an incremental solver using memory copying rather than dummy variables
			 */
			String aLibraryPath = aExecParameters.getIncrementalLibraryLocation();
			IIncrementalSATLibrary aSATLibrary = new GlueMiniSatLibrary(aLibraryPath);
			fSolver = new IncrementalSolver(aConstraintManager, aCNFEncoder, aSATLibrary);

		} else { //Create TAESolver			
			
			ICNFResultLookup aCNFLookup = new HybridCNFResultLookup(aExecParameters.getCNFDirectory(), aExecParameters.getCNFOutputName());
			IComponentGrouper aGrouper = new ConstraintGrouper();
			
			//Fix config space file based on solver
			aExecParameters.getAlgorithmExecutionOptions().paramFileDelegate.paramFile = aExecParameters.getAlgorithmExecutionOptions().algoExecDir+File.separatorChar+"sw_parameterspaces"+File.separatorChar+"sw_"+aExecParameters.getSolver()+".txt";
			AlgorithmExecutionConfig aTAEExecConfig = aExecParameters.getAlgorithmExecutionOptions().getAlgorithmExecutionConfig();
			TargetAlgorithmEvaluator aTAE = null;
			try {
				aTAE = TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(aExecParameters.getAlgorithmExecutionOptions().taeOpts, aTAEExecConfig, false, aAvailableTAEOptions);
				fSolver = new TAESolver(aConstraintManager, aCNFEncoder, aCNFLookup, aGrouper, new CNFStringWriter(),aTAE, aTAEExecConfig,aExecParameters.getSeed());			
			} finally {
				//We need to tell the TAE we are shutting down, otherwise the program may not exit 
				if(aTAE != null){ aTAE.notifyShutdown();}
			}
			
		}
		/*
		 while(true){ 
		 	//wait for message; decode and solve when it arrives
		 }
		 */
	}
	
	/*
	 * A temporary stub; currently used to send "instances" to solve 
	 * (without having to have the actual "station" information)
	 */
	public SolverResult receiveMessage(Set<Integer> aStationIDs, Set<Integer> aChannels,double aCutoff) throws Exception{
		
		//Turn station IDs into a set of stations
		Set<Station> aStations = new HashSet<Station>();
		Station aStation;
		for(Integer aID : aStationIDs){
			if((aStation=fStationManager.get(aID))!=null) aStations.add(aStation);
			else throw new Exception("Unrecognized station: "+aID);
		}
		
		//Create an instance and solve
		if(aStations.size()>0&&aChannels.size()>0){
			Instance aInstance = new Instance(aStations,aChannels);
			return fSolver.solve(aInstance, aCutoff);
		} else {
			throw new Exception("Invalid Instance: recognized station set is: "+aStations+", channels are: "+aChannels);
		}
	}
	
	/*
	 * Copied existing code; parse parameters 
	 */
	static private ExecutableSolverParameters getParameterParser(String[] args, Map<String,AbstractOptions> aAvailableTAEOptions){
		//Parse the command line arguments in a parameter object.
		ExecutableSolverParameters aExecParameters = new ExecutableSolverParameters();
		JCommander aParameterParser = JCommanderHelper.getJCommander(aExecParameters, aAvailableTAEOptions);
		try{
			aParameterParser.parse(args);
		} catch (ParameterException aParameterException){

			List<UsageSection> sections = ConfigToLaTeX.getParameters(aExecParameters, aAvailableTAEOptions);
			boolean showHiddenParameters = false;
			//A much nicer usage screen than JCommander's 
			ConfigToLaTeX.usage(sections, showHiddenParameters);
			log.error(aParameterException.getMessage());
		}
		return aExecParameters;
	}

	/*
	 * Added to that MainSolver implements ISolver (so that we can easily use InstanceGeneration)
	 */
	@Override
	public SolverResult solve(Instance aInstance, double aCutoff)
			throws Exception {
		return(fSolver.solve(aInstance, aCutoff));
	}
}