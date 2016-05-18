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
package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.factories;

import ca.ubc.cs.beta.stationpacking.solvers.sat.CompressedSATBasedSolver;

/**
 * Created by newmanne on 10/10/15.
 */
public interface ISATSolverFactory {

    /**
     * @param seedOffset SATFC as a whole takes a given seed. This says how much to offset this particular instance against that seed.
     *                   This allows us to run the same SAT solver with different seeds even though SATFC only takes one seed
     */
    CompressedSATBasedSolver create(String params, int seedOffset);

    default CompressedSATBasedSolver create(String params) {
        return create(params, 0);
    };

}
