/**
 * Copyright 2014, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
 *
 * This file is part of SATFC.
 *
 * SATFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SATFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.execution.parameters;

import ca.ubc.cs.beta.aeatk.logging.ComplexLoggingOptions;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.base.InstanceParameters;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 * SATFC facade parameters.
 * @author afrechet
 */
@UsageTextField(title="SATFC Facade Parameters",description="Parameters needed to execute SATFC facade on a single instance.")
public class SATFCFacadeParameters extends AbstractOptions {
    
    /**
     * Parameters for the instance to solve.
     */
	@ParametersDelegate
	public InstanceParameters fInstanceParameters = new InstanceParameters();
	
	/**
	 * Clasp library to use (optional - can be automatically detected).
	 */
	@Parameter(names = "-CLASP-LIBRARY",description = "clasp library file")
	public String fClaspLibrary;
	
	/**
	 * Logging options.
	 */
	@ParametersDelegate
	public ComplexLoggingOptions fLoggingOptions = new ComplexLoggingOptions();
	
}
