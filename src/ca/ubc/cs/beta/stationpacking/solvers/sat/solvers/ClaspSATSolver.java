package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers;

import java.util.HashSet;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Litteral;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.CNFCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries.ClaspLibrary;

public class ClaspSATSolver implements ISATSolver
{
	
	private static Logger log = LoggerFactory.getLogger(ClaspSATSolver.class);
	
	ClaspLibrary fClaspLibrary;
	String fParameters;
	int fMaxArgs;
	
	public ClaspSATSolver(String libraryPath, String parameters)
	{
		init(libraryPath, parameters, 128);
	}
	
	public ClaspSATSolver(String libraryPath, String parameters, int maxArgs)
	{
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
	
	protected class Interrupt
	{
		private boolean fInterrupted = false;
		public void setInterrupt()
		{
			fInterrupted = true;
		}
		public boolean getInterrupt()
		{
			return fInterrupted;
		}
	}
	
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
		CNFCompressor compressor = new CNFCompressor();
		Pointer problem = fClaspLibrary.createProblem(compressor.compress(aCNF).toDIMACS(null));
		final Pointer result = fClaspLibrary.createResult();
		final Interrupt interrupt = new Interrupt();
		// Launches a timer that will set the interrupt flag of the result object to true after aCutOff seconds.
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				fClaspLibrary.interrupt(facade);
				interrupt.setInterrupt();
			}
		}, (long)(aCutoff*1000));
		

		// Start solving
		fClaspLibrary.jnasolve(facade, problem, config, result);
		timer.cancel();
		
		SATResult satResult;
		HashSet<Litteral> assignment = new HashSet<Litteral>();
		int state = fClaspLibrary.getResultState(result);
		if (interrupt.getInterrupt())
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
				assignment = parseAssignment(fClaspLibrary.getResultAssignment(result), compressor);
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
		
		SATSolverResult answer = new SATSolverResult(satResult, (time2-time1)/1000.0, assignment);
		
		return answer;
	}

	public static HashSet<Litteral> parseAssignment(String assignment, CNFCompressor compressor)
	{
		HashSet<Litteral> set = new HashSet<Litteral>();
		StringTokenizer strtok = new StringTokenizer(assignment, ";");
		while (strtok.hasMoreTokens())
		{
			int intLit = Integer.valueOf(strtok.nextToken());
			int var = Math.abs(intLit);
			boolean sign = intLit > 0;
			Litteral aLit = new Litteral(compressor.decompress(var), sign);
			set.add(aLit);
		}
		return set;
	}
	
	@Override
	public void notifyShutdown() {}

}
