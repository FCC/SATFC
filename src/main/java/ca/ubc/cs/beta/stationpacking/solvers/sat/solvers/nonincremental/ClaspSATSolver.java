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
package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aeatk.misc.returnvalues.AEATKReturnValues;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries.ClaspLibrary;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * Implements a SAT solver using the jnaclasplibrary.so.  It gracefully handles thread interruptions while solve() is executing
 * and returns a null answer in that case.
 * @author gsauln, afrechet, seramage
 */
public class ClaspSATSolver extends AbstractCompressedSATSolver
{
	
	private static Logger log = LoggerFactory.getLogger(ClaspSATSolver.class);
	
	private ClaspLibrary fClaspLibrary;
	private String fParameters;
	private int fMaxArgs;
	private final AtomicBoolean fInterrupt = new AtomicBoolean(false);
	
	private final ScheduledExecutorService fTimerService = Executors.newScheduledThreadPool(4,new SequentiallyNamedThreadFactory("Clasp SAT Solver Timers", true));
	
	
	/**
	 * Integer flag that we use to keep track of our current request, cutoff and timer threads will only execute if
	 * this matches the id when they started.
	 */
	private final AtomicLong currentRequestID = new AtomicLong(1);
	
	/**
	 * Constructs a clasp SAT solver from a clasp library and a parameter string.
	 * @param libraryPath - a path to the clasp library.
	 * @param parameters - a string of clasp parameters.
	 */
	public ClaspSATSolver(String libraryPath, String parameters)
	{
		init(libraryPath, parameters, 128);
	}
	
//	private ClaspSATSolver(String libraryPath, String parameters, int maxArgs)
//	{
//		log.info("Building a Clasp solver from library {} and configuration {}.",libraryPath,parameters);
//		
//		init(libraryPath, parameters, maxArgs);
//	}

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
	public SATSolverResult solve(CNF aCNF, final ITerminationCriterion aTerminationCriterion, long aSeed) 
	{	
		Watch watch = Watch.constructAutoStartWatch();
		
		final long MY_REQUEST_ID = currentRequestID.incrementAndGet();
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
		
		watch.stop();
		double preTime = watch.getElapsedTime();
		
		watch.reset();
		watch.start();
		
		final double cutoff = aTerminationCriterion.getRemainingTime();
		
		if(cutoff <=0)
		{
		    log.debug("All time spent.");
		    return new SATSolverResult(SATResult.TIMEOUT, preTime, new HashSet<Literal>());
		}
		// Launches a timer that will set the interrupt flag of the result object to true after aCutOff seconds. 
		Future<?> timeoutFuture = fTimerService.schedule(
		        new Runnable(){
            			@Override
            			public void run() {
            			    
                            if(MY_REQUEST_ID == currentRequestID.get())
                            {
                				log.trace("Interrupting clasp as we are past cutoff of {} s.",cutoff);
                				timedOut.set(true);
                				fClaspLibrary.interrupt(facade);
                				return;
                            } 
            			    
            			}
        		    },(long) (cutoff*1000), TimeUnit.MILLISECONDS);
		
		// listens for thread interruption every 1 seconds, if the thread is interrupted, interrupt the library and return gracefully
		//while returning null (free library memory, etc.)
		
		final int SCHEDULING_FREQUENCY_IN_SECONDS = 1;
		Future<?> interruptFuture = fTimerService.schedule(
		        new Runnable(){
        			@Override
        			public void run() 
        			{       			    
            			    if(MY_REQUEST_ID == currentRequestID.get())
            			    {
                				if (fInterrupt.get())
                				{
                					log.trace("Clasp interruption was triggered.");
                					fClaspLibrary.interrupt(facade);
                					return;
                				} else
                				{
                					fTimerService.schedule(this, SCHEDULING_FREQUENCY_IN_SECONDS, TimeUnit.SECONDS);
                				}
            			    }
        			}
        		},SCHEDULING_FREQUENCY_IN_SECONDS,TimeUnit.SECONDS);
		
		//launches a suicide SATFC time that just kills everything if it finishes and we're still on the same job.
		final int SUICIDE_GRACE_IN_SECONDS = 5*60;
		Future<?> suicideFuture = fTimerService.schedule(
		        new Runnable(){
		            
		            @Override
		            public void run()
		            {
		                if(MY_REQUEST_ID == currentRequestID.get())
		                {
		                    log.error("Clasp has spent {} more seconds than expected ({}) on current run, killing everything (i.e. System.exit(1) ).",SUICIDE_GRACE_IN_SECONDS,cutoff);
		                    System.exit(AEATKReturnValues.OH_THE_HUMANITY_EXCEPTION);
		                }
		            }
		        }, (long) cutoff + SUICIDE_GRACE_IN_SECONDS, TimeUnit.SECONDS);
		// Start solving
		log.debug("Send problem to clasp cutting off after "+cutoff+"s");
		
		fClaspLibrary.jnasolve(facade, problem, config, result);
		log.debug("Came back from clasp.");
		
		watch.stop();
		double runtime = watch.getElapsedTime();
		watch.reset();
		watch.start();
		
		
		ClaspResult claspResult = getSolverResult(fClaspLibrary, result, timedOut, fInterrupt, runtime);
		
		log.trace("Post time to clasp result obtained: {} s.",watch.getElapsedTime());
		
		timeoutFuture.cancel(true);
		log.trace("Post time to timeout future cancellation: {} s.",watch.getElapsedTime());
		interruptFuture.cancel(true);
		log.trace("Post time to interrupt future cancellation: {} s.",watch.getElapsedTime());
		
		
		HashSet<Literal> assignment = parseAssignment(claspResult.getAssignment());
		log.trace("Post time to to assignment obtained: {} s.",watch.getElapsedTime());

		//clears memory
		fClaspLibrary.destroyFacade(facade);
		log.trace("Post time to facade destroyed: {} s.",watch.getElapsedTime());
		fClaspLibrary.destroyConfig(config);
		log.trace("Post time to config destroyed: {} s.",watch.getElapsedTime());
		fClaspLibrary.destroyProblem(problem);
		log.trace("Post time to problem destroyed: {} s.",watch.getElapsedTime());
		fClaspLibrary.destroyResult(result);
		log.trace("Post time to result destroyed: {} s.",watch.getElapsedTime());
		
		watch.stop();
		double postTime = watch.getElapsedTime();
		
		log.trace("Total post time: {} s.", postTime);
		if(postTime > 60)
		{
			log.error("Clasp SAT solver post solving time was greater than 1 minute, something wrong must have happenned.");
		}
		
		//We only increment
		log.debug("Incrementing job index.");
		currentRequestID.incrementAndGet();
		
		log.debug("Cancelling suicide future.");
		suicideFuture.cancel(true);
		
		final SATSolverResult output = new SATSolverResult(claspResult.getSATResult(), claspResult.getRuntime()+preTime+postTime, assignment); 
		log.trace("Returning result: {}.",output);
		return output;
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
	public void notifyShutdown() 
	{
	    //No shutdown necessary.
	}

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
												double runtime)
	{
		
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
			interrupted.set(false);
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
