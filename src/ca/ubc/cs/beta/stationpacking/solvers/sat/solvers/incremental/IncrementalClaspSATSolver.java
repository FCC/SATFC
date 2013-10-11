package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;

import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.ClaspResult;
import ca.ubc.cs.beta.stationpacking.utils.RunnableUtils;

public class IncrementalClaspSATSolver extends AbstractSATSolver {

	private static Logger log = LoggerFactory.getLogger(IncrementalClaspSATSolver.class);
	
	private final BlockingQueue<IncrementalClaspProblem> fProblemQueue;
	private final BlockingQueue<ClaspResult> fAnswerQueue;
	
	private final ExecutorService fExecutorService;
	
	private final IncrementalClaspJNAConsumer fConsumer;
	
	private final IncrementalCompressor fCompressor;
	
	public IncrementalClaspSATSolver(String libraryPath, String parameters, long seed)
	{
		/*
		 * Create encoding structures
		 */
		log.debug("Creating encoding structures.");
		fCompressor = new IncrementalCompressor();
		
		/*
		 * Create consumer.
		 */
		log.debug("Creating Clasp JNA consumer.");
		//Initialize communication queues.
		fProblemQueue = new ArrayBlockingQueue<IncrementalClaspProblem>(1);
		fAnswerQueue = new ArrayBlockingQueue<ClaspResult>(1);
		
		fConsumer = new IncrementalClaspJNAConsumer(libraryPath, parameters, seed, fProblemQueue, fAnswerQueue);
		
		/*
		 * Launch consumer.
		 */
		log.debug("Launching consumer.");
		
		fExecutorService = Executors.newCachedThreadPool();
		
		launchConsumer();
	}
	
	/**
	 * Launch the consumer with the executor service.
	 */
	private void launchConsumer()
	{
		final UncaughtExceptionHandler exceptionHandler = new UncaughtExceptionHandler() 
		{
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				
				e.printStackTrace();
				
				log.error("Thread {} died with an exception ({}).",t.getName(),e.getMessage());
				
				log.error("Stopping service.");
				fExecutorService.shutdownNow();
			}
		};
		
		RunnableUtils.submitRunnable(fExecutorService, exceptionHandler, fConsumer);
	}
	
	
	@Override
	public SATSolverResult solve(CNF aCNF, double aCutoff, long aSeed) {
		
		//Encode the CNF into an IncrementalProblem.
		Pointer problemPointer = fCompressor.compress(aCNF);
		IncrementalClaspProblem problem = new IncrementalClaspProblem(problemPointer,aCutoff, aSeed);
		
		//Give it to the consumer.
		try {
			log.debug("Submitting problem.");
			fProblemQueue.put(problem);
		} catch (InterruptedException e) {
			log.error("Incremental Clasp SAT Solver's put() method was interrupted, propagating interruption ({}).",e.getMessage());
			Thread.currentThread().interrupt();
			return new SATSolverResult(SATResult.INTERRUPTED, 0.0, new HashSet<Literal>());
		}
		
		
		//Wait for an answer.
		ClaspResult answer;
		try {
			answer = fAnswerQueue.take();
			log.debug("Got answer from queue.");
		} catch (InterruptedException e) {
			log.error("Incremental Clasp SAT Solver's take() method was interrupted, propagating interruption ({}).",e.getMessage());
			Thread.currentThread().interrupt();
			return new SATSolverResult(SATResult.INTERRUPTED, 0.0, new HashSet<Literal>());
		}
		
		log.debug("Post-processing result.");
		//Post-process answer for decompression.
		HashSet<Literal> assignment = parseAssignment(answer.getAssignment(), aCNF.getVariables());
		
		//Return answer.
		return new SATSolverResult(answer.getSATResult(), answer.getRuntime(), assignment);
		
	}
	
	private HashSet<Literal> parseAssignment(int[] assignment, Collection<Long> CNFVars) 
	{
		HashSet<Literal> decompressedAssignment = new HashSet<Literal>();

		for (int i = 1; i < assignment[0]; i++)
		{
			int intLit = assignment[i];
			int var = Math.abs(intLit);
			boolean sign = intLit > 0;
			long decompressValue = fCompressor.decompressVar(var);
			if (CNFVars.contains(decompressValue))
			{
				Literal aLit = new Literal(decompressValue, sign);
				decompressedAssignment.add(aLit);
			}
		}

		return decompressedAssignment;
	}

	@Override
	public void interrupt(){
		log.debug("Interrupting consumer.");
		fConsumer.interrupt();
	}

	@Override
	public void notifyShutdown() {
		log.debug("Shutting down consumer.");
		fConsumer.notifyShutdown();
		log.debug("Shutting down execution service.");
		fExecutorService.shutdownNow();
	}

}
