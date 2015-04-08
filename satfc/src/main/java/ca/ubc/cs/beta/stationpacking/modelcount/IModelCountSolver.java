/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
 *
 * This file is part of satfc.
 *
 * satfc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * satfc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with satfc.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.modelcount;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;

/**
 * A model count estimator.
 * 
 * @author tqichen
 */
public interface IModelCountSolver {
    
    /**
     * @param aInstance - the instance to be counted.
     * @param aSeed - the execution seed.
     * @return a count (or estimate) of the total number of SAT results involving any
     * <i>station subset</i> of the given instance, excluding the empty subset.
     */
    public Double countSatisfiablePackings(StationPackingInstance aInstance, long aSeed);
    
    /**
     * Produces 
     * @param aInstance
     * @param aStation
     * @param aSeed
     * @return a count (or estimate) of the total number of SAT results involving station subsets
     * which contain the target station of the given instance.
     */
    public Double countSatisfiablePackingsContainingStation(StationPackingInstance aInstance, Station aStation, long aSeed);
    
    /**
     * Tries to stop the solve call if implemented, if not throws an UnsupportedOperationException.
     * @throws UnsupportedOperationException - throws if interruption is unavailable.
     */
    public void interrupt() throws UnsupportedOperationException;
    
    /**
     * Ask the solver to shutdown.
     */
    public void notifyShutdown();

}