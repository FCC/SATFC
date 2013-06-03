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
import ca.ubc.cs.beta.stationpacking.execution.cnfencoder.CNFEncoder;
import ca.ubc.cs.beta.stationpacking.execution.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.execution.parameters.parsers.ParameterParser;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.solver.TAESolver.TAESolver;
import ca.ubc.cs.beta.stationpacking.solver.TAESolver.cnflookup.HybridCNFResultLookup;
import ca.ubc.cs.beta.stationpacking.solver.TAESolver.cnflookup.ICNFResultLookup;


public class MainSolver {
	
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
				"/Users/narnosti/Documents/fcc-station-packing/CNFs",
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
	}
	*/
	
	public MainSolver(String[] args) throws Exception{
		
		log.info("Parsing parameters...");
		Map<String,AbstractOptions> aAvailableTAEOptions = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
		ParameterParser aExecParameters = getParameterParser(args,aAvailableTAEOptions);
		
		log.info("Getting station information...");
		fStationManager = new DACStationManager(aExecParameters.getRepackingDataParameters().getStationFilename(),aExecParameters.getRepackingDataParameters().getDomainFilename());
	    
		log.info("Getting constraint information...");
		Set<Station> aStations = fStationManager.getStations();
		DACConstraintManager2 aConstraintManager = new DACConstraintManager2(aStations,aExecParameters.getRepackingDataParameters().getConstraintFilename());
	
		log.info("Creating solver...");
		ICNFEncoder aCNFEncoder = new CNFEncoder();
		
		
		if(true/*the solver requested requires a TAEsolver*/){
			
			ICNFResultLookup aCNFLookup = new HybridCNFResultLookup(aExecParameters.getCNFDirectory(), aExecParameters.getCNFOutputName());

			
			//Fix config space file based on solver
			aExecParameters.getAlgorithmExecutionOptions().paramFileDelegate.paramFile = aExecParameters.getAlgorithmExecutionOptions().algoExecDir+File.separatorChar+"sw_parameterspaces"+File.separatorChar+"sw_"+aExecParameters.getSolver()+".txt";
			AlgorithmExecutionConfig aTAEExecConfig = aExecParameters.getAlgorithmExecutionOptions().getAlgorithmExecutionConfig();
			TargetAlgorithmEvaluator aTAE = null;
			try {
				aTAE = TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(aExecParameters.getAlgorithmExecutionOptions().taeOpts, aTAEExecConfig, false, aAvailableTAEOptions);
				fSolver = new TAESolver(aConstraintManager, aCNFEncoder, aCNFLookup, aTAE, aTAEExecConfig,aExecParameters.getSeed());			
			} finally {
				//We need to tell the TAE we are shutting down, otherwise the program may not exit 
				if(aTAE != null){ aTAE.notifyShutdown();}
			}
			
		}
		
		/*
		branch on solver type
			if(TAESolver){
				construct a CNFLookup, an AlgorithmExecutionConfig, a TargetAlgorithmEvaluator
				fSolver = new TAESolver(aConstraintManager, aCNFEncoder, aCNFLookup, aTAE, aTAEExecConfig,aExecParameters.getSeed());			
			} else if(Incremental Solver){
				construct an IncrementalSATLibrary, algorithm Execution options (which type of incremental, how many dummy variables)
				fSolver = new TAESolver(aConstraintManager, aCNFEncoder, aIncrementalSATLibrary,aIncrementalParams, aExecParameters.getSeed());			

			}

		 */
		
	}
	
	public SolverResult receiveMessage(Set<Integer> aStationIDs, Set<Integer> aChannels, double aCutoff) throws Exception{
		Set<Station> aStations = new HashSet<Station>();
		for(Integer aStationID : aStationIDs){
			aStations.add(fStationManager.get(aStationID));
		}
		Instance aInstance = new Instance(aStations,aChannels);
		SolverResult aSolverResult = fSolver.solve(aInstance,aCutoff);
		return(aSolverResult);
	}
	
	static private ParameterParser getParameterParser(String[] args, Map<String,AbstractOptions> aAvailableTAEOptions){
	
		//Parse the command line arguments in a parameter object.
		ParameterParser aExecParameters = new ParameterParser();
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
}