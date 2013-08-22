package ca.ubc.cs.beta.stationpacking.execution.daemon.simple;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.options.UsageSection;
import ca.ubc.cs.beta.aclib.options.ConfigToLaTeX;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.daemon.simple.server.SolverServer;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.DaemonSolverParameters;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * Solver that listens for messages and executes corresponding commands. 
 * @author afrechet
 *
 */
public class DaemonSolverLauncher {

	private static Logger log = LoggerFactory.getLogger(DaemonSolverLauncher.class);
	
	public static void main(String[] args) throws Exception {
		
		//Parse the command line arguments in a parameter object.
		DaemonSolverParameters aDaemonSolverParameters = new DaemonSolverParameters();
		JCommander aParameterParser = JCommanderHelper.getJCommander(aDaemonSolverParameters, TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators());
		try
		{
			aParameterParser.parse(args);
		}
		catch (ParameterException aParameterException)
		{
			List<UsageSection> sections = ConfigToLaTeX.getParameters(aDaemonSolverParameters, TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators());
			
			boolean showHiddenParameters = false;
			
			//A much nicer usage screen than JCommander's 
			ConfigToLaTeX.usage(sections, showHiddenParameters);
			
			log.error(aParameterException.getMessage());
			return;
		}
		
		log.info("Creating solver server.");
		SolverServer aSolverServer = aDaemonSolverParameters.getSolverServer(); 
		try
		{
			log.info("Starting daemon solver server.");
			aSolverServer.start();
		}
		finally
		{
			log.info("Shutting down solver server.");
			aSolverServer.notifyShutdown();
		}
		
		
	}
	

}
