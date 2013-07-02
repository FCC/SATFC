package ca.ubc.cs.beta.stationpacking.execution;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.options.UsageSection;
import ca.ubc.cs.beta.aclib.options.ConfigToLaTeX;
import ca.ubc.cs.beta.stationpacking.datastructures.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.execution.parameters.asyncresolving.AsyncResolvingParameters;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.AsyncTAESolver;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class AsyncInstanceResolvingExecutor {

	private static Logger log = LoggerFactory.getLogger(InstanceGenerationExecutor.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		String[] aPaxosTargetArgs = {
				"-EXPERIMENT_DIR",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Results/Resolving",
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
				"--cores",
				"6",
				"-CUTOFF",
				"1800"
				};
		
		args = aPaxosTargetArgs;
		
		//Parse the command line arguments in a parameter object.
		AsyncResolvingParameters aInstanceResolvingParameters = new AsyncResolvingParameters();
		JCommander aParameterParser = JCommanderHelper.getJCommander(aInstanceResolvingParameters, aInstanceResolvingParameters.SolverParameters.AvailableTAEOptions);
		try
		{
			aParameterParser.parse(args);
		}
		catch (ParameterException aParameterException)
		{
			List<UsageSection> sections = ConfigToLaTeX.getParameters(aInstanceResolvingParameters,aInstanceResolvingParameters.SolverParameters.AvailableTAEOptions);
			
			boolean showHiddenParameters = false;
			
			//A much nicer usage screen than JCommander's 
			ConfigToLaTeX.usage(sections, showHiddenParameters);
			
			log.error(aParameterException.getMessage());
			return;
		}
		
		AsyncTAESolver aSolver = null;
		try
		{
			try 
			{
				aSolver = aInstanceResolvingParameters.SolverParameters.getSolver();
				
				ArrayList<StationPackingInstance> aInstances = aInstanceResolvingParameters.getInstances();
				
				for(StationPackingInstance aInstance : aInstances)
				{
					aSolver.solve(aInstance, aInstanceResolvingParameters.Cutoff, aInstanceResolvingParameters.Seed, aInstanceResolvingParameters.getExperimentReporter());
				}

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
