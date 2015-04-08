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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Created by newmanne on 06/03/15.
 */
public class StationDeserializer extends JsonDeserializer<Station> {

    @Override
    public Station deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        return new Station(p.readValueAs(Integer.class));
    }

    public static class StationJacksonModule extends SimpleModule {
        public StationJacksonModule() {
            addKeyDeserializer(Station.class, new StationClassKeyDeserializer());
        }
    }

    public static class StationClassKeyDeserializer extends KeyDeserializer {
        @Override
        public Object deserializeKey(final String key,
                                     final DeserializationContext ctxt) throws IOException {
            return new Station(Integer.parseInt(key));
        }
    }


}
