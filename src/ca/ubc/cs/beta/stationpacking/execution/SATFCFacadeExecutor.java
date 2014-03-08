package ca.ubc.cs.beta.stationpacking.execution;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.execution.parameters.SATFCFacadeParameters;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacade;
import ca.ubc.cs.beta.stationpacking.facade.SATFCResult;

import com.beust.jcommander.ParameterException;

public class SATFCFacadeExecutor {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//Parse the command line arguments in a parameter object.
		SATFCFacadeParameters parameters = new SATFCFacadeParameters();
		//Check for help
		try
		{
			JCommanderHelper.parseCheckingForHelpAndVersion(args, parameters,TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators());
		}
		catch (ParameterException aParameterException)
		{
			throw aParameterException;
		}
		parameters.fLoggingOptions.initializeLogging();
		Logger log = LoggerFactory.getLogger(SATFCExecutor.class);
		
		
		log.info("Initializing facade.");
		SATFCFacade satfc = new SATFCFacade(parameters.fClaspLibrary);
		
		log.info("Solving instance...");
		SATFCResult result = satfc.solve(
				parameters.fInstanceParameters.getPackingStationIDs(),
				parameters.fInstanceParameters.getPackingChannels(),
				new HashMap<Integer,Integer>(),
				parameters.fInstanceParameters.Cutoff,
				parameters.fInstanceParameters.Seed,
				parameters.fDataFoldername);
		
		log.info("..done!");
		
		System.out.println(result.getResult());
		System.out.println(result.getRuntime());
		System.out.println(result.getWitnessAssignment());

	}

}
