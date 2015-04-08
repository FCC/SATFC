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
package ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

	private static final int MAX_MISSING_STATIONS=20;
	private static final int MAX_TO_PACK=100;
	
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
		if(missingStations.size()<10)
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
		NeighborIndex<Station, DefaultEdge> aConstraintGraphNeighborIndex = new NeighborIndex<Station,DefaultEdge>(ConstraintGrouper.getConstraintGraph(aInstance, fConstraintManager));
		
		HashSet<Station> topackStations = new HashSet<Station>();
		for(Station missingStation : missingStations)
		{
			topackStations.add(missingStation);
			topackStations.addAll(aConstraintGraphNeighborIndex.neighborsOf(missingStation));
		}
		
		//Check if there are too many stations to make this procedure worthwhile.
		if(topackStations.size()>MAX_TO_PACK)
		{
			log.debug("Neighborhood to pack is too large ({}).",topackStations.size());
			
			watch.stop();
			return new SolverResult(SATResult.TIMEOUT,watch.getElapsedTime());
		}
		
		List<SolverResult> results = new LinkedList<SolverResult>();
		for(int i=0;i<fCertifiers.size() && !aTerminationCriterion.hasToStop(); i++)
		{
			log.debug("Trying constraint graph neighborhood certifier {}.",i+1);
			
			IStationSubsetCertifier certifier = fCertifiers.get(i);
			
			watch.stop();
			SolverResult result = certifier.certify(aInstance, topackStations, aTerminationCriterion, aSeed);
			watch.start();
			
			results.add(result);
			
			if(result.getResult().equals(SATResult.SAT) || result.getResult().equals(SATResult.UNSAT))
			{
				SATFCMetrics.postEvent(new SATFCMetrics.SolvedByEvent(aInstance.getName(), SolvedByEvent.PRESOLVER, result.getResult()));
				break;
			}
		}
		
		SolverResult combinedResult = SolverHelper.combineResults(results);
		
		watch.stop();
		double extraTime = watch.getElapsedTime();

		combinedResult = SolverResult.addTime(combinedResult, extraTime);
		
		log.debug("Result:");
		log.debug(combinedResult.toParsableString());

		return combinedResult;
		
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
