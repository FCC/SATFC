package ca.ubc.cs.beta.stationpacking.tae;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
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
 * @author afrechet
 */
public class SATFCTargetAlgorithmEvaluator extends
		AbstractSyncTargetAlgorithmEvaluator {

	public final static String SATFCONTEXTKEY = "SATFC_CONTEXT";

	private final SATFCFacade fSATFCFacade;

	private final String fInstanceDirectory;
	private final String fStationConfigFolder;

	private final ScheduledExecutorService fObserverThreadPool;

	private static final Semaphore fUniqueSATFCTAESemaphore = new Semaphore(1);

	public SATFCTargetAlgorithmEvaluator(SATFCFacade aSATFCFacade,
			String aInstanceDirectory, String aStationConfigFolder) {
		if (!new File(aInstanceDirectory).exists()) {
			throw new IllegalArgumentException("Provided instance directory "
					+ aInstanceDirectory + " does not exist.");
		}

		fInstanceDirectory = aInstanceDirectory;

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
			List<AlgorithmRunConfiguration> arg0,
			final TargetAlgorithmEvaluatorRunObserver arg1) {

		try {
			if (!fUniqueSATFCTAESemaphore.tryAcquire()) {
				System.out
						.println("[WARNING] Multiple SATFC TAEs probably exist, and this implementation does not support concurrent executions.");
				fUniqueSATFCTAESemaphore.acquireUninterruptibly();
			}

			List<AlgorithmRunResult> results = new ArrayList<AlgorithmRunResult>(
					arg0.size());

			// Initialize observation structures.
			final Map<AlgorithmRunConfiguration, AlgorithmRunResult> resultMap = Collections
					.synchronizedMap(new LinkedHashMap<AlgorithmRunConfiguration, AlgorithmRunResult>());
			final Map<AlgorithmRunConfiguration, StopWatch> watchMap = Collections
					.synchronizedMap(new LinkedHashMap<AlgorithmRunConfiguration, StopWatch>());

			for (AlgorithmRunConfiguration config : arg0) {
				resultMap.put(config, new ExistingAlgorithmRunResult(config,
						RunStatus.RUNNING, 0.0, 0.0, 0.0, config
								.getProblemInstanceSeedPair().getSeed(), 0.0));
				watchMap.put(config, new StopWatch());
			}

			// Start observer thread.
			Runnable observerThread = new Runnable() {
				@Override
				public void run() {

					List<AlgorithmRunResult> currentResults = new ArrayList<AlgorithmRunResult>(
							resultMap.size());

					for (AlgorithmRunConfiguration config : resultMap.keySet()) {
						AlgorithmRunResult result = resultMap.get(config);
						if (result.getRunStatus().equals(RunStatus.RUNNING)) {
							StopWatch configWatch = watchMap.get(config);

							synchronized (configWatch) {
								currentResults
										.add(new ExistingAlgorithmRunResult(
												config,
												RunStatus.RUNNING,
												0.0,
												0.0,
												0.0,
												config.getProblemInstanceSeedPair()
														.getSeed(), configWatch
														.time()));
							}
						}
					}

					arg1.currentStatus(currentResults);
				}
			};

			ScheduledFuture<?> observerFuture = fObserverThreadPool
					.scheduleAtFixedRate(observerThread, 0, 15,
							TimeUnit.SECONDS);

			for (AlgorithmRunConfiguration config : arg0) {

				StopWatch configWatch = watchMap.get(config);
				synchronized (configWatch) {
					configWatch.start();
				}

				// Transform algorithm run configuration to SATFC problem.
				SATFCProblem problem;
				try {
					problem = new SATFCProblem(config, fInstanceDirectory);
				} catch (IllegalArgumentException
						| TargetAlgorithmAbortException | IOException e) {
					e.printStackTrace();
					throw new TargetAlgorithmAbortException(
							"Exception encountered while creating a SATFC problem from an algorithm run configuration.",
							e);
				}

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

				// Transform result to algorithm run result.
				RunStatus status;
				String additionalRunData;
				switch (result.getResult()) {
				case SAT:
					status = RunStatus.SAT;

					Map<Integer, Integer> witness = result
							.getWitnessAssignment();
					StringBuilder sb = new StringBuilder();
					Iterator<Entry<Integer, Integer>> entryIterator = witness
							.entrySet().iterator();
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

				ExistingAlgorithmRunResult runResult;
				synchronized (configWatch) {
					configWatch.stop();
					runResult = new ExistingAlgorithmRunResult(config, status,
							result.getRuntime(), result.getRuntime(),
							result.getRuntime(),

							problem.getSeed(), additionalRunData,
							configWatch.time());
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
	private class SATFCProblem {
		private Map<Integer, Set<Integer>> fDomains;
		private Map<Integer, Integer> fPreviousAssignment;
		private double fCutoff;
		private long fSeed;
		private String fStationConfigFolder;

		public SATFCProblem(AlgorithmRunConfiguration aConfig,
				String aInstanceDirectory) throws IOException,
				FileNotFoundException, IllegalArgumentException,
				TargetAlgorithmAbortException {
			// Make sure the algorithm execution config is for a SATFC problem.
			if (!aConfig.getAlgorithmExecutionConfiguration()
					.getTargetAlgorithmExecutionContext()
					.containsKey(SATFCONTEXTKEY)) {
				throw new TargetAlgorithmAbortException(
						"Provided algorithm execution config is not meant for a SATFC TAE.");
			}

			fCutoff = aConfig.getCutoffTime();

			ProblemInstanceSeedPair pisp = aConfig.getProblemInstanceSeedPair();

			fSeed = pisp.getSeed();

			ProblemInstance instance = pisp.getProblemInstance();
			String instanceFilename = aInstanceDirectory + File.separator
					+ instance.getInstanceName();

			List<String> lines = FileUtils
					.readLines(new File(instanceFilename));

			// if the instance file has not been read properly, try again using
			// a geometric backoff
			// TODO: figure out how to do this properly
			if (lines.size() <= 1) {
				reAttemptSATFCProblem(aConfig, aInstanceDirectory, 0);
				return;
			}

			Iterator<String> linesIterator = lines.iterator();

			// Station config information is the first line of the file.
			fStationConfigFolder = linesIterator.next();

			fDomains = new HashMap<Integer, Set<Integer>>();
			fPreviousAssignment = new HashMap<Integer, Integer>();

			while (linesIterator.hasNext()) {
				String line = linesIterator.next();

				String[] lineParts = StringUtils.split(line, ";");

				// if the instance file has not been read properly, try again
				// using
				// a geometric backoff (recursive)
				// TODO: figure how to do this properly
				if (lineParts.length != 3) {
					reAttemptSATFCProblem(aConfig, aInstanceDirectory, 0);
					return;
				}

				int stationID = Integer.valueOf(lineParts[0]);

				int previousChannel = Integer.valueOf(lineParts[1]);
				if (previousChannel > 0) {
					fPreviousAssignment.put(stationID, previousChannel);
				}

				Set<Integer> domain = new HashSet<Integer>();
				String[] domainParts = StringUtils.split(lineParts[2], ",");
				for (int i = 0; i < domainParts.length; i++) {
					domain.add(Integer.valueOf(domainParts[i]));
				}

				fDomains.put(stationID, domain);
			}
		}

		/**
		 * will wait 2**c, and then try reading the file again. Max wait is 32
		 * seconds (maybe should be more?). Will wait 2**c, and then try reading
		 * the file again.
		 * 
		 * @param aConfig
		 * @param aInstanceDirectory
		 * @param numberOfReadAttemps
		 * @throws IOException
		 */
		private void reAttemptSATFCProblem(AlgorithmRunConfiguration aConfig,
				String aInstanceDirectory, int numberOfReadAttemps)
				throws IOException {

			numberOfReadAttemps++;
			try {
				Thread.sleep((int) (Math.pow(2, numberOfReadAttemps) * 1000));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(
						"Thread interupted. Don't know how to handle");
			}

			// Make sure the algorithm execution config is for a SATFC problem.
			if (!aConfig.getAlgorithmExecutionConfiguration()
					.getTargetAlgorithmExecutionContext()
					.containsKey(SATFCONTEXTKEY)) {
				throw new TargetAlgorithmAbortException(
						"Provided algorithm execution config is not meant for a SATFC TAE.");
			}

			fCutoff = aConfig.getCutoffTime();

			ProblemInstanceSeedPair pisp = aConfig.getProblemInstanceSeedPair();

			fSeed = pisp.getSeed();

			ProblemInstance instance = pisp.getProblemInstance();
			String instanceFilename = aInstanceDirectory + File.separator
					+ instance.getInstanceName();

			List<String> lines = FileUtils
					.readLines(new File(instanceFilename));

			// if the instance file has not been read properly, try again using
			// a geometric backoff (recursive)
			if (lines.size() <= 1 && numberOfReadAttemps < 6) {
				reAttemptSATFCProblem(aConfig, aInstanceDirectory,
						numberOfReadAttemps);
				return;
			}
			// after max attempts
			if (lines.size() <= 1) {
				throw new IllegalArgumentException(
						"Not enough lines in the SATFC problem file "
								+ instanceFilename
								+ " to create a SATFC problem. Only "
								+ lines.size() + " lines found.");
			}

			Iterator<String> linesIterator = lines.iterator();

			// Station config information is the first line of the file.
			fStationConfigFolder = linesIterator.next();

			fDomains = new HashMap<Integer, Set<Integer>>();
			fPreviousAssignment = new HashMap<Integer, Integer>();

			while (linesIterator.hasNext()) {
				String line = linesIterator.next();

				String[] lineParts = StringUtils.split(line, ";");

				// if the instance file has not been read properly, try again
				// using a geometric backoff (recursive)
				if (lineParts.length != 3 && numberOfReadAttemps < 6) {
					reAttemptSATFCProblem(aConfig, aInstanceDirectory,
							numberOfReadAttemps);
					return;
				}
				// after max attempts
				if (lineParts.length != 3) {
					throw new IllegalArgumentException("Ill-formatted line "
							+ line + " in SATFC problem file "
							+ instanceFilename + ".");
				}

				int stationID = Integer.valueOf(lineParts[0]);

				int previousChannel = Integer.valueOf(lineParts[1]);
				if (previousChannel > 0) {
					fPreviousAssignment.put(stationID, previousChannel);
				}

				Set<Integer> domain = new HashSet<Integer>();
				String[] domainParts = StringUtils.split(lineParts[2], ",");
				for (int i = 0; i < domainParts.length; i++) {
					domain.add(Integer.valueOf(domainParts[i]));
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
