package ca.ubc.cs.beta.stationpacking.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.misc.returnvalues.AEATKReturnValues;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.execution.parameters.SATFCFacadeParameters;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacade;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeBuilder;
import ca.ubc.cs.beta.stationpacking.facade.SATFCResult;

import com.beust.jcommander.ParameterException;

public class SATFCFacadeExecutor {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//Parse the command line arguments in a parameter object.
		Logger log = null ;
		try 
		{
			SATFCFacadeParameters parameters = new SATFCFacadeParameters();
			try 
			{
				//Check for help
				JCommanderHelper.parseCheckingForHelpAndVersion(args, parameters,TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators());
				SATFCFacade.initializeLogging(parameters.fLoggingOptions.logLevel);
			}
			finally
			{
				log = LoggerFactory.getLogger(SATFCFacadeExecutor.class);
			}
			
			log.info("Initializing facade.");
			SATFCFacadeBuilder satfcBuilder = new SATFCFacadeBuilder();
			
			String library = parameters.fClaspLibrary;
			if(library != null)
			{
				satfcBuilder.setLibrary(parameters.fClaspLibrary);
			}
			satfcBuilder.setInitializeLogging(true);
			
			try(SATFCFacade satfc = satfcBuilder.build())
			{
				log.info("Solving ...");
				SATFCResult result = satfc.solve(
						parameters.fInstanceParameters.getPackingStationIDs(),
						parameters.fInstanceParameters.getPackingChannels(),
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
		} catch (ParameterException e)
		{
			log.error("Invalid parameter argument detected ({}).",e.getMessage());
			System.exit(AEATKReturnValues.PARAMETER_EXCEPTION);
		} catch (RuntimeException e)
		{
			log.error("Runtime exception encountered ({})",e.getMessage());
			System.exit(AEATKReturnValues.UNCAUGHT_EXCEPTION);
		}catch(UnsatisfiedLinkError e)
		{
			log.error("Couldn't initialize facade, see previous log messages and/or try logging with DEBUG.");
			System.exit(AEATKReturnValues.UNCAUGHT_EXCEPTION);
		}catch(Throwable t)
		{
			log.error("Throwable encountered ({})",t.getMessage());
			System.exit(AEATKReturnValues.UNCAUGHT_EXCEPTION);
		}
	}

}
