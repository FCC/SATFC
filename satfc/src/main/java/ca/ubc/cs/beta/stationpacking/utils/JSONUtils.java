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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

import lombok.Getter;

/**
 * Created by newmanne on 02/12/14.
 */
public class JSONUtils {

    @Getter
    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.registerModule(new GuavaModule());
        mapper.registerModule(new SATFCJacksonModule());
    }

    public static <T> T toObject(String jsonString, Class<T> klazz) {
        try {
            return toObjectWithException(jsonString, klazz);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't deserialize string " + jsonString + " into type " + klazz, e);
        }
    }

    public static <T> T toObjectWithException(String jsonString, Class<T> klazz) throws IOException {
        return mapper.readValue(jsonString, klazz);
    }

    public static String toString(Object object) {
    	return toString(object, false);
    }

    public static String toString(Object object, boolean pretty) {
        try {
        	final String json;
        	if (pretty) {
        		json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        	} else {
        		json = mapper.writeValueAsString(object);
        	}
            return json;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Couldn't serialize object " + object, e);
        }
    }

}