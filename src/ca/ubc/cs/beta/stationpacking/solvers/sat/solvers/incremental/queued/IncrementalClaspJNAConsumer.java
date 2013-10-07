package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental.queued;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries.IncrementalClaspLibrary;

/**
 * Consumer runnable in charge of feeding problems for and reading answers from the JNA clasp library (in an online fashion since we're working incrementally).
 * @author alex
 */
public class IncrementalClaspJNAConsumer implements Runnable{

	private static Logger log = LoggerFactory.getLogger(IncrementalClaspJNAConsumer.class);
	
	private final BlockingQueue<IncrementalProblem> fProblemQueue;
	private final BlockingQueue<SATSolverResult> fAnswerQueue;
	
	/**
	 * Constructs a problem consumer that feeds problems to incremental clasp (in an online fashion) and sends back answers, 
	 * all through thread safe blocking queues.
	 */
	public IncrementalClaspJNAConsumer(IncrementalClaspLibrary aIncrementalClaspLibrary,BlockingQueue<IncrementalProblem> aProblemQueue, BlockingQueue<SATSolverResult> aAnswerQueue)
	{
		//Setup the queues.
		fProblemQueue = aProblemQueue;
		fAnswerQueue = aAnswerQueue;
		
		//Setup the necessary library components.
		//TODO
		
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName("Incremental Clasp JNA Consumer");
		
		log.info("Incremental Clasp JNA Consumer processing problems on a single thread.");
		try
		{
			while(true)
			{
				//Take problem from problem queue.
				IncrementalProblem problem;
				try {
					problem = fProblemQueue.take();
				} catch (InterruptedException e) {
					log.error("Incremental Clasp JNA Consumer's take() method was interrupted, propagating interruption ({}).",e.getMessage());
					Thread.currentThread().interrupt();
					return;
				}
				
				
				//Solve the problem by offering it to the JNA library.
				//TODO duplicate IncrementalClaspSATSolver code.
							
				
				//Get problem answer from library.
				//TODO duplicate IncrementalClaspSATSolver code.
				SATSolverResult answer = null;
				
				try {
					fAnswerQueue.put(answer);
				} catch (InterruptedException e) {
					log.error("Incremental Clasp JNA Consumer's put() method was interrupted, propagating interruption ({}).",e.getMessage());
					Thread.currentThread().interrupt();
					return;
				}
			
			}
		}
		finally
		{
			log.info("Shutting down Incremental Clasp JNA Consumer.");
			//TODO Shutdown the library.
		}
	}
	
	/**
	 * Interrupt the current solving, if any.
	 */
	public void interrupt()
	{
		//TODO
	}
	
	/**
	 * Shutdown the consumer, preventing any further use.
	 */
	public void notifyShutdown()
	{
		//TODO
	}

}
