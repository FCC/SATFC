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
import ca.ubc.cs.beta.stationpacking.solver.reporters.AsynchronousLocalExperimentReporter;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.AsyncTAESolver;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class AsyncInstanceResolvingExecutor {

	private static Logger log = LoggerFactory.getLogger(AsyncInstanceResolvingExecutor.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
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
		
		AsynchronousLocalExperimentReporter aReporter = aInstanceResolvingParameters.getExperimentReporter();
		log.info("Starting to write instances...");
		aReporter.startWritingReport();
		
		
		
		ArrayList<StationPackingInstance> aInstances = aInstanceResolvingParameters.getInstances();
			AsyncTAESolver aSolver = null;
			try
			{
				try 
				{
					aSolver = aInstanceResolvingParameters.SolverParameters.getSolver();
					
					log.info("Submitting the instances...");
					
					for(int aInstanceIndex = 0; aInstanceIndex < aInstances.size(); aInstanceIndex++)
					{
						StationPackingInstance aInstance = aInstances.get(aInstanceIndex);
						aSolver.solve(aInstance, aInstanceResolvingParameters.Cutoff, aInstanceResolvingParameters.Seed, aReporter);
						
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
//		}
		
		log.info("Done! Shutting down EVERYTHING!");
		aReporter.stopWritingReport();
		
	}
	
		
}
