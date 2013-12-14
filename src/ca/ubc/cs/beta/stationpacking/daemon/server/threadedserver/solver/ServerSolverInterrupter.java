package ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.solvers.ISolver;

/**
 * Atomically maintains state for a solving thread. Main functionality is to allow interruption.
 * @author afrechet
 */
public class ServerSolverInterrupter {

	private static Logger log = LoggerFactory.getLogger(ServerSolverInterrupter.class);
	
	private String fCurrentJobID;
	private ISolver fCurrentSolver;
	
	private Collection<String> fInterruptedJobIDs;
	private final static int MAXSIZE = 200;
	
	public ServerSolverInterrupter()
	{
		fInterruptedJobIDs = Collections.newSetFromMap(new ConcurrentHashMap<String,Boolean>());
		
		fCurrentJobID = null;
		fCurrentSolver = null;
	}
	
	/**
	 * Get the current job ID.
	 * @return the String ID of the current job.
	 */
	public synchronized String getCurrentJobID()
	{	
		return fCurrentJobID;
	}
	
	/**
	 * Check if the job ID has been interrupted. If the job was interrupted, it will also be removed from the set of interrupted jobs.
	 * @param aJobID - a String job ID.
	 * @return true if the job ID has been interrupted, false otherwise.
	 */
	public synchronized boolean isInterrupted(String aJobID)
	{
		if(fInterruptedJobIDs.size()>MAXSIZE)
		{
			log.warn("Interrupted jobs contains more than {} jobs to be interrupted. Having such a large set may lead to inefficiencies.");
		}
		boolean isInterrupted = fInterruptedJobIDs.contains(aJobID);
		if(isInterrupted)
		{
			fInterruptedJobIDs.remove(aJobID);
		}
		return isInterrupted;
	}
	
	/**
	 * Notify that the the provided solver is starting to solve a job with the given ID.
	 * @param aJobID - a String job ID.
	 * @param aSolver - an ISolver solving a job with the given ID.
	 * @return true if and only if the job is interrupted (will not be started).
	 */
	public synchronized boolean notifyStart(String aJobID, ISolver aSolver)
	{
		if(fCurrentJobID!=null)
		{
			throw new IllegalStateException("Cannot notify start as there is already a current job.");
		}
		else
		{
			if(!isInterrupted(aJobID))
			{
			
				fCurrentJobID = aJobID;
				fCurrentSolver = aSolver;
				return false;
			}
			else
			{
				return true;
			}
		}
	}
	
	/**
	 * Notify that we have stopped solving a job with the given ID.
	 * @param aJobID - a String job ID.
	 * @return true if the current job was stopped properly, false otherwise (e.g. current job was not already assigned, or current job does not equal given job). 
	 */
	public synchronized void notifyStop(String aJobID)
	{
		if(fCurrentJobID==null)
		{
			throw new IllegalStateException("Cannot notify stop as there is not current job.");
		}
		else
		{
			if(fCurrentJobID.equals(aJobID))
			{
		
				fCurrentJobID = null;
				fCurrentSolver = null;
			}
			else
			{
				throw new IllegalStateException("Cannot notify stop job "+aJobID+" as it is not equal to the current job "+fCurrentJobID);
			}
		}
	}
	
	
	/**
	 * Interrupt a job with the given ID. This is either done by interrupting the current job through its corresponding solver, or by adding the job ID to 
	 * the set of interrupted job IDs. If the current job is interrupted, note that notifyStop() also needs to be called to signal the state that we indeed stopped
	 * the job.
	 * @param aJobID - a String job ID.
	 */
	public synchronized void interrupt(String aJobID)
	{
		if(fCurrentJobID.equals(aJobID))
		{
			log.debug("Job {} was current job, interrupting it.",aJobID);
			fCurrentSolver.interrupt();
		}
		else
		{
			log.debug("Adding job {} to the set of (to-be) interrupted jobs.",aJobID);
			fInterruptedJobIDs.add(aJobID);
		}
	}
	
	/**
	 * Interrupt the current executing job, not matter what it is. Ends gracefully if there is no current job. Note that notifyStop() also needs to be called to signal the state that we indeed stopped
	 * the job.
	 */
	public synchronized void interruptCurrentJob()
	{
		if(fCurrentJobID==null)
		{
			log.debug("No current job to interrupt.");
		}
		else
		{
			log.debug("Interrupting current job with ID {}.",fCurrentJobID);
			fCurrentSolver.interrupt();
		}
	}
	
	


}
