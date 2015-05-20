/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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
package ca.ubc.cs.beta.stationpacking.tae;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import lombok.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunningAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.kill.StatusVariableKillHandler;
import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aeatk.misc.watch.StopWatch;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.AbstractSyncTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacade;
import ca.ubc.cs.beta.stationpacking.facade.SATFCResult;

import com.google.common.collect.ImmutableMap;

/**
 * Target algorithm evaluator that wraps around the SATFC facade and only
 * solves, synchronously, station packing feasibility problems.
 * 
 * @author afrechet, seramage
 */
public class SATFCTargetAlgorithmEvaluator extends
		AbstractSyncTargetAlgorithmEvaluator {
	
	private static final Logger log = LoggerFactory.getLogger(SATFCTargetAlgorithmEvaluator.class);
	
	//Context key for a SATFC specific TAE run config.
	public final static String SATFC_CONTEXT_KEY = "SATFC_CONTEXT";

	private final SATFCFacade fSATFCFacade;
	private final String fStationConfigFolder;

	private final ScheduledExecutorService fObserverThreadPool;

	private static final Semaphore fUniqueSATFCTAESemaphore = new Semaphore(1);

	public SATFCTargetAlgorithmEvaluator(SATFCFacade aSATFCFacade, String aStationConfigFolder) {
		
	    /*
	     * Since asynchronous evaluations are guaranteed by the {@link AbstractSyncTargetAlgorithmEvaluator}, but the SATFCTAE is inherently synchronous,
	     * we constrained the AbstractSyncTargetAlgorithmEvaluator supertype to only use 2 threads.
	     */
	    
	    super(2);
	    
		if(aSATFCFacade == null)
		{
			throw new IllegalArgumentException("Cannot provide a null SATFC facade.");
		}
		
		if(aStationConfigFolder == null)
		{
			throw new IllegalArgumentException("Cannot provide a null station config folder.");
		}
		
		
		if (!new File(aStationConfigFolder).exists()) {
			throw new IllegalArgumentException(
					"Provided station config folder " + aStationConfigFolder
							+ " does not exist.");
		}

		fStationConfigFolder = aStationConfigFolder;

		fSATFCFacade = aSATFCFacade;

		fObserverThreadPool = Executors.newScheduledThreadPool(2,
				new SequentiallyNamedThreadFactory("SATFC Observer Thread",
						true));
	}

	@Override
	public boolean areRunsObservable() {
		return true;
	}

	@Override
	public boolean areRunsPersisted() {
		return false;
	}

	@Override
	public boolean isRunFinal() {
		return false;
	}

	@Override
	protected void subtypeShutdown() {
		try {
            fSATFCFacade.close();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Could not shutdown SATFC facade.",e);
        }
	}
	
	@Override
	public synchronized List<AlgorithmRunResult> evaluateRun(
			List<AlgorithmRunConfiguration> aRuns,
			final TargetAlgorithmEvaluatorRunObserver aObserver) {

		
		if (!fUniqueSATFCTAESemaphore.tryAcquire()) {
			System.out.println("[WARNING] Multiple SATFC TAEs probably exist, and this implementation does not support concurrent executions.");
			log.warn("Multiple SATFC TAEs probably exist, and this implementation does not support concurrent executions.");
			fUniqueSATFCTAESemaphore.acquireUninterruptibly();
		}
		
		try {
			List<AlgorithmRunResult> results = new ArrayList<AlgorithmRunResult>(
					aRuns.size());

			// Initialize observation structures.
			final Map<AlgorithmRunConfiguration, AlgorithmRunResult> resultMap = Collections.synchronizedMap(new LinkedHashMap<AlgorithmRunConfiguration, AlgorithmRunResult>());
			final Map<AlgorithmRunConfiguration, StopWatch> watchMap = Collections.synchronizedMap(new LinkedHashMap<AlgorithmRunConfiguration, StopWatch>());
			final Map<AlgorithmRunConfiguration, StatusVariableKillHandler> runconfigToKillMap = new ConcurrentHashMap<>();
			
			for (AlgorithmRunConfiguration config : aRuns) {
				StatusVariableKillHandler killHandler = new StatusVariableKillHandler();
				resultMap.put(config, new RunningAlgorithmRunResult(config,0.0, 0.0, 0.0, config.getProblemInstanceSeedPair().getSeed(), 0.0, killHandler));
				runconfigToKillMap.put(config, killHandler);
				watchMap.put(config, new StopWatch());
			}
			
			/*
			 * Observer thread.
			 * 
			 * Provides observation of the run along with estimated wallclock time.
			 * 
			 */
			Runnable observerThread = new SATFCTAEObserverThread(aObserver, resultMap, watchMap, runconfigToKillMap);
			
			//Start observer thread.
			observerThread.run();
			ScheduledFuture<?> observerFuture = fObserverThreadPool.scheduleAtFixedRate(observerThread, 0, 5, TimeUnit.SECONDS);
			
			//Process the runs.
			for (AlgorithmRunConfiguration config : aRuns) {
				
				final ExistingAlgorithmRunResult runResult;
				
				if (!runconfigToKillMap.get(config).isKilled())
				{	
					StopWatch configWatch = watchMap.get(config);
					synchronized (configWatch) {
						configWatch.start();
					}
					
					log.debug("Solving instance corresponding to algo run config \"{}\"",config);
					
					log.debug("Transforming config into SATFC problem...");
					
					// Transform algorithm run configuration to SATFC problem.
					SATFCProblem problem = new SATFCProblem(config);
	
					log.debug("Giving problem to SATFC facade...");
					// Solve the problem.
					SATFCResult result = fSATFCFacade.solve(
							problem.getDomains(),
							problem.getPreviousAssignment(),
							problem.getCutoff(),
							problem.getSeed(),
							fStationConfigFolder + File.separator
									+ problem.getStationConfigFolder()
							);
					
					log.debug("Transforming SATFC facade result to TAE result...");
					runResult = getAlgorithmRunResult(result, config, configWatch, problem.getSeed());
					
				} 
				else
				{
					log.debug("Run has been preemptively killed in SATFC TAE");
					runResult = new ExistingAlgorithmRunResult(
							config, 
							RunStatus.KILLED,
							0, 
							0,
							0,
							config.getProblemInstanceSeedPair().getSeed(),
							"Killed Preemptively before starting in SATFC TAE by Steve Ramage, Handsome Developer",
							0);
				}

				resultMap.put(config, runResult);

				results.add(runResult);
			}

			observerFuture.cancel(false);

			return results;
		} 
		finally 
		{
			fUniqueSATFCTAESemaphore.release();
		}
	}
	
	/**
	 * Creates the algorithm run result corresponding to the given SATFC result and scenario, and stop the given watch as well.
	 * 
	 * @param result - the SATFC result.
	 * @param config - the config corresponding to the run executed.
	 * @param configWatch - the running watch used for the run.
	 * @param seed - the seed used for the run.
	 * @return the algorithm run result corresponding to the given results and scenario.
	 */
	private static ExistingAlgorithmRunResult getAlgorithmRunResult(SATFCResult result, AlgorithmRunConfiguration config, StopWatch configWatch, long seed)
	{
		// Transform result to algorithm run result.
		final RunStatus status;
		final String additionalRunData;
		switch (result.getResult()) {
			case SAT:
				status = RunStatus.SAT;
				//Send back the witness assignment.
				Map<Integer, Integer> witness = result.getWitnessAssignment();
				StringBuilder sb = new StringBuilder();
				Iterator<Entry<Integer, Integer>> entryIterator = witness.entrySet().iterator();
				while (entryIterator.hasNext()) {
					
					Entry<Integer, Integer> entry = entryIterator.next();
					
					int stationID = entry.getKey();
					int channel = entry.getValue();

					sb.append(stationID + "=" + channel);

					if (entryIterator.hasNext()) {
						sb.append(",");
					}
				}
				additionalRunData = sb.toString();
				break;
			case UNSAT:
				status = RunStatus.UNSAT;
				additionalRunData = "";
				break;
			case TIMEOUT:
				status = RunStatus.TIMEOUT;
				additionalRunData = "";
				break;
			default:
				status = RunStatus.CRASHED;
				additionalRunData = "";
				break;
		}

		synchronized (configWatch) {
			configWatch.stop();
			return new ExistingAlgorithmRunResult(
					config, 
					status,
					result.getRuntime(), 
					result.getRuntime(),
					result.getRuntime(),
					seed,
					additionalRunData,
					configWatch.time()/1000.0);
		}
	}
	
	
	
	/**
	 * Observer runnable that provides ongoing runs with estimate wall clock time to use the real observer on.
	 *  
	 * @author afrechet
	 *
	 */
	@Data
	private static class SATFCTAEObserverThread implements Runnable
	{
		private final TargetAlgorithmEvaluatorRunObserver fObserver;
		
		private final Map<AlgorithmRunConfiguration,AlgorithmRunResult> fResultMap;
		private final Map<AlgorithmRunConfiguration,StopWatch> fWatchMap;
		private final Map<AlgorithmRunConfiguration, StatusVariableKillHandler> fRunconfigToKillMap;
		
		@Override
		public synchronized void run() {
		    
		    //Update current status for wallclock time.
			List<AlgorithmRunResult> currentResults = new ArrayList<AlgorithmRunResult>(fResultMap.size());

			for (AlgorithmRunConfiguration config : fResultMap.keySet())
			{
				AlgorithmRunResult result = fResultMap.get(config);
				if (result.getRunStatus().equals(RunStatus.RUNNING))
				{
					StopWatch configWatch = fWatchMap.get(config);

					synchronized (configWatch)
					{
						currentResults.add(new RunningAlgorithmRunResult(
										config,
										0.0,
										0.0,
										0.0,
										config.getProblemInstanceSeedPair().getSeed(),
										configWatch.time()/1000.0,
										fRunconfigToKillMap.get(config)));
					}
				} 
				else
				{
					currentResults.add(result);
				}
			}
			
			//Trigger observer.
			fObserver.currentStatus(currentResults);
		}
	}

	/**
	 * Conversion object between a SATFC problem specified in an algorithm run
	 * configuration object and what the SATFC facade requires.
	 * 
	 * @author afrechet
	 */
	@Data
	private static class SATFCProblem {
		
		private final ImmutableMap<Integer, Set<Integer>> domains;
		private final ImmutableMap<Integer, Integer> previousAssignment;
		private final double cutoff;
		private final long seed;
		private final String stationConfigFolder;

		public SATFCProblem(AlgorithmRunConfiguration aConfig){

			// Make sure the algorithm execution config is for a SATFC
			// problem.
			if (!aConfig.getAlgorithmExecutionConfiguration()
					.getTargetAlgorithmExecutionContext()
					.containsKey(SATFC_CONTEXT_KEY)) {
				throw new TargetAlgorithmAbortException(
						"Provided algorithm execution config is not meant for a SATFC TAE.");
			}
			
			//Read the straightforward parameters from the problem instance.
			
			cutoff = aConfig.getCutoffTime();

			ProblemInstanceSeedPair pisp = aConfig
					.getProblemInstanceSeedPair();

			seed = pisp.getSeed();

			//Read the feasibility checking instance from the additional run info.
			
			ProblemInstance instance = pisp.getProblemInstance();
			
			//String instanceName = instance.getInstanceName();
			
			String instanceString = instance.getInstanceSpecificInformation();
			String[] instanceParts = instanceString.split("_");
			
			if(instanceParts.length <=1)
			{
				throw new IllegalArgumentException("Instance string is not a valid SATFC TAE instance:\n "+instanceString);
			}
			
			stationConfigFolder = instanceParts[0];
			
			Map<Integer,Set<Integer>> tempdomains = new HashMap<Integer,Set<Integer>>();
			Map<Integer,Integer> temppreviousassignment = new HashMap<Integer,Integer>();
			
			for(int i=1;i<instanceParts.length;i++)
			{
				String stationString = instanceParts[i];
				String[] stationParts = stationString.split(";");
				
				if(stationParts.length!=3)
				{
					throw new IllegalArgumentException("String representing station info is not formatted correctly:\n \""+stationString+"\"");
				}
				
				int stationID = Integer.valueOf(stationParts[0]);
				int previousChannel = Integer.valueOf(stationParts[1]);
				
				if(previousChannel>0)
				{
					temppreviousassignment.put(stationID, previousChannel);
				}
				
				String channelsString = stationParts[2];
				String[] channelsParts = channelsString.split(",");
				
				Set<Integer> domain = new HashSet<Integer>();
				
				for(int j=0;j<channelsParts.length;j++)
				{
					int channel = Integer.valueOf(channelsParts[j]);
					domain.add(channel);
				}
				
				tempdomains.put(stationID, domain);
			}
			
			domains = ImmutableMap.copyOf(tempdomains);
			previousAssignment = ImmutableMap.copyOf(temppreviousassignment);
		}
	}
	
	
}
