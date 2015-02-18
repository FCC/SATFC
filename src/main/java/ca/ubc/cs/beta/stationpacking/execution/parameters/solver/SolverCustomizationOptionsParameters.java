package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeParameter;

import com.beust.jcommander.Parameter;

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

        public SATFCFacadeParameter.SolverCustomizationOptions getOptions() {
            SATFCFacadeParameter.SolverCustomizationOptions options = new SATFCFacadeParameter.SolverCustomizationOptions();
            options.setPresolve(presolve);
            options.setUnderconstrained(underconstrained);
            options.setDecompose(decomposition);
          
            return options;
        }

}
