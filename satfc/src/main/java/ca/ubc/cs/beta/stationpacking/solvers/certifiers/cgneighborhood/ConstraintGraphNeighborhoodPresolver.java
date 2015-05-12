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

import java.util.*;

import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.SolverData;
import ca.ubc.cs.beta.stationpacking.solvers.SolverHelper;
import ca.ubc.cs.beta.stationpacking.solvers.base.Assignment;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

/**
 * Pre-solve by applying a sequence of station subsets certifiers based on 
 * the neighborhood of stations missing from a problem instance's previous assignment.
 *
 * If an UNSAT result is found for the immediate local neighborhood of the missing stations, the search
 *  is expanded to include the neighbors' neighbors. This is iterated until any of the following conditions is met:
 *  <ol>
 *      <li>A SAT result is found</li>
 *      <li>The expanded neighborhood contains all the stations connected (directly or indirectly) to the original
 *      	missing stations</li>
 *      <li>The search times out</li>
 *  </ol>
 * @author afrechet
 * @author pcernek
 *
 */
public class ConstraintGraphNeighborhoodPresolver implements ISolver {

	public static final int A_FEW_MISSING_STATIONS = 10;
	// TODO: This arbitrary constant needs to be validated empirically.
	public static final int MAX_MISSING_STATIONS = 20;

	private static Logger log = LoggerFactory.getLogger(ConstraintGraphNeighborhoodPresolver.class);
	
	private final IConstraintManager fConstraintManager;
	private final List<IStationSubsetCertifier> fCertifiers;
	
	public ConstraintGraphNeighborhoodPresolver(IConstraintManager aConstraintManager, List<IStationSubsetCertifier> aCertifiers)
	{
		fConstraintManager = aConstraintManager;
		fCertifiers = aCertifiers;
	}
	
