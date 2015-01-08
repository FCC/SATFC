/**
 * Copyright 2014, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
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
package ca.ubc.cs.beta.stationpacking.facade;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.logging.LogLevel;
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

import com.google.common.collect.Sets;
import com.google.common.io.Resources;

/**
 * A facade for solving station packing problems with SATFC.
 * Each instance of the facade corresponds to an independent copy
 * of SATFC (with different state).
 * @author afrechet
 */
public class SATFCFacade implements AutoCloseable{
	
	private final Logger log;
	
	private volatile static boolean logInitialized = false;
	private final SolverManager fSolverManager;
	
//	/**
//	 * Construct a SATFC solver facade.
//	 * @param aClaspLibrary - the location of the compiled jna clasp library to use.
//	 */
//	public SATFCFacade(final String aClaspLibrary)
//	{
//		this(aClaspLibrary,true);
//	}
	
	
	/**
	 * Construct a SATFC solver facade, with the option of initializing logging if its not already done.
	 * @param aSATFCParameters parameters needed by the facade.
	 */
	SATFCFacade(final SATFCFacadeParameter aSATFCParameters)
	{
		//Initialize logging.
		if(!logInitialized && aSATFCParameters.isInitializeLogging())
		{
			initializeLogging(LogLevel.INFO);
			log = LoggerFactory.getLogger(getClass());
			log.warn("Logging initialized by default to INFO.");
			
		} else
		{
			log = LoggerFactory.getLogger(getClass());
		}
		
		//Check provided library.
		if(aSATFCParameters.getClaspLibrary() == null)
		{
			throw new IllegalArgumentException("Cannot provide null library.");
		}
		
		File libraryFile = new File(aSATFCParameters.getClaspLibrary());
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
			new ClaspSATSolver(aSATFCParameters.getClaspLibrary(), ClaspLibSATSolverParameters.ALL_CONFIG_11_13);
		}
		catch(UnsatisfiedLinkError e)
		{
			
			log.error("\n--------------------------------------------------------\n"+
					"Could not load clasp from library : {}. \n"+
					"Possible Solutions:\n"+
					"1) Try rebuilding the library, on Linux this can be done by going to the clasp folder and running \"bash compile.sh\"\n"+
					"2) Check that all library dependancies are met, e.g., run \"ldd {}\".\n"+
					"3) Manually set the library to use with the \"-CLASP-LIBRARY\" options. On MacOS it should be a .dylib file, on linux a .so file.\n"+
					"--------------------------------------------------------", aSATFCParameters.getClaspLibrary(), aSATFCParameters.getClaspLibrary()
					);
			
			log.debug("Exception occured while loading library:\n",e);
			
			throw new IllegalArgumentException("Could not load JNA library.");
		}
		
		log.debug("Using library {}.",aSATFCParameters.getClaspLibrary());
		
