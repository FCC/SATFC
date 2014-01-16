package ca.ubc.cs.beta.stationpacking.execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.ExecutableSolverParameters;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.CPUTimeTerminationCriterion;

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
			
			//Check for help
			JCommander aParameterParser = JCommanderHelper.parseCheckingForHelpAndVersion(args, aExecutableSolverParameter,TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators());
			
			try
			{
				aParameterParser.parse(args);
			}
			catch (ParameterException aParameterException)
			{
				throw aParameterException;
			}
			
			
			aSolver = aExecutableSolverParameter.getSolver();
			
			StationPackingInstance aInstance = aExecutableSolverParameter.getInstance();
			
			SolverResult aResult = aSolver.solve(aInstance, new CPUTimeTerminationCriterion(aExecutableSolverParameter.ProblemInstanceParameters.Cutoff), aExecutableSolverParameter.ProblemInstanceParameters.Seed);
	
			System.out.println("Result for feasibility checker: "+aResult.toParsableString());
			
			//Save the result of the single instance execution.
			try
			{
				String aOutputFilename = System.getProperty("user.dir")+File.separator+"InstanceResultArchive.csv";
				BufferedWriter aWriter = new BufferedWriter(new FileWriter(aOutputFilename,true));
				
				DateFormat aDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				Date aDate = new Date();
				aWriter.write(aDateFormat.format(aDate)+","+aInstance+","+aResult.toParsableString()+"\n");
				
				aWriter.close();
			}
			catch(IOException e)
			{
				log.error("There was an error while trying to save the execution result to file ({}).",e.getMessage());
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
