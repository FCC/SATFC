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
package ca.ubc.cs.beta.stationpacking.base;

import junit.framework.TestCase;

import org.junit.Test;

import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class StationPackingInstanceTest extends TestCase {

    @Test
    public void testSerialization() {
        final StationPackingInstance instance = new StationPackingInstance(ImmutableMap.of(new Station(3), ImmutableSet.of(3, 4, 5)), ImmutableMap.of(new Station(3), 3), ImmutableMap.of(StationPackingInstance.NAME_KEY, "SAMPLE"));
        assertEquals("{\"domains\":{\"3\":[3,4,5]},\"previousAssignment\":{\"3\":3},\"metadata\":{\"NAME\":\"SAMPLE\"}}", JSONUtils.toString(instance));
    }

    @Test
    public void testDeserialization() {
        final StationPackingInstance instance = JSONUtils.toObject("{\"domains\":{\"3\":[3,4,5]},\"previousAssignment\":{\"3\":3}}", StationPackingInstance.class);
        System.out.println(instance.toString());
    }

}