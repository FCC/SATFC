package ca.ubc.cs.beta.stationpacking.execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.options.UsageSection;
import ca.ubc.cs.beta.aclib.options.ConfigToLaTeX;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.ExecutableSolverParameters;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.SolverResult;

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
					
					
					//Save the result of the single instance execution.
					DateFormat aDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
					Date aDate = new Date();
					
					String aOutputFilename = aExecutableSolverParameter.SolverParameters.CNFDirectory+File.separatorChar+"InstanceResultArchive.csv";
					BufferedWriter aWriter = new BufferedWriter(new FileWriter(aOutputFilename,true));
					aWriter.write(aDateFormat.format(aDate)+","+aInstance+","+aResult.toParsableString()+"\n");
					aWriter.close();
					
				} 
				catch (Exception e) 
				{
					log.warn("Caught an error during execution ({}).",e.getMessage());
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
