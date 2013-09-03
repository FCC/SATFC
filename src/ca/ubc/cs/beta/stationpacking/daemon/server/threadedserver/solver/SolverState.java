package ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import ca.ubc.cs.beta.stationpacking.solvers.ISolver;

/**
 * Atomically maintains state for a solving thread/process. Main functionality is to allow interruption.
 * @author afrechet
 *
 */
public class SolverState {

	private String fCurrentJobID;
	private ISolver fCurrentSolver;
	
	private Collection<String> fInterruptedJobIDs;
	
	public SolverState()
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
	 * Check if the job ID has been interrupted.
	 * @param aJobID - a String job ID.
	 * @return true if the job ID has been interrupted, false otherwise.
	 */
	public synchronized boolean isInterrupted(String aJobID)
	{
		return fInterruptedJobIDs.contains(aJobID);
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
	 * the set of interrupted job IDs.
	 * @param aJobID - a String job ID.
	 */
	public synchronized void interrupt(String aJobID)
	{
		if(fCurrentJobID.equals(aJobID))
		{
			fCurrentSolver.interrupt();
		}
		else
		{
			fInterruptedJobIDs.add(aJobID);
		}
	}
	
	


}
