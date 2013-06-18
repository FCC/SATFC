package ca.ubc.cs.beta.stationpacking.execution.parameters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class ImplementedSolverParameterValidator implements IParameterValidator {
	private Set<String> fImplementedSolvers = new HashSet<String>(Arrays.asList("picosat","clasp","glucose","plingeling","tunedclasp","glueminisat-incremental"));
	
	@Override
	public void validate(String name, String value)
			throws ParameterException {
		if(name.equals("-SOLVER"))
		{
			if(!fImplementedSolvers.contains(value))
			{
				throw new ParameterException("Provided SOLVER parameter "+value+" is not an implemented solver.");
			}
		}
		else
		{
			throw new ParameterException("ImplementedSolver verifier is only valid for the SOLVER parameter.");
		}	
	}
}
