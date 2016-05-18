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
package ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder;

import org.apache.commons.math3.util.Pair;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.yaml.EncodingType;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.base.CompressionBijection;

/**
 * Encodes a problem instance as a propositional satisfiability problem.
 * Insures that the SAT variables are contiguous from 1 to n.
 * A variable of the SAT encoding is a station channel pair, each constraint is trivially
 * encoded as a clause (this station cannot be on this channel when this other station is on this other channel is a two clause with the previous
 * SAT variables), and base clauses are added (each station much be on exactly one channel).
 *
 * @author afrechet
 */
public class SATCompressor implements ISATEncoder {

    private final IConstraintManager fConstraintManager;
    private EncodingType encodingType;

    public SATCompressor(IConstraintManager aConstraintManager, EncodingType encodingType) {
        fConstraintManager = aConstraintManager;
        this.encodingType = encodingType;
    }

    @Override
    public Pair<CNF, ISATDecoder> encode(StationPackingInstance aInstance) {
        SATEncoder aSATEncoder = new SATEncoder(fConstraintManager, new CompressionBijection<>(), encodingType);
        return aSATEncoder.encode(aInstance);
    }

    @Override
    public SATEncoder.CNFEncodedProblem encodeWithAssignment(StationPackingInstance aInstance) {
        SATEncoder aSATEncoder = new SATEncoder(fConstraintManager, new CompressionBijection<>(), encodingType);
        return aSATEncoder.encodeWithAssignment(aInstance);

    }
}
