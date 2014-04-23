package ca.ubc.cs.beta.stationpacking.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
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
		SATFCFacade.initializeLogging(parameters.fLoggingOptions.logLevel);
		Logger log = LoggerFactory.getLogger(SATFCFacadeExecutor.class);
		
		
		System.out.println(System.getProperties());
		log.info("Initializing facade.");
		try(SATFCFacade satfc = new SATFCFacade(parameters.fClaspLibrary);)
		{
			log.info("Solving ...");
			SATFCResult result = satfc.solve(
					parameters.fInstanceParameters.getDomains(),
					parameters.fInstanceParameters.getPreviousAssignment(),
					parameters.fInstanceParameters.Cutoff,
					parameters.fInstanceParameters.Seed,
					parameters.fDataFoldername);
			
			log.info("..done!");
			
			System.out.println(result.getResult());
			System.out.println(result.getRuntime());
			System.out.println(result.getWitnessAssignment());

		}

	}

}
