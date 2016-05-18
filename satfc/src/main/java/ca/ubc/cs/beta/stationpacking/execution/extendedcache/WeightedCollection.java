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

import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/**
 * Created by emily404 on 6/15/15.
 */

/**
 * Collection with random and weighted selector
 * @param <E> class of the collection
 */
public class WeightedCollection<E> {

    private final NavigableMap<Double, E> map = new TreeMap<>();
    private final Random random;
    private double total = 0;

    public WeightedCollection() {
        this(new Random());
    }

    public WeightedCollection(Random random) {
        this.random = random;
    }

    /**
     * Accumulate total weight
     * Map current total weight to current object
     * @param weight weight of current object
     * @param object object to be added to map
     */
    public void add(double weight, E object) {
        if (weight <= 0) return;
        total += weight;
        map.put(total, object);
    }

    /**
     * Random: generate a random number range [0, total] inclusisve
     * Weighted: object contributed higher weight in method, add(double weight, E object), has a higher chance of being selected
     * @return weighted random selection object
     */
    public E next() {
        double value = random.nextDouble() * total;
        return map.ceilingEntry(value).getValue();
    }
}
