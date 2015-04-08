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
package ca.ubc.cs.beta.stationpacking.execution.parameters;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.base.QuestionInstanceParameters;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 * Parameters for the stand-alone SATFC executable.
 * @author afrechet
 */
@UsageTextField(title="SATFC Executable Parameters",description="Parameters needed to execute SATFC on a single instance.")
public class SATFCExecutableParameters extends AbstractOptions {
	
    /**
     * SATFC solver parameters.
     */
	@ParametersDelegate
	public SATFCParameters SATFCParameters = new SATFCParameters();
	
	/**
	 * Question defining station packing instance to solve.
	 */
	@ParametersDelegate
	public QuestionInstanceParameters QuestionParameters = new QuestionInstanceParameters();
	
	/**
	 * Working directory.
	 */
	@Parameter(names = "-WORKDIR", description = "Working directory (especially where to find problem data).")
	public String WorkDirectory = "";
	
}
