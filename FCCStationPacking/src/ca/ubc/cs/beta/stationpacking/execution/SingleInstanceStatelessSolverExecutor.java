package ca.ubc.cs.beta.stationpacking.execution;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.options.UsageSection;
import ca.ubc.cs.beta.aclib.options.ConfigToLaTeX;
import ca.ubc.cs.beta.stationpacking.datastructures.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.ExecutableSolverParameters;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class SingleInstanceStatelessSolverExecutor {

	private static Logger log = LoggerFactory.getLogger(SingleInstanceStatelessSolverExecutor.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		ISolver aSolver = null;
		try
		{
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
			
			
				try 
				{
					aSolver = aExecutableSolverParameter.getSolver();
					
					StationPackingInstance aInstance = aExecutableSolverParameter.getInstance();
					
					SolverResult aResult = aSolver.solve(aInstance, aExecutableSolverParameter.ProblemInstanceParameters.Cutoff, aExecutableSolverParameter.ProblemInstanceParameters.Seed);
			
					System.out.println("Result for feasibility checker: "+aResult.toParsableString());
	
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
		}
		finally{
			if(aSolver!=null)
			{
				aSolver.notifyShutdown();
			}
		}
		
		
		
		

	}

}
