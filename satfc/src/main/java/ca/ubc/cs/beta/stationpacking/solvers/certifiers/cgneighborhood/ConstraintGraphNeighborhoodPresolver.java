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
import ca.ubc.cs.beta.stationpacking.metrics.SATFCMetrics;
import ca.ubc.cs.beta.stationpacking.metrics.SATFCMetrics.SolvedByEvent;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.SolverHelper;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

/**
 * Pre-solve by applying a sequence of station subsets certifiers based on 
 * the neighborhood of stations missing from a problem instance's previous assignment. 
 * @author afrechet
 *
 */
public class ConstraintGraphNeighborhoodPresolver implements ISolver {

	public static final int FEW_MISSING_STATIONS = 10;
	private static final int MAX_MISSING_STATIONS=20;
	private static final int MAX_TO_PACK=100;
	private static final int MAX_NEIGHBOR_DISTANCE = 25;

	private static Logger log = LoggerFactory.getLogger(ConstraintGraphNeighborhoodPresolver.class);
	
	private final IConstraintManager fConstraintManager;
	
	private final List<IStationSubsetCertifier> fCertifiers;
	
	public ConstraintGraphNeighborhoodPresolver(IConstraintManager aConstraintManager, List<IStationSubsetCertifier> aCertifiers)
	{
		fConstraintManager = aConstraintManager;
		fCertifiers = aCertifiers;
	}
	
	@Override
	public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion,
			long aSeed) {
		
		Watch watch = Watch.constructAutoStartWatch();
		
		Map<Station,Integer> previousAssignment = aInstance.getPreviousAssignment();
		
		//Check if there is any previous assignment to work with.
		if(previousAssignment.isEmpty())
		{
			log.debug("No assignment to use for bounding pre-solving.");
			
			watch.stop();
			return new SolverResult(SATResult.TIMEOUT, watch.getElapsedTime());
		}
		
		//Get the stations in the problem instance that are not in the previous assignment.
		Collection<Station> missingStations = new HashSet<Station>();
		for(Station station : aInstance.getStations())
		{
			if(!previousAssignment.containsKey(station))
			{
				missingStations.add(station);
			}
		}
		if(missingStations.size()< FEW_MISSING_STATIONS)
		{
			log.debug("Stations {} are not part of previous assignment.",missingStations);
		}
		else
		{
			log.debug("There are {} stations that are not part of previous assignment.",missingStations.size());
		}
		
		//Check if there are too many stations to make this procedure worthwhile.
		if(missingStations.size()>MAX_MISSING_STATIONS)
		{
			log.debug("Too many missing stations in previous assignment ({}).",missingStations.size());
			
			watch.stop();
			return new SolverResult(SATResult.TIMEOUT,watch.getElapsedTime());
		}

		log.debug("Building constraint graph.");
		NeighborIndex<Station, DefaultEdge> aConstraintGraphNeighborIndex =
				new NeighborIndex<Station, DefaultEdge>(ConstraintGrouper.getConstraintGraph(aInstance, fConstraintManager));

		// Iterate through successive layers of neighbors
		int neighborLayer = 1;
		List<SolverResult> results = new LinkedList<SolverResult>();
		HashSet<Station> toPackStations = new HashSet<>();
		toPackStations.addAll(missingStations);

		while (neighborLayer < MAX_NEIGHBOR_DISTANCE && !aTerminationCriterion.hasToStop()) {

			// Add the next layer of neighbors to the stations to repack.
			log.debug("Adding level {} of neighbors.", neighborLayer);
			for (Station unpackedStation : toPackStations) {
				Set<Station> neighborStations = aConstraintGraphNeighborIndex.neighborsOf(unpackedStation);
				toPackStations.addAll(neighborStations);
			}

			//Check if there are too many stations to make this procedure worthwhile.
			if (toPackStations.size() > MAX_TO_PACK) {
				log.debug("Neighborhood to pack is too large ({}).", toPackStations.size());

				watch.stop();
				return new SolverResult(SATResult.TIMEOUT, watch.getElapsedTime());
			}

			boolean solved = runCertifiersOnInstance(aInstance, aTerminationCriterion, aSeed, watch, results, toPackStations);
			if (solved)
				break;

			neighborLayer++;
		}
		
		SolverResult combinedResult = SolverHelper.combineResults(results);
		
		watch.stop();
		double extraTime = watch.getElapsedTime();

		combinedResult = SolverResult.addTime(combinedResult, extraTime);
		
		log.debug("Result:");
		log.debug(combinedResult.toParsableString());

		return combinedResult;
		
	}

	private boolean runCertifiersOnInstance(
			StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed,
			Watch watch, List<SolverResult> results, HashSet<Station> toPackStations)
	{
		for (int i = 0; i < fCertifiers.size() && !aTerminationCriterion.hasToStop(); i++) {
            log.debug("Trying constraint graph neighborhood certifier {}.", i + 1);

            IStationSubsetCertifier certifier = fCertifiers.get(i);

            watch.stop();
            SolverResult result = certifier.certify(aInstance, toPackStations, aTerminationCriterion, aSeed);
            watch.start();

            results.add(result);

            if (result.getResult().equals(SATResult.SAT) || result.getResult().equals(SATResult.UNSAT)) {
                SATFCMetrics.postEvent(new SolvedByEvent(aInstance.getName(), SolvedByEvent.PRESOLVER, result.getResult()));
                return true;
            }
        }

		return false;
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
