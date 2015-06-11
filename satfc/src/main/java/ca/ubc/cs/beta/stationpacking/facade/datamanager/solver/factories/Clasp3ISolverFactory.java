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
package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.factories;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ClaspLibraryGenerator;
import ca.ubc.cs.beta.stationpacking.solvers.sat.CompressedSATBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.Clasp3SATSolver;
import lombok.RequiredArgsConstructor;

/**
* Created by newmanne on 03/06/15.
*/
@RequiredArgsConstructor
public class Clasp3ISolverFactory {

    private final ClaspLibraryGenerator claspLibraryGenerator;
    private final SATCompressor satCompressor;
    private final IConstraintManager constraintManager;

    public CompressedSATBasedSolver create(String aConfig) {
        return create(aConfig, 0);
    }

    public CompressedSATBasedSolver create(String aConfig, int seedOffset) {
        final AbstractCompressedSATSolver claspSATsolver = new Clasp3SATSolver(claspLibraryGenerator.createClaspLibrary(), aConfig, seedOffset);
        return new CompressedSATBasedSolver(claspSATsolver, satCompressor, constraintManager);
    }

}
