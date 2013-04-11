package ca.ubc.cs.beta.stationpacking.experiment;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.IExperimentReporter;
import ca.ubc.cs.beta.stationpacking.experiment.instance.IInstance;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.IInstanceEncoder;
import ca.ubc.cs.beta.stationpacking.experiment.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SATResult;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SolverResult;


/**
 * Experiment to generate largest satisfiable station packing instance.
 * @author afrechet
 *
 */
public class InstanceGeneration {

	private static Logger log = LoggerFactory.getLogger(InstanceGeneration.class);
	
	private IInstanceEncoder fInstanceEncoder;
	private ISolver fSolver;
	private IExperimentReporter fExperimentReporter;
	
	
	public InstanceGeneration(IInstanceEncoder aInstanceEncoder, ISolver aSolver, IExperimentReporter aExperimentReporter)
	{
		fInstanceEncoder = aInstanceEncoder;
		fSolver = aSolver;
		fExperimentReporter = aExperimentReporter;
	}
	
	public void run(Set<Station> aStartingStations, Iterator<Station> aStationIterator, double aCutoff, Integer ... aRange)
	{
		Integer[] fRange;
		if(aRange.length > 0){
			fRange = aRange;
		} else {
			fRange = new Integer[2];
			fRange[0] = 30;
			fRange[1] = 14;
		}
		HashSet<Station> aCurrentStations = new HashSet<Station>(aStartingStations);
		
		while(aStationIterator.hasNext())
		{
			
			Station aStation = aStationIterator.next();
			
			log.info("Trying to add {} to current set.",aStation);
			
			aCurrentStations.add(aStation);
			
			try 
			{
				log.info("Getting problem instance.");
				IInstance aInstance = fInstanceEncoder.getProblemInstance(aCurrentStations,fRange);
				
				
				log.info("Solving instance of size {}.",aInstance.getNumberofStations());
				
				SolverResult aRunResult = fSolver.solve(aInstance,aCutoff);
				
				log.info("Result: {}",aRunResult);
				
				try
				{
					fExperimentReporter.report(aInstance, aRunResult);
				} 
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				
				if(!aRunResult.getResult().equals(SATResult.SAT)){
					log.info("Instance was UNSAT, removing station.");
					aCurrentStations.remove(aStation);
				}			
				
			} 
			catch (Exception e1) 
			{
				e1.printStackTrace();
			}
			
			
		}
	}
	
}
