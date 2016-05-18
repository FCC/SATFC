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
package ca.ubc.cs.beta.stationpacking.base;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.collect.ImmutableMap;

import lombok.Data;

public class StationPackingInstanceDeserializer extends
		JsonDeserializer<StationPackingInstance> {

	@Override
	public StationPackingInstance deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		Dummy readValueAs = p.readValueAs(Dummy.class);
        if (readValueAs.getMetadata() != null) {
            return new StationPackingInstance(readValueAs.getDomains(), readValueAs.getPreviousAssignment(), readValueAs.getMetadata());
        } else {
            return new StationPackingInstance(readValueAs.getDomains(), readValueAs.getPreviousAssignment());
        }
	}
	
	@Data
	public static class Dummy {
		private ImmutableMap<Station, Set<Integer>> domains;
		private ImmutableMap<Station, Integer> previousAssignment;
		private Map<String, Object> metadata;
	}

}