	@Override
	public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed)
	{
		Watch watch = Watch.constructAutoStartWatch();
		
		if (aInstance.getStations().isEmpty()) {
			log.debug("Presolver was given an empty StationPackingInstance.");
			watch.stop();
			return new SolverResult(SATResult.TIMEOUT, watch.getElapsedTime());
		}
		
		Collection<Station> missingStations = getStationsNotInPreviousAssignment(aInstance);
		
		if (missingStations.isEmpty())
		{
			log.debug("No new stations were added this round. Presolver is returning SAT trivially.");
			watch.stop();
			return new SolverResult(SATResult.SAT, watch.getElapsedTime(), Assignment.fromStationChannelMap(aInstance.getPreviousAssignment()).toChannelStationMap());
		}
		
		logNumberOfMissingStations(missingStations);
		
		// Check if there are too many stations to make this procedure worthwhile.
		if (missingStations.size() > MAX_MISSING_STATIONS)
		{
			log.debug("Too many missing stations in previous assignment ({}).", missingStations.size());
			watch.stop();
			return new SolverResult(SATResult.TIMEOUT, watch.getElapsedTime());
		}

		NeighborIndex<Station, DefaultEdge> constraintGraphNeighborIndex = buildNeighborIndex(aInstance);

		SolverData arguments = new SolverData(aInstance, aTerminationCriterion, aSeed, watch);
		SolverResult combinedResult = runPresolver(arguments, missingStations, constraintGraphNeighborIndex);
		
		logResult(combinedResult);

		return combinedResult;
		
	}

	private void logResult(SolverResult combinedResult) {
		log.debug("Result:");
		log.debug(combinedResult.toParsableString());
	}

	private NeighborIndex<Station, DefaultEdge> buildNeighborIndex(StationPackingInstance aInstance) 
	{
		log.debug("Building constraint graph.");
		NeighborIndex<Station, DefaultEdge> aConstraintGraphNeighborIndex =
				new NeighborIndex<>(ConstraintGrouper.getConstraintGraph(aInstance, fConstraintManager));
		return aConstraintGraphNeighborIndex;
	}

	private SolverResult runPresolver(SolverData arguments, Collection<Station> missingStations,
			NeighborIndex<Station, DefaultEdge> aConstraintGraphNeighborIndex)
	{
		List<SolverResult> results = iterateSolverOverNeighbors(arguments, missingStations,
				aConstraintGraphNeighborIndex);
		
		SolverResult combinedResult = SolverHelper.combineResults(results);
		
		arguments.getWatch().stop();
		double extraTime = arguments.getWatch().getElapsedTime();
		combinedResult = SolverResult.addTime(combinedResult, extraTime);
		
		return combinedResult;
	}

	private List<SolverResult> iterateSolverOverNeighbors(SolverData arguments,
			Collection<Station> missingStations,
			NeighborIndex<Station, DefaultEdge> aConstraintGraphNeighborIndex)
		{
		List<SolverResult> results = new LinkedList<>();
		
		int neighborLayer = 1;
		HashSet<Station> toPackStations = new HashSet<>();

		toPackStations.addAll(missingStations);
		boolean foundSAT = false;

		// Keep expanding neighbors until either a SAT result is found or we are forced to stop 
		while (!arguments.getTerminationCriterion().hasToStop() && !foundSAT) {
			
			log.debug("Adding level {} of neighbors.", neighborLayer);
			HashSet<Station> currentNeighbors = getCurrentNeighbors(aConstraintGraphNeighborIndex, toPackStations);
			
			// Only run the solver if new neighbors are actually added to the stations to pack,
			//   unless it's the first layer (in this case we have one or more disconnected components)
			if (toPackStations.addAll(currentNeighbors) || neighborLayer == 1) {
				foundSAT = doCertifiersFindSATResult(arguments, results, toPackStations);
				neighborLayer++;
			}
			else {
				break;
			}
		}
		
		return results;
	}

	private HashSet<Station> getCurrentNeighbors(NeighborIndex<Station, DefaultEdge> aConstraintGraphNeighborIndex,
			HashSet<Station> toPackStations) 
		{
		HashSet<Station> currentNeighbors = new HashSet<>();
		for (Station unpackedStation : toPackStations) {
			Set<Station> neighborStations = aConstraintGraphNeighborIndex.neighborsOf(unpackedStation);
			currentNeighbors.addAll(neighborStations);
		}
		return currentNeighbors;
	}

	private void logNumberOfMissingStations(Collection<Station> missingStations) {
		if(missingStations.size()< A_FEW_MISSING_STATIONS)
		{
			log.debug("Stations {} are not part of previous assignment.",missingStations);
		}
		else
		{
			log.debug("There are {} stations that are not part of previous assignment.",missingStations.size());
		}
	}

	private Collection<Station> getStationsNotInPreviousAssignment(StationPackingInstance aInstance) 
	{
		
		Map<Station,Integer> previousAssignment = aInstance.getPreviousAssignment();
		Collection<Station> missingStations = new HashSet<>();

		for (Station station : aInstance.getStations()) {
			if (!previousAssignment.containsKey(station)) {
				missingStations.add(station);
			}
		}

		return missingStations;
	}

	private boolean doCertifiersFindSATResult(SolverData arguments, List<SolverResult> results,
			HashSet<Station> toPackStations)
	{
		// Unpack arguments
		ITerminationCriterion aTerminationCriterion = arguments.getTerminationCriterion();
		StationPackingInstance aInstance = arguments.getInstance();
		Watch watch = arguments.getWatch();
		long aSeed = arguments.getSeed();
		
		// Run all certifiers
		for (int i = 0; i < fCertifiers.size() && !aTerminationCriterion.hasToStop(); i++) {
            log.debug("Trying constraint graph neighborhood certifier {}.", i + 1);

            IStationSubsetCertifier certifier = fCertifiers.get(i);

            watch.stop();
			SolverResult result = certifier.certify(aInstance, toPackStations, aTerminationCriterion, aSeed);
            watch.start();

            results.add(result);

            if (isConclusive(result)) {
                SATFCMetrics.postEvent(new SolvedByEvent(aInstance.getName(), SolvedByEvent.PRESOLVER, result.getResult()));
                return result.getResult().equals(SATResult.SAT);
            }
        }

		return false;
	}

	private boolean isConclusive(SolverResult result) {
		return result.getResult().equals(SATResult.SAT) || result.getResult().equals(SATResult.UNSAT);
	}

	@Override
	public void interrupt() throws UnsupportedOperationException {
		for(IStationSubsetCertifier certifier : fCertifiers)
		{
			certifier.interrupt();
		}
	}

	@Override
	public void notifyShutdown() {
		for(IStationSubsetCertifier certifier : fCertifiers)
		{
			certifier.notifyShutdown();
		}
	}
	
}
