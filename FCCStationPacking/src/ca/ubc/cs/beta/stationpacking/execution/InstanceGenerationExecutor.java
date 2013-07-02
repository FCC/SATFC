package ca.ubc.cs.beta.stationpacking.execution;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.options.UsageSection;
import ca.ubc.cs.beta.aclib.options.ConfigToLaTeX;
import ca.ubc.cs.beta.stationpacking.execution.parameters.instancegeneration.InstanceGenerationParameters;
import ca.ubc.cs.beta.stationpacking.experiment.InstanceGeneration;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class InstanceGenerationExecutor {

	private static Logger log = LoggerFactory.getLogger(InstanceGenerationExecutor.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		String[] aPaxosTargetArgs = {
				"-EXPERIMENT_NAME",
				"InstanceGenerationTest",
				"-STATION_POPULATIONS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/station-pops.csv",
				"-EXPERIMENT_DIR",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Results/TestExperiment",
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
				"1800",
				"-PACKING_CHANNELS",
				"14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30",
				};
		
		args = aPaxosTargetArgs;
		
		//Parse the command line arguments in a parameter object.
		InstanceGenerationParameters aInstanceGenerationParameters = new InstanceGenerationParameters();
		JCommander aParameterParser = JCommanderHelper.getJCommander(aInstanceGenerationParameters, aInstanceGenerationParameters.SolverParameters.AvailableTAEOptions);
		try
		{
			aParameterParser.parse(args);
		}
		catch (ParameterException aParameterException)
		{
			List<UsageSection> sections = ConfigToLaTeX.getParameters(aInstanceGenerationParameters,aInstanceGenerationParameters.SolverParameters.AvailableTAEOptions);
			
			boolean showHiddenParameters = false;
			
			//A much nicer usage screen than JCommander's 
			ConfigToLaTeX.usage(sections, showHiddenParameters);
			
			log.error(aParameterException.getMessage());
			return;
		}
		
		InstanceGeneration aInstanceGeneration = null;
		try
		{
			try 
			{
				aInstanceGeneration = aInstanceGenerationParameters.getInstanceGeneration();
				
				aInstanceGeneration.run(aInstanceGenerationParameters.getStartingStations(), aInstanceGenerationParameters.getStationIterator(), aInstanceGenerationParameters.getPackingChannels(), aInstanceGenerationParameters.Cutoff, aInstanceGenerationParameters.Seed);

			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			
		}
		finally{
			aInstanceGeneration.getSolver().notifyShutdown();
		}
		
		
		
		

	}

}
