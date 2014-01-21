package ca.ubc.cs.beta.stationpacking.solvers.certifierpresolvers.cgneighborhood;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
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

private static final int MAX_MISSING_STATIONS=1;
	
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
		
		HashMap<Station,Integer> previousAssignment = aInstance.getPreviousAssignment();
		
		//Check if there is any previous assignment to work with.
		if(previousAssignment.isEmpty())
		{
			log.warn("No assignment to use for bounding pre-solving.");
			
			watch.stop();
			return new SolverResult(SATResult.TIMEOUT, watch.getEllapsedTime());
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
		log.debug("There are {} stations that are not part of previous assignment.",missingStations.size());
		//Check if there are too many stations to make this procedure worthwile.
		if(missingStations.size()>MAX_MISSING_STATIONS)
		{
			log.warn("Too many missing stations in previous assignment ({}).",missingStations.size());
			
			watch.stop();
			return new SolverResult(SATResult.TIMEOUT,watch.getEllapsedTime());
		}
		
		log.debug("Building constraint graph.");
		NeighborIndex<Station, DefaultEdge> aConstraintGraphNeighborIndex = new NeighborIndex<Station,DefaultEdge>(ConstraintGrouper.getConstraintGraph(aInstance, fConstraintManager));
		
		HashSet<Station> topackStations = new HashSet<Station>();
		for(Station missingStation : missingStations)
		{
			topackStations.add(missingStation);
			topackStations.addAll(aConstraintGraphNeighborIndex.neighborsOf(missingStation));
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
				break;
			}
		}
		
		SolverResult combinedResult = SolverHelper.combineResults(results);
		
		watch.stop();
		double extraTime = watch.getEllapsedTime();

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
