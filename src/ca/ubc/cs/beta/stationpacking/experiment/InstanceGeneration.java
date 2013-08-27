package ca.ubc.cs.beta.stationpacking.experiment;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.reporters.IExperimentReporter;


/**
 * Experiment to greedily generate a large station packing assignment in SAT.
 * Constructor takes a Solver and an ExperimentReporter.
 * Run method starts with a working set of stations and iteratively adds to it,
 * ensuring that the packing problem remains satisfiable.
 * @author afrechet, narnosti
 */
public class InstanceGeneration {

	private static Logger log = LoggerFactory.getLogger(InstanceGeneration.class);	
	private ISolver fSolver;
	private IExperimentReporter fExperimentReporter;
	
	public InstanceGeneration(ISolver aSolver, IExperimentReporter aExperimentReporter){
		fSolver = aSolver;
		fExperimentReporter = aExperimentReporter;
	}
	
	/**
	 * @param aStartingStations - the initial set of stations (may be empty)
	 * @param aStationIterator - an iterator that specifies the order in which stations are considered
	 * @param aChannelRange - the set of channels into which the stations should be packed
	 * @param aCutoff - the maximum time to consider any individual SAT solver run
	 *
	**/
	public void run(Set<Station> aStartingStations, Iterator<Station> aStationIterator, Set<Integer> aChannelRange,double aCutoff, long aSeed){
		
		int i = 0;
		
		HashSet<Station> aStations = new HashSet<Station>(aStartingStations);
		
		while(aStationIterator.hasNext()) {
			log.info("Iteration "+i);
			i++;
			Station aStation = aStationIterator.next();
			log.info("Trying to add {} to current set.",aStation);
			
			aStations.add(aStation);
			StationPackingInstance aInstance = new StationPackingInstance(aStations,aChannelRange);
				SolverResult aRunResult = fSolver.solve(aInstance,aCutoff,aSeed);
				log.info("Result: {}",aRunResult);
				fExperimentReporter.report(aInstance, aRunResult);
				if(!aRunResult.getResult().equals(SATResult.SAT)){
					log.info("Instance was UNSAT, removing {}",aStation);
					aStations.remove(aStation);						
				} /*else {
					log.info("Instance was SAT, with assignment "+aRunResult.getAssignment());
				}	*/			
		
			log.info("-------------------------------------------------------------------------");
		}
	}	
	
	/**
	 * @return the solver associated with the instance generation.
	 */
	public ISolver getSolver()
	{
		return fSolver;
	}
}
