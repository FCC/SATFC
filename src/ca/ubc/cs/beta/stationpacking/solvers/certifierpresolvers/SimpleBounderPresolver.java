package ca.ubc.cs.beta.stationpacking.solvers.certifierpresolvers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

import com.google.common.collect.Sets;

/**
 * Simple certifier-type pre-solver by Ilya Segal that uses a station packing instance's previous assignment in the following way:
 * <ol>
 * <li>
 * Fix all stations that are part of previous assignment and not a neighbor of a station not in the previous assignment. Then check
 * if the problem can be solved. This is a SAT certifier-type pre-solver.
 * </li>
 * <li>
 * Remove all stations that are part of previous assignment and not a neighbor of a station not in the previous assignment. Then check
 * if the problem can be solved. This is an UNSAT certifier-type pre-solver.
 * </li>
 * </ol>
 * The goal is to use previous assignment to quickly assess if we can or cannot solve a station packing instance.
 * @author afrechet
 *
 */
public class SimpleBounderPresolver implements ISolver{
	
	private static final int MAX_MISSING_STATIONS=1;
	
	private static Logger log = LoggerFactory.getLogger(SimpleBounderPresolver.class);
	
	private final ISolver fSolver;
	private final IConstraintManager fConstraintManager;
	
	public SimpleBounderPresolver(ISolver aSolver, IConstraintManager aConstraintManager)
	{
		fSolver = aSolver;
		fConstraintManager = aConstraintManager;
	}
	
	
	@Override
	public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion,
			long aSeed) {
		
		AutoStartStopWatch preWatch = new AutoStartStopWatch();
		double aTimeSpent = 0;
		
		HashMap<Station,Integer> previousAssignment = aInstance.getPreviousAssignment();
		
		//Check if there is any previous assignment to work with.
		if(previousAssignment.isEmpty())
		{
			log.warn("No assignment to use for bounding pre-solving.");
			
			return new SolverResult(SATResult.TIMEOUT, preWatch.stop()/1000.0);
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
			
			return new SolverResult(SATResult.TIMEOUT, preWatch.stop()/1000.0);
		}
		
		log.debug("Building constraint graph.");
		NeighborIndex<Station, DefaultEdge> aConstraintGraphNeighborIndex = new NeighborIndex<Station,DefaultEdge>(ConstraintGrouper.getConstraintGraph(aInstance, fConstraintManager));
		
		HashSet<Station> topackStations = new HashSet<Station>();
		for(Station missingStation : missingStations)
		{
			topackStations.add(missingStation);
			topackStations.addAll(aConstraintGraphNeighborIndex.neighborsOf(missingStation));
		}
		
		aTimeSpent += preWatch.stop()/1000.0;
		
		/*
		 * Try the UNSAT bound, namely to see if the missing stations plus their neighborhoods are unpackable.
		 */
		//Get the neighbours of the missing stations.
		log.debug("Evaluating if stations not in previous assignment ({}) with their neighborhood are unpackable.",topackStations.size());
		StationPackingInstance UNSATboundInstance = new StationPackingInstance(topackStations, aInstance.getChannels(), previousAssignment);
		
		
		if(!aTerminationCriterion.hasToStop())
		{
			SolverResult UNSATboundResult = fSolver.solve(UNSATboundInstance, aTerminationCriterion, aSeed);
			
			aTimeSpent += UNSATboundResult.getRuntime();
			
			if(UNSATboundResult.getResult().equals(SATResult.UNSAT))
			{
				log.debug("Stations not in previous assignment cannot be packed with their neighborhood.");
				return new SolverResult(SATResult.UNSAT,aTimeSpent);
			}
			else if(UNSATboundResult.getResult().equals(SATResult.TIMEOUT))
			{
				return new SolverResult(SATResult.TIMEOUT, aTimeSpent);
			}
		}
		else
		{
			return new SolverResult(SATResult.TIMEOUT, aTimeSpent);
		}
		
		AutoStartStopWatch interWatch = new AutoStartStopWatch();
		
		/*
		 * Try the SAT bound, namely to see if the missing stations plus their neighborhood are packable when all other stations are fixed
		 * to their previous assignment values.
		 */
		//Change station packing instance so that the 'not to pack' stations have reduced domain.
		Set<Station> reducedDomainStations = new HashSet<Station>();
		for(Station station : aInstance.getStations())
		{
			if(!topackStations.contains(station))
			{
				reducedDomainStations.add(new Station(station.getID(),Sets.newHashSet(previousAssignment.get(station))));
			}
			else
			{
				reducedDomainStations.add(station);
			}
		}
		log.debug("Evaluating if stations not in previous assignment ({}) with their neighborhood are packable when all other stations are fixed to previous assignment.",reducedDomainStations.size());
		StationPackingInstance SATboundInstance = new StationPackingInstance(reducedDomainStations, aInstance.getChannels(), previousAssignment);
		
		aTimeSpent += interWatch.stop()/1000.0;
		if(!aTerminationCriterion.hasToStop())
		{
			SolverResult SATboundResult = fSolver.solve(SATboundInstance, aTerminationCriterion, aSeed);
			
			aTimeSpent += SATboundResult.getRuntime();
			
			if(SATboundResult.getResult().equals(SATResult.SAT))
			{
				log.debug("Stations not in previous assignment can be packed with their neighborhood when all other stations are fixed to their previous assignment..");
				return new SolverResult(SATResult.SAT,aTimeSpent,SATboundResult.getAssignment());
			}
			else if(SATboundResult.getResult().equals(SATResult.TIMEOUT))
			{
				return new SolverResult(SATResult.TIMEOUT, aTimeSpent);
			}
		}
		else
		{
			return new SolverResult(SATResult.TIMEOUT, aTimeSpent);
		}

		return new SolverResult(SATResult.TIMEOUT,aTimeSpent);
	}
	

	@Override
	public void interrupt() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Cannot be interrupted.");
	}

	@Override
	public void notifyShutdown() {
		//No shutdown necessary.
	}



}
