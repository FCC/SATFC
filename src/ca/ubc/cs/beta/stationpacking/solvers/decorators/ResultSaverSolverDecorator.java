package ca.ubc.cs.beta.stationpacking.solvers.decorators;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import net.jcip.annotations.NotThreadSafe;

import org.apache.commons.io.FileUtils;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * Solver decorator that saves solve results post-execution. 
 * @author afrechet
 */
@NotThreadSafe
public class ResultSaverSolverDecorator extends ASolverDecorator {
	
	private final File fResultFile;
	
	public ResultSaverSolverDecorator(ISolver aSolver,String aResultFile) {
		super(aSolver);
		
		if(aResultFile == null)
		{
			throw new IllegalArgumentException("Result file cannot be null.");
		}
		
		File resultFile = new File(aResultFile);
		
		if(resultFile.exists())
		{
			throw new IllegalArgumentException("Result file "+aResultFile+" already exists.");
		}
		
		fResultFile = resultFile;
	}
	
	@Override
	public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed)
	{
		SolverResult result = fDecoratedSolver.solve(aInstance, aTerminationCriterion, aSeed);
		
		String instanceName = aInstance.getHashString()+".cnf";
		
		String line = instanceName+","+result.toParsableString();
		try {
			FileUtils.writeLines(
					fResultFile,
					Arrays.asList(line),
					true);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException("Could not write result to file "+fResultFile+".");
		}
		
		return result;
	}

}
