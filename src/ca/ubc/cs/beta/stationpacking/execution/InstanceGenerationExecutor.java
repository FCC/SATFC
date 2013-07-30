package ca.ubc.cs.beta.stationpacking.execution;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.options.UsageSection;
import ca.ubc.cs.beta.aclib.options.ConfigToLaTeX;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
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

		//Parse the command line arguments in a parameter object.
		InstanceGenerationParameters aInstanceGenerationParameters = new InstanceGenerationParameters();
		JCommander aParameterParser = JCommanderHelper.getJCommander(aInstanceGenerationParameters, TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators());
		try
		{
			aParameterParser.parse(args);
		}
		catch (ParameterException aParameterException)
		{
			List<UsageSection> sections = ConfigToLaTeX.getParameters(aInstanceGenerationParameters, TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators());
			
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
