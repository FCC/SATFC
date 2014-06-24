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
import java.util.concurrent.atomic.AtomicReference;

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

/**
 * Target algorithm evaluator that wraps around the SATFC facade and only
 * solves, synchronously, station packing feasibility problems.
 * 
 * @author afrechet, seramage
 */
public class SATFCTargetAlgorithmEvaluator extends
		AbstractSyncTargetAlgorithmEvaluator {
	
	private static final Logger log = LoggerFactory.getLogger(SATFCTargetAlgorithmEvaluator.class);
	
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
		fSATFCFacade.close();
	}
	
	@Override
	public synchronized List<AlgorithmRunResult> evaluateRun(
			List<AlgorithmRunConfiguration> aRuns,
			final TargetAlgorithmEvaluatorRunObserver aObserver) {

		try {
			if (!fUniqueSATFCTAESemaphore.tryAcquire()) {
				System.out.println("[WARNING] Multiple SATFC TAEs probably exist, and this implementation does not support concurrent executions.");
				fUniqueSATFCTAESemaphore.acquireUninterruptibly();
			}

			List<AlgorithmRunResult> results = new ArrayList<AlgorithmRunResult>(
					aRuns.size());

			// Initialize observation structures.
			final Map<AlgorithmRunConfiguration, AlgorithmRunResult> resultMap = Collections.synchronizedMap(new LinkedHashMap<AlgorithmRunConfiguration, AlgorithmRunResult>());
			final Map<AlgorithmRunConfiguration, StopWatch> watchMap = Collections.synchronizedMap(new LinkedHashMap<AlgorithmRunConfiguration, StopWatch>());

			final Map<AlgorithmRunConfiguration, StatusVariableKillHandler> runconfigToKillMap = new ConcurrentHashMap<>();
			
			AtomicReference<AlgorithmRunConfiguration> currentRun = new AtomicReference<AlgorithmRunConfiguration>();
			
			for (AlgorithmRunConfiguration config : aRuns) {
				StatusVariableKillHandler killHandler = new StatusVariableKillHandler();
				resultMap.put(config, new RunningAlgorithmRunResult(config,0.0, 0.0, 0.0, config.getProblemInstanceSeedPair().getSeed(), 0.0, killHandler));
				runconfigToKillMap.put(config, killHandler);
				watchMap.put(config, new StopWatch());
			}
			
			
			
			//Observer thread.
			Runnable observerThread = new Runnable() {
				@Override
				public synchronized void run() {
				    
				    //Update current status for wallclock time.
					List<AlgorithmRunResult> currentResults = new ArrayList<AlgorithmRunResult>(resultMap.size());

					for (AlgorithmRunConfiguration config : resultMap.keySet())
					{
						AlgorithmRunResult result = resultMap.get(config);
						if (result.getRunStatus().equals(RunStatus.RUNNING))
						{
							StopWatch configWatch = watchMap.get(config);

							synchronized (configWatch)
							{
								currentResults.add(new RunningAlgorithmRunResult(
												config,
												0.0,
												0.0,
												0.0,
												config.getProblemInstanceSeedPair().getSeed(),
												configWatch.time()/1000.0,
												runconfigToKillMap.get(config)));
							}
						} 
						else
						{
							currentResults.add(result);
						}
					}
					
					aObserver.currentStatus(currentResults);
				}
			};

			
			//Start observer thread.
			observerThread.run();
			ScheduledFuture<?> observerFuture = fObserverThreadPool.scheduleAtFixedRate(observerThread, 0, 15,	TimeUnit.SECONDS);

			for (AlgorithmRunConfiguration config : aRuns) {
				
				ExistingAlgorithmRunResult runResult;
				
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
							problem.getStations(),
							problem.getChannels(),
							problem.getReducedDomains(),
							problem.getPreviousAssignment(),
							problem.getCutoff(),
							problem.getSeed(),
							fStationConfigFolder + File.separator
									+ problem.getStationConfigFolder());
	
					log.debug("Transforming SATFC facade result to TAE result...");
					// Transform result to algorithm run result.
					RunStatus status;
					String additionalRunData;
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
						runResult = new ExistingAlgorithmRunResult(
								config, 
								status,
								result.getRuntime(), 
								result.getRuntime(),
								result.getRuntime(),
								problem.getSeed(),
								additionalRunData,
								configWatch.time()/1000.0);
					}
				} else
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
		} finally {
			fUniqueSATFCTAESemaphore.release();
		}
	}

	/**
	 * Conversion object between a SATFC problem specified in an algorithm run
	 * configuration object and what the SATFC facade requires.
	 * 
	 * @author afrechet
	 */
	private static class SATFCProblem {
		
		private final Map<Integer, Set<Integer>> fDomains;
		private final Map<Integer, Integer> fPreviousAssignment;
		private final double fCutoff;
		private final long fSeed;
		private final String fStationConfigFolder;

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
			
			fCutoff = aConfig.getCutoffTime();

			ProblemInstanceSeedPair pisp = aConfig
					.getProblemInstanceSeedPair();

			fSeed = pisp.getSeed();

			//Read the feasibility checking instance from the additional run info.
			
			ProblemInstance instance = pisp.getProblemInstance();
			
			//String instanceName = instance.getInstanceName();
			
			String instanceString = instance.getInstanceSpecificInformation();
			String[] instanceParts = instanceString.split("_");
			
			if(instanceParts.length <=1)
			{
				throw new IllegalArgumentException("Instance string is not a valid SATFC TAE instance:\n "+instanceString);
			}
			
			fStationConfigFolder = instanceParts[0];
			
			fDomains = new HashMap<Integer,Set<Integer>>();
			fPreviousAssignment = new HashMap<Integer,Integer>();
			
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
					fPreviousAssignment.put(stationID, previousChannel);
				}
				
				String channelsString = stationParts[2];
				String[] channelsParts = channelsString.split(",");
				
				Set<Integer> domain = new HashSet<Integer>();
				
				for(int j=0;j<channelsParts.length;j++)
				{
					int channel = Integer.valueOf(channelsParts[j]);
					domain.add(channel);
				}
				
				fDomains.put(stationID, domain);
			}
		}

		public Set<Integer> getStations() {
			return Collections.unmodifiableSet(fDomains.keySet());
		}

		public Set<Integer> getChannels() {
			Set<Integer> channels = new HashSet<Integer>();
			for (Set<Integer> domain : fDomains.values()) {
				channels.addAll(domain);
			}
			return Collections.unmodifiableSet(channels);
		}

		public Map<Integer, Set<Integer>> getReducedDomains() {
			return Collections.unmodifiableMap(fDomains);
		}

		public Map<Integer, Integer> getPreviousAssignment() {
			return Collections.unmodifiableMap(fPreviousAssignment);
		}

		public double getCutoff() {
			return fCutoff;
		}

		public long getSeed() {
			return fSeed;
		}

		public String getStationConfigFolder() {
			return fStationConfigFolder;
		}

	}
}
