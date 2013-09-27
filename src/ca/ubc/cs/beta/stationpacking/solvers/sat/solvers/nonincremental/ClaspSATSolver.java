package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental;

import java.util.HashSet;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Litteral;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries.ClaspLibrary;
import ca.ubc.cs.beta.stationpacking.utils.Holder;

/**
 * Implements a SAT solver using the jnaclasplibrary.so.  It gracefully handles thread interruptions while solve() is executing
 * and returns a null answer in that case.
 * @author gsauln
 *
 */
public class ClaspSATSolver extends AbstractCompressedSATSolver
{
	
	private static Logger log = LoggerFactory.getLogger(ClaspSATSolver.class);
	
	private ClaspLibrary fClaspLibrary;
	private String fParameters;
	private int fMaxArgs;
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
	public SATSolverResult solve(CNF aCNF, double aCutoff, long aSeed) {
		long time1 = System.currentTimeMillis();
		
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
		final Holder<Boolean> timeout = new Holder<Boolean>();
		timeout.set(false);
		
		// Launches a timer that will set the interrupt flag of the result object to true after aCutOff seconds.
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				fClaspLibrary.interrupt(facade);
				timeout.set(true);
			}
		}, (long)(aCutoff*1000));
		// listens for thread interruption every 0.1 seconds, if the thread is interrupted, interrupt the library and return gracefully
		// while returning null (free library memory, etc.)
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (fInterrupt.get())
				{
					fClaspLibrary.interrupt(facade);
				}
			}
		}, 0, 100);

		// Start solving
		fClaspLibrary.jnasolve(facade, problem, config, result);
		timer.cancel();
		
		SATResult satResult = null;
		HashSet<Litteral> assignment = new HashSet<Litteral>();
		int state = fClaspLibrary.getResultState(result);
		
		if (fInterrupt.compareAndSet(true, false))
		{
			satResult = SATResult.INTERRUPTED;
		}
		else if (timeout.get())
		{
			satResult = SATResult.TIMEOUT;
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
				assignment = parseAssignment(fClaspLibrary.getResultAssignment(result));
			}
			else 
			{
				satResult = SATResult.CRASHED;
				log.error("Clasp crashed!");
			}
		}

		//clears memory
		fClaspLibrary.destroyFacade(facade);
		fClaspLibrary.destroyConfig(config);
		fClaspLibrary.destroyProblem(problem);
		fClaspLibrary.destroyResult(result);
		
		long time2 = System.currentTimeMillis();
		
		// should be null only if thread was interrupted.
		SATSolverResult answer = null;
		if (satResult != null)
		{
			answer = new SATSolverResult(satResult, (time2-time1)/1000.0, assignment);
		}
		
		return answer;
	}

	public static HashSet<Litteral> parseAssignment(String assignment)
	{
		HashSet<Litteral> set = new HashSet<Litteral>();
		StringTokenizer strtok = new StringTokenizer(assignment, ";");
		while (strtok.hasMoreTokens())
		{
			int intLit = Integer.valueOf(strtok.nextToken());
			int var = Math.abs(intLit);
			boolean sign = intLit > 0;
			Litteral aLit = new Litteral(var, sign);
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

}
