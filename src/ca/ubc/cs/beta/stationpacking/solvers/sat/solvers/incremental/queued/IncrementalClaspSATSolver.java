package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental.queued;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractSATSolver;
import ca.ubc.cs.beta.stationpacking.utils.RunnableUtils;

public class IncrementalClaspSATSolver extends AbstractSATSolver {

	private static Logger log = LoggerFactory.getLogger(IncrementalClaspSATSolver.class);
	
	private final BlockingQueue<IncrementalProblem> fProblemQueue;
	private final BlockingQueue<SATSolverResult> fAnswerQueue;
	
	private final ExecutorService fExecutorService;
	
	private final IncrementalClaspJNAConsumer fConsumer;
	
	public IncrementalClaspSATSolver(String libraryPath, String parameters, long seed)
	{
		/*
		 * Create encoding structures
		 */
		log.debug("Creating encoding structures.");
		//TODO (setup the required objects to encode problem instance into an incremental problem.)
		
		/*
		 * Create consumer.
		 */
		log.debug("Creating Clasp JNA consumer.");
		//Initialize communication queues.
		fProblemQueue = new ArrayBlockingQueue<IncrementalProblem>(1);
		fAnswerQueue = new ArrayBlockingQueue<SATSolverResult>(1);
		
		fConsumer = constructConsumer();
		
		/*
		 * Launch consumer.
		 */
		log.debug("Launching consumer.");
		
		fExecutorService = Executors.newCachedThreadPool();
		
		launchConsumer();
	}
	
	/**
	 * Constructs an IncrementalClaspJNAConsumer to be used in solving.
	 */
	private IncrementalClaspJNAConsumer constructConsumer()
	{
		//TODO (construct a consumer, setting up incremental Clasp library and whatnot.)
		throw new UnsupportedOperationException("constructConsumer() is not implemented.");
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
		//TODO (construct the incremental problem.)
		IncrementalProblem problem = null;
		
		//Give it to the consumer.
		try {
			fProblemQueue.put(problem);
		} catch (InterruptedException e) {
			log.error("Incremental Clasp SAT Solver's put() method was interrupted, propagating interruption ({}).",e.getMessage());
			Thread.currentThread().interrupt();
			return new SATSolverResult(SATResult.INTERRUPTED, 0.0, new HashSet<Literal>());
		}
		
		
		//Wait for an answer.
		SATSolverResult answer;
		try {
			answer = fAnswerQueue.take();
		} catch (InterruptedException e) {
			log.error("Incremental Clasp SAT Solver's take() method was interrupted, propagating interruption ({}).",e.getMessage());
			Thread.currentThread().interrupt();
			return new SATSolverResult(SATResult.INTERRUPTED, 0.0, new HashSet<Literal>());
		}
		
		//Return answer.
		return answer;
		
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
