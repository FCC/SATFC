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
package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.SATFCCachingParameters;
import ca.ubc.cs.beta.stationpacking.facade.SolverCustomizationOptions;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 * Created by newmanne on 13/01/15.
 */
@UsageTextField(title="Solver customization options",description="Parameters describing which optimizations to apply")
public class SolverCustomizationOptionsParameters extends AbstractOptions {


        @Parameter(names = "--presolve", description = "pre-solving", arity = 1)
        private boolean presolve = true;
        @Parameter(names = "--underconstrained", description = "underconstrained station optimizations", arity = 1)
        private boolean underconstrained = true;
        @Parameter(names = "--decomposition", description = "connected component decomposition", arity = 1)
        private boolean decomposition = true;
        @ParametersDelegate
        private SATFCCachingParameters cachingParams = new SATFCCachingParameters();

        public SolverCustomizationOptions getOptions() {
            SolverCustomizationOptions options = new SolverCustomizationOptions();
            options.setPresolve(presolve);
            options.setUnderconstrained(underconstrained);
            options.setDecompose(decomposition);
            if (cachingParams.useCache)
            {
                options.setServerURL(cachingParams.serverURL);
            }
            return options;
        }

}
