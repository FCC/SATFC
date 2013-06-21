package ca.ubc.cs.beta.stationpacking.execution.daemon;


import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.options.UsageSection;
import ca.ubc.cs.beta.aclib.options.ConfigToLaTeX;
import ca.ubc.cs.beta.stationpacking.datamanagers.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.daemon.server.SolverServer;
import ca.ubc.cs.beta.stationpacking.execution.parameters.TAESolverParameters;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;

/**
 * Solver that listens for messages and executes corresponding commands. 
 * @author afrechet
 *
 */
public class DaemonSolverExecutor {

	private static Logger log = LoggerFactory.getLogger(DaemonSolverExecutor.class);
	
	public static void main(String[] args) throws Exception {
		
		/*
		 * 
		 */
		
		String[] aPaxosTargetArgs = {
				"-STATIONS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/stations2.csv",
				"-DOMAINS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/NewDACData/Domain-041813.csv",
				"-CONSTRAINTS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/NewDACData/Interferences-041813.csv",
				"-CNF_DIR",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/CNFs",
				"-SOLVER",
				"tunedclasp",
				"--execDir",
				"SATsolvers",
				/*
				"--paramFile",
				"SATsolvers/sw_parameterspaces/sw_tunedclasp.txt",
				*/
				"--algoExec",
				"python solverwrapper.py",
				"--cutoffTime",
				"1800",
				"--logAllCallStrings",
				"true"
				};
		
		/*
		 * 
		 */
		
		args = aPaxosTargetArgs;
		
		//Parse the command line arguments in a parameter object.
		TAESolverParameters aExecutableSolverParameters = new TAESolverParameters();
		JCommander aParameterParser = JCommanderHelper.getJCommander(aExecutableSolverParameters, aExecutableSolverParameters.AvailableTAEOptions);
		try
		{
			aParameterParser.parse(args);
		}
		catch (ParameterException aParameterException)
		{
			List<UsageSection> sections = ConfigToLaTeX.getParameters(aExecutableSolverParameters, aExecutableSolverParameters.AvailableTAEOptions);
			
			boolean showHiddenParameters = false;
			
			//A much nicer usage screen than JCommander's 
			ConfigToLaTeX.usage(sections, showHiddenParameters);
			
			log.error(aParameterException.getMessage());
			return;
		}
		
		
		ISolver aSolver = null;
		try
		{
			try 
			{
				
				aSolver = aExecutableSolverParameters.getTAESolver();
				IStationManager aStationManager = aExecutableSolverParameters.RepackingDataParameters.getDACStationManager();
				log.info("Creating solver server...");
				//Create and start the solver server.
				SolverServer aSolverServer = new SolverServer(aSolver, aStationManager, 8080);
				
				log.info("Starting solver server");
				aSolverServer.start();
				
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			
		}
		finally{
			aSolver.notifyShutdown();
		}
		
		
	}
	

}
