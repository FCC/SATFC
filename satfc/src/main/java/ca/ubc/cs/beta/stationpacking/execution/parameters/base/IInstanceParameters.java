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
package ca.ubc.cs.beta.stationpacking.execution.parameters.base;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

/**
 * Interface for station packing instance building parameters.
 * @author afrechet
 */
public interface IInstanceParameters {
    
    /**
     * @return the interference data foldername.
     */
    public String getInterferenceData();
    
    /**
     * @return the instance cutoff time.
     */
    public double getCutoff();
    
    /**
     * @param aStationManager - a station manager to select instance stations from.
     * @return station packing instance represented by parameters.
     */
    public StationPackingInstance getInstance(IStationManager aStationManager);
}
