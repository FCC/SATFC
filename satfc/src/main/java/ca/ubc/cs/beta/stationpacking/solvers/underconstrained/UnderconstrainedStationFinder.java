/**
 * Copyright 2015, Auctionomics, Alexandre FrÃ©chette, Neil Newman, Kevin Leyton-Brown.
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
package ca.ubc.cs.beta.stationpacking.solvers.underconstrained;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;

import com.google.common.collect.Sets;

/**
 * Created by newmanne on 1/8/15.
 */
@Slf4j
public class UnderconstrainedStationFinder implements IUnderconstrainedStationFinder {

    private final IConstraintManager fConstraintManager;

    public UnderconstrainedStationFinder(IConstraintManager aConstraintManger) {
        fConstraintManager = aConstraintManger;
    }


    @Override
    public Set<Station> getUnderconstrainedStations(Map<Station, Set<Integer>> domains) {
        //Find  the under constrained nodes in the instance.
        final Set<Station> underconstrainedStations = new HashSet<Station>();

        log.debug("Finding underconstrained stations in the instance...");

        final Map<Station,Set<Integer>> badChannels = new HashMap<Station,Set<Integer>>();
        for(final Entry<Station, Set<Integer>> domainEntry : domains.entrySet())
        {
            final Station station = domainEntry.getKey();
            final Set<Integer> domain = domainEntry.getValue();

            for(Integer domainChannel : domain)
            {
                for(Station coNeighbour : fConstraintManager.getCOInterferingStations(station,domainChannel))
                {
                    if(domains.keySet().contains(coNeighbour) && domains.get(coNeighbour).contains(domainChannel))
                    {
                        Set<Integer> stationBadChannels = badChannels.get(station);
                        if(stationBadChannels == null)
                        {
                            stationBadChannels = new HashSet<Integer>();
                        }
                        stationBadChannels.add(domainChannel);
                        badChannels.put(station, stationBadChannels);

                        Set<Integer> coneighbourBadChannels = badChannels.get(coNeighbour);
                        if(coneighbourBadChannels == null)
                        {
                            coneighbourBadChannels = new HashSet<Integer>();
                        }
                        coneighbourBadChannels.add(domainChannel);
                        badChannels.put(coNeighbour, coneighbourBadChannels);
                    }
                }
                for(Station adjNeighbour : fConstraintManager.getADJplusInterferingStations(station, domainChannel))
                {
                    if(domains.keySet().contains(adjNeighbour) && domains.get(adjNeighbour).contains(domainChannel+1))
                    {
                        Set<Integer> stationBadChannels = badChannels.get(station);
                        if(stationBadChannels == null)
                        {
                            stationBadChannels = new HashSet<Integer>();
                        }
                        stationBadChannels.add(domainChannel);
                        badChannels.put(station, stationBadChannels);

                        Set<Integer> adjneighbourBadChannels = badChannels.get(adjNeighbour);
                        if(adjneighbourBadChannels == null)
                        {
                            adjneighbourBadChannels = new HashSet<Integer>();
                        }
                        adjneighbourBadChannels.add(domainChannel+1);
                        badChannels.put(adjNeighbour, adjneighbourBadChannels);
                    }
                }
            }
        }

        for(final Entry<Station, Set<Integer>> domainEntry : domains.entrySet())
        {
            final Station station = domainEntry.getKey();
            final Set<Integer> domain = domainEntry.getValue();

            Set<Integer> stationBadChannels = badChannels.get(station);
            if(stationBadChannels == null)
            {
                stationBadChannels = Collections.emptySet();
            }

            final Set<Integer> stationGoodChannels = Sets.difference(domain, stationBadChannels);

            log.trace("Station {} domain channels: {}.",station,domain);
            log.trace("Station {} bad channels: {}.",station,stationBadChannels);

            if(!stationGoodChannels.isEmpty())
            {
                log.trace("Station {} is underconstrained has it has {} domain channels ({}) on which it interferes with no one.",station,stationGoodChannels.size(),stationGoodChannels);
                underconstrainedStations.add(station);
            }
        }

        return underconstrainedStations;

    }

}