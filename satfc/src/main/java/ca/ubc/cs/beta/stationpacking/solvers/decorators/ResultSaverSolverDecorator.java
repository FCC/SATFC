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
package ca.ubc.cs.beta.stationpacking.solvers.decorators;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import net.jcip.annotations.NotThreadSafe;

import org.apache.commons.io.FileUtils;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * Solver decorator that saves solve results post-execution.
 *
 * @author afrechet
 */
@NotThreadSafe
public class ResultSaverSolverDecorator extends ASolverDecorator {

    private final File fResultFile;

    public ResultSaverSolverDecorator(ISolver aSolver, String aResultFile) {
        super(aSolver);

        if (aResultFile == null) {
            throw new IllegalArgumentException("Result file cannot be null.");
        }

        File resultFile = new File(aResultFile);

        if (resultFile.exists()) {
            throw new IllegalArgumentException("Result file " + aResultFile + " already exists.");
        }

        fResultFile = resultFile;
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed) {
        SolverResult result = fDecoratedSolver.solve(aInstance, aTerminationCriterion, aSeed);

        String instanceName = aInstance.getHashString() + ".cnf";

        String line = instanceName + "," + result.toParsableString();
        try {
            FileUtils.writeLines(
                    fResultFile,
                    Arrays.asList(line),
                    true);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not write result to file " + fResultFile + ".");
        }

        return result;
    }

}
