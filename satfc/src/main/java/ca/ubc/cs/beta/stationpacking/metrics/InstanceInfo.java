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
package ca.ubc.cs.beta.stationpacking.metrics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Data;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by newmanne on 21/01/15.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class InstanceInfo {

    private int numStations;
    private Set<Station> stations;
    private String name;
    private Double runtime;
    private SATResult result;
    private Set<Integer> underconstrainedStations = new HashSet<>();
    private Map<String, InstanceInfo> components = new HashMap<>();
    private String solvedBy;
    private Map<String, Double> timingInfo = new HashMap<>();
    private String cacheResultUsed;
    private Map<Station, Integer> stationToDegree = new HashMap<>();

}