		fSolverManager = new SolverManager(
				new ISolverBundleFactory() {
			
					@Override
					public ISolverBundle getBundle(IStationManager aStationManager,
							IConstraintManager aConstraintManager) {
						
						/*
						 * SOLVER BUNDLE.
						 * 
						 * Set what bundle we're using here.
						 */
						switch(aSATFCParameters.getSolverChoice())
						{
						case SATFC:
							return new SATFCSolverBundle(
									aSATFCParameters.getClaspLibrary(),
									aStationManager,
									aConstraintManager,
									aSATFCParameters.getCNFDirectory(),
									aSATFCParameters.getResultFile());
						case MIPFC:
							throw new UnsupportedOperationException("MIPFC not available in current release.");
						default:
							throw new IllegalArgumentException("Unrecognized solver choice "+aSATFCParameters.getSolverChoice());
						}
						
						
					}
				}
				
				);
	}
		
	
	/**
	 * Solve a station packing problem. 
	 * @param aDomains - a map taking integer station IDs to set of integer channels domains.
	 * @param aPreviousAssignment - a valid (proved to not create any interference) partial (can concern only some of the provided station) station to channel assignment.
	 * @param aCutoff - a cutoff in seconds for SATFC's execution.
	 * @param aSeed - a long seed for randomization in SATFC.
	 * @param aStationConfigFolder - a folder in which to find station config data (<i>i.e.</i> interferences and domains files).
	 * @return a result about the packability of the provided problem, with the time it took to solve, and corresponding valid witness assignment of station IDs to channels. 
	 */
	public SATFCResult solve(
			Map<Integer,Set<Integer>> aDomains,
			Map<Integer,Integer> aPreviousAssignment,
			double aCutoff, 
			long aSeed, String aStationConfigFolder)
	{
		if(aDomains == null || aPreviousAssignment == null || aStationConfigFolder == null)
		{
			throw new IllegalArgumentException("Cannot provide null arguments.");
		}
		
		if(aDomains.isEmpty())
		{
			log.warn("Provided an empty domains map.");
			return new SATFCResult(SATResult.SAT, 0.0, new HashMap<Integer,Integer>());
		}
		if(aCutoff <=0)
		{
			throw new IllegalArgumentException("Cutoff must be strictly positive.");
		}
		
		log.debug("Getting data managers...");
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
		
		log.debug("Translating arguments to SATFC objects...");
		//Translate arguments.
		Map<Station,Set<Integer>> domains = new HashMap<Station,Set<Integer>>();
		
		for(Integer stationID : aDomains.keySet())
		{
			Station station = stationManager.getStationfromID(stationID);
			
			Set<Integer> domain = aDomains.get(stationID);
			Set<Integer> stationDomain = stationManager.getDomain(station);
			
			Set<Integer> truedomain = Sets.intersection(domain, stationDomain);
			
			if(truedomain.isEmpty())
			{
				log.warn("Station {} has an empty domain, cannot pack.",station);
				return new SATFCResult(SATResult.UNSAT, 0.0, new HashMap<Integer,Integer>());
			}
			
			domains.put(station, truedomain);
		}
		
		Map<Station,Integer> previousAssignment = new HashMap<Station,Integer>();
		for(Station station : domains.keySet())
		{
			Integer previousChannel = aPreviousAssignment.get(station.getID());
			if(previousChannel != null && previousChannel > 0)
			{
				previousAssignment.put(station, previousChannel);
			}
		}
		
		log.debug("Constructing station packing instance...");
		//Construct the instance.
		StationPackingInstance instance = new StationPackingInstance(domains, previousAssignment);
		
		log.debug("Getting solver...");
		//Get solver
		ISolver solver = bundle.getSolver(instance);
		
		log.debug("Setting termination criterion...");
		//Set termination criterion.
		ITerminationCriterion CPUtermination = new CPUTimeTerminationCriterion(aCutoff);
		ITerminationCriterion WALLtermination = new WalltimeTerminationCriterion(aCutoff);
		ITerminationCriterion termination = new DisjunctiveCompositeTerminationCriterion(Arrays.asList(CPUtermination,WALLtermination)); 
		
		/*
		 * Logging problem info
		 */
		log.debug("Solving instance {} ...",instance);
		log.debug("Instance stats:");
		log.debug("{} stations.",instance.getStations().size());
		log.debug("stations: {}.",instance.getStations());
		log.debug("{} all channels.",instance.getAllChannels().size());
		log.debug("all channels: {}.",instance.getAllChannels());
		log.debug("Previous assignment: {}",instance.getPreviousAssignment());
		
    	SolverResult result; 
		//Solve instance.
		result = solver.solve(instance, termination, aSeed);
    		
		log.debug("Transforming result into SATFC output...");
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
		
		log.debug("Result: {}.",outputResult);
		
		return outputResult;
	
	}
	
	/**
	 * Solve a station packing problem. The channel domain of a station will be the intersection of the station's original domain (given in data files) with the packing channels,
	 * and additionally intersected with its reduced domain if available and if non-empty. 
	 * @param aStations - a collection of integer station IDs.
	 * @param aChannels - a collection of integer channels.
	 * @param aReducedDomains - a map taking integer station IDs to set of integer channels domains.
	 * @param aPreviousAssignment - a valid (proved to not create any interference) partial (can concern only some of the provided station) station to channel assignment.
	 * @param aCutoff - a cutoff in seconds for SATFC's execution.
	 * @param aSeed - a long seed for randomization in SATFC.
	 * @param aStationConfigFolder - a folder in which to find station config data (<i>i.e.</i> interferences and domains files).
	 * @return a result about the packability of the provided problem, with the time it took to solve, and corresponding valid witness assignment of station IDs to channels. 
	 */
	public SATFCResult solve(Set<Integer> aStations,
			Set<Integer> aChannels,
			Map<Integer,Set<Integer>> aReducedDomains,
			Map<Integer,Integer> aPreviousAssignment,
			double aCutoff,
			long aSeed,
			String aStationConfigFolder
			)
	{
		//Check input.
		if(aStations == null || aChannels == null ||  aPreviousAssignment == null || aStationConfigFolder == null || aReducedDomains == null)
		{
			throw new IllegalArgumentException("Cannot provide null arguments.");
		}
		
		if(aStations.isEmpty())
		{
			log.warn("Provided an empty set of stations.");
			return new SATFCResult(SATResult.SAT, 0.0, new HashMap<Integer,Integer>());
		}
		if(aCutoff <=0)
		{
			throw new IllegalArgumentException("Cutoff must be strictly positive.");
		}
		
				
		log.debug("Transforming instance to a domains only instance.");
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
		
		//TODO Change facade to only be given a simple domains map.
		//Construct the domains map.
		Map<Integer,Set<Integer>> aDomains = new HashMap<Integer,Set<Integer>>();
		
		for(Integer station : aStations)
		{
			Set<Integer> originalDomain = stationManager.getDomain(stationManager.getStationfromID(station));
			Set<Integer> reducedDomain = aReducedDomains.get(station);
			
			Set<Integer> domain;
			domain = Sets.intersection(originalDomain, aChannels);
			if(reducedDomain != null && !reducedDomain.isEmpty())
			{
				domain = Sets.intersection(domain, reducedDomain);
			}
			
			aDomains.put(station, domain);
		}
		
		return solve(aDomains,aPreviousAssignment,aCutoff,aSeed,aStationConfigFolder);
		
	}

	@Override
	public void close() throws Exception{
		log.info("Shutting down...");
		fSolverManager.close();
		log.info("Goodbye!");
	}

	
	private static final String LOGBACK_CONFIGURATION_FILE_PROPERTY ="logback.configurationFile";
	/**
	 * Initialize logging.
	 * @param logLevel - logging level to use.
	 */
	public static synchronized void initializeLogging(LogLevel logLevel)
	{
		if (logInitialized) return;
		
		System.setProperty("LOGLEVEL", logLevel.name());
		if(System.getProperty(LOGBACK_CONFIGURATION_FILE_PROPERTY)!= null)
		{
			Logger log = LoggerFactory.getLogger(SATFCFacade.class);
			log.debug("System property for logback.configurationFile has been found already set as {} , logging will follow this file.", System.getProperty(LOGBACK_CONFIGURATION_FILE_PROPERTY));
		} else
		{
			
			String logback = Resources.getResource("logback.xml").toString(); 
			System.setProperty(LOGBACK_CONFIGURATION_FILE_PROPERTY, logback);
			
			Logger log = LoggerFactory.getLogger(SATFCFacade.class);
			if(log.isDebugEnabled())
			{
				log.debug("Logging initialized to use file:" + logback);
			} else
			{
				log.debug("Logging initialized");
			}
			
		}
		logInitialized = true;
	}
	
	
	
	
	

}
