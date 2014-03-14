package ca.ubc.cs.beta.stationpacking.facade;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
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
public class SATFCFacade implements AutoCloseable{
	
	private static Logger log = LoggerFactory.getLogger(SATFCFacade.class);
	private final SolverManager fSolverManager;
	
	/**
	 * Construct a SATFC solver facade.
	 * @param aClaspLibrary - the location of the compiled jna clasp library to use.
	 */
	public SATFCFacade(final String aClaspLibrary)
	{
		
		//Check provided library.
		if(aClaspLibrary == null)
		{
			throw new IllegalArgumentException("Cannot provide null library.");
		}
		
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
	 * @param aDomains - a map taking integer station IDs to set of integer channels domains. The domain of every station in aStations will be <i>reduced</i> to the set
	 * of channels assigned to the station's ID in this map. All stations start with default domains specified by their domains file in the station config folder. If a station
	 * does not appear in this map, its domain will not be reduced. 
	 * @param aPreviousAssignment - a valid (proved to not create any interference) partial (can concern only some of the provided station) station to channel assignment.
	 * @param aCutoff - a cutoff in seconds for SATFC's execution.
	 * @param aSeed - a long seed for randomization in SATFC.
	 * @param aStationConfigFolder - a folder in which to find station config data (<i>i.e.</i> interferences and domains files).
	 * @return a result about the packability of the provided problem, with the time it took to solve, and corresponding valid witness assignment of station IDs to channels. 
	 */
	public SATFCResult solve(Set<Integer> aStations,
			Set<Integer> aChannels,
			Map<Integer,Set<Integer>> aDomains,
			Map<Integer,Integer> aPreviousAssignment,
			double aCutoff,
			long aSeed,
			String aStationConfigFolder
			)
	{
		log.info("Checking input...");
		//Check input.
		if(aStations == null || aChannels == null || aPreviousAssignment == null || aStationConfigFolder == null || aDomains == null)
		{
			throw new IllegalArgumentException("Cannot provide null arguments.");
		}
		
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
		
		log.info("Getting data managers...");
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
		
		log.info("Translating arguments to SATFC objects...");
		//Translate arguments.
		Set<Station> originalStations = stationManager.getStationsfromID(aStations);
		Set<Integer> channels = new HashSet<Integer>(aChannels);
		
		log.info("Constraining station domains...");
		//Constrain domains.
		Set<Station> constrainedStations = new HashSet<Station>();
		for(Station station : originalStations)
		{
			Set<Integer> reducedDomain = aDomains.get(station.getID());
			if(reducedDomain != null)
			{
				constrainedStations.add(station.getReducedDomainStation(reducedDomain));
			}
			else
			{
				constrainedStations.add(station);
			}
		}
		
		Map<Station,Integer> previousAssignment = new HashMap<Station,Integer>();
		for(Station station : constrainedStations)
		{
			Integer previousChannel = aPreviousAssignment.get(station.getID());
			if(previousChannel != null)
			{
				previousAssignment.put(station, previousChannel);
			}
		}
		
		log.info("Constructing station packing instance...");
		//Construct the instance.
		StationPackingInstance instance = new StationPackingInstance(constrainedStations, channels, previousAssignment);
		
		log.info("Getting solver...");
		//Get solver
		ISolver solver = bundle.getSolver(instance);
		
		log.info("Setting termination criterion...");
		//Set termination criterion.
		ITerminationCriterion CPUtermination = new CPUTimeTerminationCriterion(aCutoff);
		ITerminationCriterion WALLtermination = new WalltimeTerminationCriterion(aCutoff);
		ITerminationCriterion termination = new DisjunctiveCompositeTerminationCriterion(Arrays.asList(CPUtermination,WALLtermination)); 
		
		log.info("Solving instance...");
		//Solve instance.
		SolverResult result = solver.solve(instance, termination, aSeed);
		
		log.info("Transforming result into SATFC output...");
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

	@Override
	public void close(){
		log.info("Shutting down...");
		fSolverManager.notifyShutdown();
		log.info("Goodbye!");
	}
	
	
	
	
	
	

}
