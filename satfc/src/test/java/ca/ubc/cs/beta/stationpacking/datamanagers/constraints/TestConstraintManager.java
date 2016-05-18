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
package ca.ubc.cs.beta.stationpacking.datamanagers.constraints;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

/**
* Created by newmanne on 08/07/15.
*/
public class TestConstraintManager extends AMapBasedConstraintManager {

	public TestConstraintManager(List<TestConstraint> constraints) throws FileNotFoundException {
        super(null, "");
        constraints.stream().forEach(c -> {
            final Map<Station, Map<Integer, Set<Station>>> map = c.getKey().equals(ConstraintKey.CO) ? fCOConstraints : c.getKey().equals(ConstraintKey.ADJp1) ? fADJp1Constraints : fADJp2Constraints;
            map.putIfAbsent(c.getReference(), new HashMap<>());
            map.get(c.getReference()).putIfAbsent(c.getChannel(), new HashSet<>());
            map.get(c.getReference()).get(c.getChannel()).addAll(c.getInterfering());
        });
    }

	@Override
	protected void loadConstraints(IStationManager stationManager, String fileName) throws FileNotFoundException {
	}

}
