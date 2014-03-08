package ca.ubc.cs.beta.stationpacking.facade;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.SolverManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ISolverBundleFactory;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.SATFCSolverBundle;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.ClaspSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.composite.DisjunctiveCompositeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.cputime.CPUTimeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.walltime.WalltimeTerminationCriterion;

/**
 * A facade for solving station packing problems with SATFC.
 * @author afrechet
 */
public class SATFCFacade {
	
	private static Logger log = LoggerFactory.getLogger(SATFCFacade.class);
	private final SolverManager fSolverManager;
	
	/**
	 * Construct a SATFC solver facade.
	 * @param aClaspLibrary - the location of the compiled jna clasp library to use.
	 */
	public SATFCFacade(final String aClaspLibrary)
	{
		
		//Check provided library.
		File libraryFile = new File(aClaspLibrary);
		if(!libraryFile.exists())
		{
			throw new IllegalArgumentException("Provided clasp library does not exist.");
		}
		if (libraryFile.isDirectory())
		{
			throw new IllegalArgumentException("Provided clasp library is a directory.");
		}
		
		try
		{
			new ClaspSATSolver(aClaspLibrary, ClaspLibSATSolverParameters.ALL_CONFIG_11_13);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new IllegalArgumentException("");
		}
		
		
		
		fSolverManager = new SolverManager(
				new ISolverBundleFactory() {
			
					@Override
					public ISolverBundle getBundle(IStationManager aStationManager,
							IConstraintManager aConstraintManager) {
						
						//Set what bundle we're using here.
						return new SATFCSolverBundle(aClaspLibrary, aStationManager, aConstraintManager);
					}
				}
				
				);
	}
	
	/**
	 * Solve a station packing problem.
	 * @param aStations - a collection of integer station IDs.
	 * @param aChannels - a collection of integer channels.
	 * @param aPreviousAssignment - a valid (proved to not create any interference) partial (can concern only some of the provided station) station to channel assignment.
	 * @param aCutoff - a cutoff in seconds for SATFC's execution.
	 * @param aSeed - a long seed for randomization in SATFC.
	 * @param aStationConfigFolder - a folder in which to find station config data (e.g. interferences and domains files).
	 * @return a result about the packability of the provided problem, with the time it took to solve, and corresponding valid witness assignment of station IDs to channels. 
	 */
	public SATFCResult solve(Collection<Integer> aStations,
			Collection<Integer> aChannels,
			Map<Integer,Integer> aPreviousAssignment,
			double aCutoff,
			long aSeed,
			String aStationConfigFolder
			)
	{
		//Check input.
		if(aStations.isEmpty())
		{
			log.warn("Provided an empty collection of stations.");
			return new SATFCResult(SATResult.SAT, 0.0, new HashMap<Integer,Integer>());
		}
		if(aChannels.isEmpty())
		{
			log.warn("Provided an empty collection of channels.");
			return new SATFCResult(SATResult.UNSAT, 0.0, new HashMap<Integer,Integer>());
		}
		if(aCutoff <=0)
		{
			throw new IllegalArgumentException("Cutoff must be strictly positive.");
		}
		
		//Get the data managers and solvers corresponding to the provided station config data.
		ISolverBundle bundle;
		try {
			bundle = fSolverManager.getData(aStationConfigFolder);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			log.error("Did not find the necessary data files in provided station config data folder {}.",aStationConfigFolder);
			throw new IllegalArgumentException("Station config files not found.");
		}
		
		IStationManager stationManager = bundle.getStationManager();
		
		//Translate arguments.
		Set<Station> stations = stationManager.getStationsfromID(aStations);
		Set<Integer> channels = new HashSet<Integer>(aChannels);
		Map<Station,Integer> previousAssignment = new HashMap<Station,Integer>();
		for(Entry<Integer, Integer> stationchannel : aPreviousAssignment.entrySet())
		{
			previousAssignment.put(stationManager.getStationfromID(stationchannel.getKey()), stationchannel.getValue());
		}
		
		//Construct the instance.
		StationPackingInstance instance = new StationPackingInstance(stations, channels, previousAssignment);
		
		//Get solver
		ISolver solver = bundle.getSolver(instance);
		
		//Set termination criterion.
		ITerminationCriterion CPUtermination = new CPUTimeTerminationCriterion(aCutoff);
		ITerminationCriterion WALLtermination = new WalltimeTerminationCriterion(aCutoff);
		ITerminationCriterion termination = new DisjunctiveCompositeTerminationCriterion(Arrays.asList(CPUtermination,WALLtermination)); 
		
		//Solve instance.
		SolverResult result = solver.solve(instance, termination, aSeed);
		
		//Transform back solver result to output result.
		Map<Integer,Integer> witness = new HashMap<Integer,Integer>();
		for(Entry<Integer,Set<Station>> entry : result.getAssignment().entrySet())
		{
			Integer channel = entry.getKey();
			for(Station station : entry.getValue())
			{
				witness.put(station.getID(), channel);
			}
		}
		
		SATFCResult outputResult = new SATFCResult(result.getResult(), result.getRuntime(), witness);
		
		return outputResult;
		
	}
	
	public void notifyShutdown()
	{
		fSolverManager.notifyShutdown();
	}
	
	
	
	
	
	

}
