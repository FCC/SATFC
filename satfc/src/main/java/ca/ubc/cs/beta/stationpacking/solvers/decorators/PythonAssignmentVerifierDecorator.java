/**
 * Copyright 2016, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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

import java.util.List;

import org.python.core.PyObject;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.factories.PythonInterpreterContainer;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by emily404 on 8/31/15.
 * Verifies that SAT results returned by solvers don't violate the constraints
 * This verifier is actually written in python, by someone who has not seen any other constraint parsing code, just to be doubly sure of correctness.
 */
@Slf4j
public class PythonAssignmentVerifierDecorator extends ASolverDecorator {

    private final PythonInterpreterContainer python;

    /**
     * @param aSolver - decorated ISolver, verifying assignemnt in python.
     */
    public PythonAssignmentVerifierDecorator(ISolver aSolver, PythonInterpreterContainer pythonInterpreter) {
        super(aSolver);
        python = pythonInterpreter;
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed) {
        final SolverResult result = fDecoratedSolver.solve(aInstance, aTerminationCriterion, aSeed);
        if (result.getResult().equals(SATResult.SAT)) {
            log.debug("Independently verifying the veracity of returned assignment using python verifier script");

            final String evalString = "check_violations(\'"+JSONUtils.toString(result.getAssignment())+"\')";
            final PyObject eval = python.eval(evalString);
            final String checkResult = eval.toString();
            if(!checkResult.equals("None")){
                List<String> violationResult = (List<String>) eval.__tojava__(List.class);
                throw new IllegalStateException("Station " + violationResult.get(0) + " is assigned to channel " + violationResult.get(1) + " which violated " + violationResult.get(2) + System.lineSeparator() + result.getAssignment());
            };
            log.debug("Assignment was independently verified to be satisfiable.");
        }
        return result;
    }

}
