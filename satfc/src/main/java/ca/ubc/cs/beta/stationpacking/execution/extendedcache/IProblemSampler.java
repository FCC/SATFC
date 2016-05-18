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
package ca.ubc.cs.beta.stationpacking.execution.extendedcache;

import java.util.List;

/**
 * Created by emily404 on 6/4/15.
 */
public interface IProblemSampler {

    /**
     * Determine a list of new problems to extend based on a sampling method
     * {@link ca.ubc.cs.beta.stationpacking.execution.extendedcache.ProblemSamplerParameters.ProblemSamplingMethod}
     * @param counter put counter number of new problems to keyQueue
     * @return keys of cache entries to be added to keyQueue
     */
    List<String> sample(int counter);

    /**
     * Determine a new problems to extend based on a sampling method
     * {@link ca.ubc.cs.beta.stationpacking.execution.extendedcache.ProblemSamplerParameters.ProblemSamplingMethod}
     * @return key of cache entry to be added to keyQueue
     */
    String sample();
}
