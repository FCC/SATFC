package ca.ubc.cs.beta.stationpacking.execution;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.options.UsageSection;
import ca.ubc.cs.beta.aclib.options.ConfigToLaTeX;
import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.execution.parameters.ExecutableSolverParameters;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class SolverExecutor {

	private static Logger log = LoggerFactory.getLogger(SolverExecutor.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
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
				"--algoExec",
				"python solverwrapper.py",
				"--cutoffTime",
				"1800",
				"--logAllCallStrings",
				"true",
				"-PACKING_CHANNELS",
				"14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30",
				"-PACKING_STATIONS",
				"25684,32334,39664"
				};
		
		args = aPaxosTargetArgs;
		
		//Parse the command line arguments in a parameter object.
		ExecutableSolverParameters aExecutableSolverParameter = new ExecutableSolverParameters();
		JCommander aParameterParser = JCommanderHelper.getJCommander(aExecutableSolverParameter, aExecutableSolverParameter.SolverParameters.AvailableTAEOptions);
		try
		{
			aParameterParser.parse(args);
		}
		catch (ParameterException aParameterException)
		{
			List<UsageSection> sections = ConfigToLaTeX.getParameters(aExecutableSolverParameter,aExecutableSolverParameter.SolverParameters.AvailableTAEOptions);
			
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
				
				aSolver = aExecutableSolverParameter.getSolver();
				Instance aInstance = aExecutableSolverParameter.getInstance();
				
				log.info("Solving instance {}",aInstance);
				
				SolverResult aResult = aSolver.solve(aInstance, aExecutableSolverParameter.ProblemInstanceParameters.Cutoff, aExecutableSolverParameter.ProblemInstanceParameters.Seed);
				
				log.info("Solved.");
				log.info("Result : {}",aResult);
				
				System.out.println(aResult);
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
