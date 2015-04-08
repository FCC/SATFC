/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
 *
 * This file is part of satfc.
 *
 * satfc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * satfc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with satfc.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
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
