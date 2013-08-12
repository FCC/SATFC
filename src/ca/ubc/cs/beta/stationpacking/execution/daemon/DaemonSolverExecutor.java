package ca.ubc.cs.beta.stationpacking.execution.daemon;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.options.UsageSection;
import ca.ubc.cs.beta.aclib.options.ConfigToLaTeX;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.daemon.server.SolverServer;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.ExecutableSolverParameters;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

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
		ExecutableSolverParameters aExecutableSolverParameters = new ExecutableSolverParameters();
		JCommander aParameterParser = JCommanderHelper.getJCommander(aExecutableSolverParameters, aExecutableSolverParameters.SolverParameters.TAESATSolverParameters.AvailableTAEOptions);
		try
		{
			aParameterParser.parse(args);
		}
		catch (ParameterException aParameterException)
		{
			List<UsageSection> sections = ConfigToLaTeX.getParameters(aExecutableSolverParameters, aExecutableSolverParameters.SolverParameters.TAESATSolverParameters.AvailableTAEOptions);
			
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
				
				aSolver = aExecutableSolverParameters.getSolver();
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
