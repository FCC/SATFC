package ca.ubc.cs.beta.stationpacking.experiment;

import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.IExperimentReporter;
import ca.ubc.cs.beta.stationpacking.experiment.instance.*;
import ca.ubc.cs.beta.stationpacking.experiment.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SATResult;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SolverResult;


/**
 * Experiment to greedily generate a large station packing assignment in SAT.
 * Constructor takes a Solver and an ExperimentReporter.
 * Run method starts with a working set of stations and iteratively adds to it,
 * ensuring that the packing problem remains satisfiable.
 * @author afrechet, narnosti
 */
public class NInstanceGeneration {

	private static Logger log = LoggerFactory.getLogger(NInstanceGeneration.class);	
	private ISolver fSolver;
	private IExperimentReporter fExperimentReporter;
	
	public NInstanceGeneration(ISolver aSolver, IExperimentReporter aExperimentReporter){
		fSolver = aSolver;
		fExperimentReporter = aExperimentReporter;
	}
	
	/*@param aStartingStations - the initial set of stations (may be empty)
	 *@param aStationIterator - an iterator that specifies the order in which stations are considered
	 *@param aChannelRange - the set of channels into which the stations should be packed
	 *@param aCutoff - the maximum time to consider any individual SAT solver run
	 */
	public void run(Set<Station> aStartingStations, Iterator<Station> aStationIterator, Set<Integer> aChannelRange,double aCutoff){
		IInstance aInstance = new NInstance(aStartingStations,aChannelRange);
		System.out.println(aStationIterator.hasNext());
		while(aStationIterator.hasNext()) {
			Station aStation = aStationIterator.next();
			log.info("Trying to add {} to current set.",aStation);
			aInstance.addStation(aStation);
			try {
				log.info("Solving instance of size {}.",aInstance.getNumberofStations());
				SolverResult aRunResult = fSolver.solve(aInstance,aCutoff);
				log.info("Result: {}",aRunResult);
				fExperimentReporter.report(aInstance, aRunResult);
				if(!aRunResult.getResult().equals(SATResult.SAT)){
					log.info("Instance was UNSAT, removing station.");
					aInstance.removeStation(aStation);
				} else {
					
				}
			} 
			catch (Exception e){ 
				e.printStackTrace();
			} 
		}
	}	
}
