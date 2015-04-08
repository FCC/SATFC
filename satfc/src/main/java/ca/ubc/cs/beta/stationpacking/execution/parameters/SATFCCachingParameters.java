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

import lombok.Getter;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.Parameter;

/**
 * Created by newmanne on 04/12/14.
 */
@UsageTextField(title="SATFC Caching Parameters",description="Parameters for the SATFC problem cache.")
public class SATFCCachingParameters extends AbstractOptions {

    @Parameter(names = "--useCache", description = "Should the cache be used", required = false, arity = 0)
    public boolean useCache = false;

    @Parameter(names = "--serverURL", description = "base URL for the SATFC server", required = false)
    @Getter
    public String serverURL = "http://localhost:8080/satfcserver";

}
