package ca.ubc.cs.beta.stationpacking.execution.instancegeneration;

import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.execution.parameters.instancegeneration.InstanceGenerationParameters;
import ca.ubc.cs.beta.stationpacking.experiment.InstanceGeneration;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class InstanceGenerationExecutor {

	//private static Logger log = LoggerFactory.getLogger(InstanceGenerationExecutor.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		//Parse the command line arguments in a parameter object.
		InstanceGenerationParameters aInstanceGenerationParameters = new InstanceGenerationParameters();
		//Check for help
		JCommander aParameterParser = JCommanderHelper.parseCheckingForHelpAndVersion(args, aInstanceGenerationParameters,TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators());
		
		try
		{
			aParameterParser.parse(args);
		}
		catch (ParameterException aParameterException)
		{
			throw aParameterException;
		}
		
		InstanceGeneration aInstanceGeneration = null;
		try
		{
		
			aInstanceGeneration = aInstanceGenerationParameters.getInstanceGeneration();
				
			aInstanceGeneration.run(aInstanceGenerationParameters.getStartingStations(), aInstanceGenerationParameters.getStationIterator(), aInstanceGenerationParameters.getPackingChannels(), aInstanceGenerationParameters.Cutoff, aInstanceGenerationParameters.Seed);

		}
		finally{
			
				aInstanceGeneration.getSolver().notifyShutdown();
			
		}
		
		
		

	}

}
