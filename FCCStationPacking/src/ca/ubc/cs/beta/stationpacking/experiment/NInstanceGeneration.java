package ca.ubc.cs.beta.stationpacking.experiment;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.IExperimentReporter;
import ca.ubc.cs.beta.stationpacking.experiment.instance.*;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.IInstanceEncoder;
import ca.ubc.cs.beta.stationpacking.experiment.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SATResult;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SolverResult;


/**
 * Experiment to generate largest satisfiable station packing instance.
 * @author afrechet
 *
 */
public class NInstanceGeneration {

	private static Logger log = LoggerFactory.getLogger(InstanceGeneration.class);
	
	private ISolver fSolver;
	private IExperimentReporter fExperimentReporter;
	
	public NInstanceGeneration(ISolver aSolver, IExperimentReporter aExperimentReporter){
		fSolver = aSolver;
		fExperimentReporter = aExperimentReporter;
	}
	
	public void run(Set<Station> aStartingStations, Iterator<Station> aStationIterator, double aCutoff,Integer ... aChannelRange){
		//HashSet<Station> aCurrentStations = new HashSet<Station>(aStartingStations);
		IInstance aInstance = new NInstance(aStartingStations,aChannelRange);
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
				}			
			} 
			catch (Exception e){ 
				e.printStackTrace();
			} 
		}
	}	
}
