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
package ca.ubc.cs.beta.stationpacking.solvers.decorators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.metrics.SATFCMetrics;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.underconstrained.IUnderconstrainedStationFinder;
import ca.ubc.cs.beta.stationpacking.solvers.underconstrained.UnderconstrainedStationFinder;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

/**
 * Solver decorator that removes underconstrained stations from the instance, solve the sub-instance and then add back
 * the underconstrained stations using simply looping over all the station's domain channels.
 * 
 * A station is underconstrained if no matter what channels the other stations are assigned to, the station will always have a feasible
 * channel to be packed on.
 * 
 * We "approximately" find underconstrained stations by limiting to stations that have more domain channels than sum of stations that CO- or ADJ+1-
 * interfering with them (double counting stations that interferes the two ways).
 * 
 * @author afrechet
 */
@Slf4j
public class UnderconstrainedStationRemoverSolverDecorator extends ASolverDecorator {

	private final IConstraintManager fConstraintManager;
	private final IUnderconstrainedStationFinder fUnderconstrainedStationFinder;
	
	public UnderconstrainedStationRemoverSolverDecorator(ISolver aSolver, IConstraintManager aConstraintManager) {
		super(aSolver);
		fConstraintManager = aConstraintManager;
		fUnderconstrainedStationFinder = new UnderconstrainedStationFinder(fConstraintManager);
	}
	
	@Override
	public SolverResult solve(StationPackingInstance aInstance,
			ITerminationCriterion aTerminationCriterion, long aSeed) {
		
		Watch watch = Watch.constructAutoStartWatch();

		final Map<Station,Set<Integer>> domains = aInstance.getDomains();
		final Set<Station> underconstrainedStations = fUnderconstrainedStationFinder.getUnderconstrainedStations(domains);
		SATFCMetrics.postEvent(new SATFCMetrics.UnderconstrainedStationsRemovedEvent(aInstance.getName(), underconstrainedStations));
		SATFCMetrics.postEvent(new SATFCMetrics.TimingEvent(aInstance.getName(), SATFCMetrics.TimingEvent.FIND_UNDERCONSTRAINED_STATIONS, watch.getElapsedTime()));


		log.debug("Removing {} underconstrained stations...",underconstrainedStations.size());
		
		//Remove the nodes from the instance.
		Map<Station,Set<Integer>> alteredDomains = new HashMap<>(domains);
		alteredDomains = Maps.filterKeys(alteredDomains, new Predicate<Station>(){
			@Override
			public boolean apply(Station arg0) {
				return !underconstrainedStations.contains(arg0);
			}});

		final SolverResult subResult;
		final double preTime;
		if(!alteredDomains.isEmpty())
		{
			//Solve the reduced instance.
			log.debug("Solving the sub-instance...");
			StationPackingInstance alteredInstance = new StationPackingInstance(alteredDomains, aInstance.getPreviousAssignment(), aInstance.getMetadata());
			watch.stop();
			preTime = watch.getElapsedTime();
			log.trace("{} s spent on underconstrained pre-solving setup.",preTime);
			subResult = fDecoratedSolver.solve(alteredInstance, aTerminationCriterion, aSeed);
		}
		else
		{
			log.debug("All stations were underconstrained!");
			preTime = watch.getElapsedTime();
			log.trace("{} s spent on underconstrained pre-solving setup.",preTime);
			subResult = new SolverResult(SATResult.SAT, 0.0,new HashMap<>());
		}
		watch.start();
		
		if(subResult.getResult().equals(SATResult.SAT))
		{
			log.debug("Sub-instance is packable, adding back the underconstrained stations...");
			//If satisfiable, find a channel for the under constrained nodes that were removed by brute force through their domain.
			final Map<Integer,Set<Station>> assignment = subResult.getAssignment();
			final Map<Integer,Set<Station>> alteredAssignment = new HashMap<Integer,Set<Station>>(assignment);
			
			for(Station station : underconstrainedStations)
			{
				
				boolean stationAdded = false;
				
				Set<Integer> domain = domains.get(station);
				//Try to add the underconstrained station at one of its channel domain.
				log.trace("Trying to add back underconstrained station {} on its domain {} ...",station,domain);
				
				for(Integer channel : domain)
				{
					log.trace("Checking domain channel {} ...",channel);
					if(!alteredAssignment.containsKey(channel))
					{
						alteredAssignment.put(channel, new HashSet<Station>());
					}
					
					alteredAssignment.get(channel).add(station);
					final boolean addedSAT = fConstraintManager.isSatisfyingAssignment(alteredAssignment);
					
					if(addedSAT)
					{
						log.trace("Added on channel {}.",channel);
						stationAdded = true;
						break;
					}
					else
					{
						alteredAssignment.get(channel).remove(station);
						if(alteredAssignment.get(channel).isEmpty())
						{
							alteredAssignment.remove(channel);
						}
					}
				}
				if(!stationAdded)
				{
					throw new IllegalStateException("Could not add unconstrained station "+station+" on any of its domain channels.");
				}
			}
			watch.stop();
			final double postTime = watch.getElapsedTime();
			log.trace("{} s spent on underconstrained post-solving wrap up.",postTime);
			return new SolverResult(SATResult.SAT, subResult.getRuntime() + preTime + postTime, alteredAssignment);
			
		}
		else
		{
			log.debug("Sub-instance was not satisfiable, no need to consider adding back underconstrained stations.");
			//Not satisfiable, so re-adding the underconstrained nodes cannot change anything.
			watch.stop();
			final double postTime = watch.getElapsedTime();
			log.trace("{} s spent on underconstrained post-solving wrap up.",postTime);
			return SolverResult.addTime(subResult, preTime + postTime);
		}
	}

}
