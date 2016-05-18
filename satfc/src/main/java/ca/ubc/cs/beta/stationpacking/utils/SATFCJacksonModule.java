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
package ca.ubc.cs.beta.stationpacking.utils;

import com.fasterxml.jackson.databind.module.SimpleModule;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationDeserializer;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.YAMLBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.yaml.ISolverConfig;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.yaml.IStationAddingStrategyConfig;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.yaml.IStationPackingConfigurationStrategyConfig;

/**
* Created by newmanne on 05/10/15.
 * A jackson module for all SATFC specific classes with custom serializer and deserializers
*/
public class SATFCJacksonModule extends SimpleModule {
    public SATFCJacksonModule() {
        addKeyDeserializer(Station.class, new StationDeserializer.StationClassKeyDeserializer());
        addDeserializer(ISolverConfig.class, new YAMLBundle.SolverConfigDeserializer());
        addDeserializer(IStationAddingStrategyConfig.class, new YAMLBundle.StationAddingStrategyConfigurationDeserializer());
        addDeserializer(IStationPackingConfigurationStrategyConfig.class, new YAMLBundle.PresolverConfigurationDeserializer());
    }
}
