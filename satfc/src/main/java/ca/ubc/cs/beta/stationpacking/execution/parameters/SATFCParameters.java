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

import ca.ubc.cs.beta.aeatk.logging.ComplexLoggingOptions;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.SATFCSolverManagerParameters;

import com.beust.jcommander.ParametersDelegate;

/**
 * Parameters to build SATFC.
 * @author afrechet
 */
@UsageTextField(title="SATFC Parameters",description="Parameters needed to build SATFC.")
public class SATFCParameters extends AbstractOptions {
    
    /**
     * Parameters needed to build SATFC solver manager.
     */
	@ParametersDelegate
	public SATFCSolverManagerParameters SolverManagerParameters = new SATFCSolverManagerParameters();
	
	/**
	 * Logging options.
	 */
	@ParametersDelegate
	public ComplexLoggingOptions LoggingOptions = new ComplexLoggingOptions();
}
