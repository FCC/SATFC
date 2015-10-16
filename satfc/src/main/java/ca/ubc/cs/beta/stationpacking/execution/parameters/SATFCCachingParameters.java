/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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

import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.Parameter;

/**
 * Created by newmanne on 04/12/14.
 */
@UsageTextField(title="SATFC Caching Parameters",description="Parameters for the SATFC problem cache.")
public class SATFCCachingParameters extends AbstractOptions {

    @Parameter(names = {"--serverURL", "-SERVER-URL"}, description = "base URL for the SATFC server", required = false)
    public String serverURL;

    @UsageTextField(claimRequired = "-SERVER-URL")
    @Parameter(names = {"-CACHE-RESULTS"}, description = "If true, problems solved are added to the cache", required = false)
    public boolean cacheResults = true;

    @UsageTextField(level = OptionLevel.DEVELOPER)
    @Parameter(names = "--extendedCacheProblem", description = "solve extended cache problem in redis queue", required = false)
    public boolean extendedCacheProblem = false;

}
