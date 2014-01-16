package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental;

import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import ca.ubc.cs.beta.aclib.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries.ClaspLibrary;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

/**
 * Implements a SAT solver using the jnaclasplibrary.so.  It gracefully handles thread interruptions while solve() is executing
 * and returns a null answer in that case.
 * @author gsauln, afrechet
 */
public class ClaspSATSolver extends AbstractCompressedSATSolver
{
	
	private static Logger log = LoggerFactory.getLogger(ClaspSATSolver.class);
	
	private ClaspLibrary fClaspLibrary;
	private String fParameters;
	private int fMaxArgs;
	private Watch fSolveTimeStopWatch = new Watch();
	private final AtomicBoolean fInterrupt = new AtomicBoolean(false);
	
	public ClaspSATSolver(String libraryPath, String parameters)
	{
		init(libraryPath, parameters, 128);
	}
	
	public ClaspSATSolver(String libraryPath, String parameters, int maxArgs)
	{
		log.info("Building a Clasp solver from library {} and configuration {}.",libraryPath,parameters);
		
		init(libraryPath, parameters, maxArgs);
	}

	private void init(String libraryPath, String parameters, int maxArgs)
	{
		// load the library
		fClaspLibrary = (ClaspLibrary) Native.loadLibrary(libraryPath, ClaspLibrary.class);
		fMaxArgs = maxArgs;
		fParameters = parameters;
		
		// set the info about parameters, throw an exception if seed is contained.
		if (parameters.contains("--seed"))
		{
			throw new IllegalArgumentException("The parameter string cannot contain a seed as it is given upon a call to solve!");
		}
		
		// check if the configuration is valid.
		String params = fParameters+" --seed=1";
		Pointer config = fClaspLibrary.createConfig(params, params.length(), fMaxArgs);
		try {
			int status = fClaspLibrary.getConfigStatus(config);
			if (status == 2)
			{
				String configError = fClaspLibrary.getConfigErrorMessage(config);
				String claspError = fClaspLibrary.getConfigClaspErrorMessage(config);
				String error = configError + "\n" + claspError;
				throw new IllegalArgumentException(error);
			}
		}
		finally 
		{
			fClaspLibrary.destroyConfig(config);
		}
	}
	
	/*
	 * Note that the fInterrupt boolean is shared by all solve jobs, so would have to be modified to make a parallel solver.
	 * 
	 * (non-Javadoc)
	 * @see ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ISATSolver#solve(ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF, double, long)
	 */
	@Override
	public SATSolverResult solve(CNF aCNF, ITerminationCriterion aTerminationCriterion, long aSeed) 
	{	
		// create the facade
		final Pointer facade = fClaspLibrary.createFacade();
		
		// Create the configuration object
		// the construction of the config should always work as it as been checked in the constructor.
		int seed = (new Random(aSeed)).nextInt();
		String params = fParameters+" --seed="+seed;
		Pointer config = fClaspLibrary.createConfig(params, params.length(), fMaxArgs);
		
		// create the problem
		Pointer problem = fClaspLibrary.createProblem(aCNF.toDIMACS(null));
		final Pointer result = fClaspLibrary.createResult();
		final AtomicBoolean timedOut = new AtomicBoolean(false);
		
		
		
		double cutoff = aTerminationCriterion.getRemainingTime();
		ScheduledExecutorService timerService = Executors.newScheduledThreadPool(2,new SequentiallyNamedThreadFactory("Clasp SAT Solver Timers", true));
		
		// Launches a timer that will set the interrupt flag of the result object to true after aCutOff seconds. 
		timerService.schedule(new Runnable(){
			@Override
			public void run() {
				fClaspLibrary.interrupt(facade);
				timedOut.set(true);
			}}, (long) cutoff, TimeUnit.SECONDS);
		// listens for thread interruption every 1 seconds, if the thread is interrupted, interrupt the library and return gracefully
		//while returning null (free library memory, etc.)
		timerService.scheduleWithFixedDelay(new Runnable(){
			@Override
			public void run() {
				if (fInterrupt.get())
				{
					fClaspLibrary.interrupt(facade);
				}
				
			}}, 1, 1, TimeUnit.SECONDS);

		// Start solving
		log.debug("Send problem to clasp cutting off after "+cutoff+"s");
		
		fSolveTimeStopWatch.start();
		fClaspLibrary.jnasolve(facade, problem, config, result);
		
		//Terminate timing tasks.
		timerService.shutdown();
		try {
			if(!timerService.awaitTermination(3, TimeUnit.SECONDS))
			{
				throw new IllegalStateException("Could not terminate clasp timer tasks within 3 seconds.");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new IllegalStateException("Interrupted while trying to terminate clasp timer tasks.");
		}
		
		long timeInMillis = fSolveTimeStopWatch.stop();
		
		ClaspResult claspResult = getSolverResult(fClaspLibrary, result, timedOut, fInterrupt, timeInMillis);
		aTerminationCriterion.notifyEvent(claspResult.getRuntime());
		
		HashSet<Literal> assignment = parseAssignment(claspResult.getAssignment());

		//clears memory
		fClaspLibrary.destroyFacade(facade);
		fClaspLibrary.destroyConfig(config);
		fClaspLibrary.destroyProblem(problem);
		fClaspLibrary.destroyResult(result);
		
		return new SATSolverResult(claspResult.getSATResult(), claspResult.getRuntime(), assignment);
	}

	private HashSet<Literal> parseAssignment(int[] assignment)
	{
		HashSet<Literal> set = new HashSet<Literal>();
		for (int i = 1; i < assignment[0]; i++)
		{
			int intLit = assignment[i];
			int var = Math.abs(intLit);
			boolean sign = intLit > 0;
			Literal aLit = new Literal(var, sign);
			set.add(aLit);
		}
		return set;
	}
	
	@Override
	public void notifyShutdown() {}

	@Override
	public void interrupt() throws UnsupportedOperationException 
	{
		fInterrupt.set(true);
	}
	
	/**
	 * Extract solver result from JNA Clasp library.
	 * @return an increment result.
	 */
	public static ClaspResult getSolverResult(ClaspLibrary library, 
												Pointer JNAResult, 
												AtomicBoolean timedOut, 
												AtomicBoolean interrupted, 
												long timeInMillis)
	{
		double runtime = (double)timeInMillis/1000.0;
		
		SATResult satResult;
		int[] assignment = {0};
		int state = library.getResultState(JNAResult);
		
		/*
		 * The order in which the status flags are checked is important.
		 * The timeout flag needs to be checked first because a timed out job will/can
		 * be both timed out AND interrupted.
		 */
		if (timedOut.compareAndSet(true, false))
		{
			satResult = SATResult.TIMEOUT;
			interrupted.set(false);
		}
		else if (interrupted.compareAndSet(true, false))
		{
			satResult = SATResult.INTERRUPTED;
		}
		else 
		{
			if (state == 0)
			{
				satResult = SATResult.UNSAT;
			}
			else if (state == 1)
			{
				satResult = SATResult.SAT;
				IntByReference pRef = library.getResultAssignment(JNAResult); 
				int size = pRef.getValue();
				assignment = pRef.getPointer().getIntArray(0, size);
			}
			else 
			{
				satResult = SATResult.CRASHED;
				log.error("Clasp crashed!");
			}
		}

		return new ClaspResult(satResult, assignment, runtime);
	}

}
