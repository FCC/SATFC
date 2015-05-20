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
package ca.ubc.cs.beta.stationpacking.utils;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by newmanne on 19/03/15.
 */
public class CacheUtils {

    private static RestTemplate restTemplate;

    public static BitSet toBitSet(StationPackingInstance aInstance) {
        final BitSet bitSet = new BitSet();
        aInstance.getStations().forEach(station -> bitSet.set(station.getID()));
        return bitSet;
    }

    public static BitSet toBitSet(Map<Integer, Set<Station>> answer) {
        final BitSet bitSet = new BitSet();
        answer.values().stream().forEach(stations -> stations.forEach(station -> bitSet.set(station.getID())));
        return bitSet;
    }

    public static Map<Station, Integer> stationToChannelFromChannelToStation(Map<Integer, Set<Station>> channelToStation) {
        final Map<Station, Integer> stationToChannel = new HashMap<>();
        channelToStation.entrySet().forEach(entry -> {
            entry.getValue().forEach(station -> {
                stationToChannel.put(station, entry.getKey());
            });
        });
        return stationToChannel;
    }

    public static RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
            final MappingJackson2HttpMessageConverter mappingJacksonHttpMessageConverter = new MappingJackson2HttpMessageConverter();
            final ObjectMapper mapper = JSONUtils.getMapper();
            mappingJacksonHttpMessageConverter.setObjectMapper(mapper);
            // swap out the default message converter
            for (int i = 0; i < restTemplate.getMessageConverters().size(); i++) {
                if (restTemplate.getMessageConverters().get(i) instanceof MappingJackson2HttpMessageConverter) {
                    restTemplate.getMessageConverters().remove(i);
                    restTemplate.getMessageConverters().add(i, mappingJacksonHttpMessageConverter);
                    break;
                }
            }
        }
        return restTemplate;
    }

}
