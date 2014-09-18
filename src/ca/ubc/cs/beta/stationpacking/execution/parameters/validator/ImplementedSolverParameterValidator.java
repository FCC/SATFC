package ca.ubc.cs.beta.stationpacking.execution.parameters.validator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * Validator for the type of SAT solvers that are currently implemented.
 * @author afrechet
 */
public class ImplementedSolverParameterValidator implements IParameterValidator {
	private Set<String> fImplementedSolvers = new HashSet<String>(Arrays.asList("picosat","clasp","plingeling","tunedclasp","glueminisat"));
	
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
