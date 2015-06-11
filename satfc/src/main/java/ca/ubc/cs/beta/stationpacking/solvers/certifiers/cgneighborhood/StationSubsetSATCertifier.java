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
package ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.metrics.SATFCMetrics;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterionFactory;
import ca.ubc.cs.beta.stationpacking.solvers.termination.composite.DisjunctiveCompositeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

import com.google.common.collect.Sets;

/**
 * SAT certifier. Pre-solver that checks whether the missing stations plus their neighborhood are packable when all other stations are fixed
 * to their previous assignment values.
 * @author afrechet
 */
@Slf4j
public class StationSubsetSATCertifier implements IStationSubsetCertifier {

    public static final String STATION_SUBSET_SATCERTIFIER = "_StationSubsetSATCertifier";
    private final ISolver fSolver;
	private final ITerminationCriterionFactory fTerminationCriterionFactory;
	
	public StationSubsetSATCertifier(ISolver aSolver, ITerminationCriterionFactory aTerminationCriterionFactory)
	{
		fSolver = aSolver;
		fTerminationCriterionFactory = aTerminationCriterionFactory;
	}

	@Override
	public SolverResult certify(StationPackingInstance aInstance,
			Set<Station> aToPackStations,
			ITerminationCriterion aTerminationCriterion, long aSeed) {
		
		Watch watch = Watch.constructAutoStartWatch();
		
		ITerminationCriterion terminationCriterion = new DisjunctiveCompositeTerminationCriterion(Arrays.asList(fTerminationCriterionFactory.getTerminationCriterion(),aTerminationCriterion));
		
		Map<Station,Integer> previousAssignment = aInstance.getPreviousAssignment();
		
		/*
		 * Try the SAT bound, namely to see if the missing stations plus their neighborhood are packable when all other stations are fixed
		 * to their previous assignment values.
		 */
		//Change station packing instance so that the 'not to pack' stations have reduced domain.
		Map<Station,Set<Integer>> domains = aInstance.getDomains();
		Map<Station,Set<Integer>> reducedDomains = new HashMap<Station,Set<Integer>>();
		
		
		for(Station station : aInstance.getStations())
		{
			Set<Integer> domain = domains.get(station);
			
			if(!aToPackStations.contains(station))
			{
				Integer previousChannel = previousAssignment.get(station);
				if(!domain.contains(previousChannel))
				{
					//One empty domain station, cannot affirm anything.
					log.warn("Station {} in previous assignment is assigned to channel {} not in current domain {} - SAT certifier is indecisive.",station,previousChannel,domain);
					watch.stop();
					double extraTime = watch.getElapsedTime();
					return new SolverResult(SATResult.TIMEOUT, extraTime);
				}
				reducedDomains.put(station,Sets.newHashSet(previousChannel));
			}
			else
			{
				reducedDomains.put(station,domain);
			}
		}
		log.debug("Evaluating if stations not in previous assignment with their neighborhood are packable when all other stations are fixed to previous assignment.");
		
		if(aToPackStations.size()<10)
		{
			log.debug("Missing station and neighborhood: {} .",aToPackStations);
		}

        Map<String, Object> metadata = new HashMap<>(aInstance.getMetadata());
        metadata.put(StationPackingInstance.NAME_KEY, aInstance.getName() + STATION_SUBSET_SATCERTIFIER);
        StationPackingInstance SATboundInstance = new StationPackingInstance(reducedDomains, previousAssignment, metadata);
		
		if(!terminationCriterion.hasToStop())
		{
			watch.stop();
            log.debug("Going off to SAT solver...");
			SolverResult SATboundResult = fSolver.solve(SATboundInstance, terminationCriterion, aSeed);
            log.debug("Back from SAT solver... SAT bound result was {}", SATboundResult.getResult());
            watch.start();

			if(SATboundResult.getResult().equals(SATResult.SAT))
			{
				log.debug("Stations not in previous assignment can be packed with their neighborhood when all other stations are fixed to their previous assignment..");
				
				watch.stop();
				double extraTime = watch.getElapsedTime();
                SATFCMetrics.postEvent(new SATFCMetrics.SolvedByEvent(aInstance.getName(), SATFCMetrics.SolvedByEvent.PRESOLVER, SATboundResult.getResult()));
                return SolverResult.addTime(SATboundResult, extraTime);
			}
			else
			{
				watch.stop();
				double extraTime = watch.getElapsedTime();
				return new SolverResult(SATResult.TIMEOUT, SATboundResult.getRuntime()+extraTime);
			}
		}
		else
		{
			watch.stop();
			double extraTime = watch.getElapsedTime();
			return new SolverResult(SATResult.TIMEOUT, extraTime);
		}
	}

	@Override
	public void notifyShutdown() {
        fSolver.notifyShutdown();
	}

    @Override
    public void interrupt() {
        fSolver.interrupt();
    }
}
