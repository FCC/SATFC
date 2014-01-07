package ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver;

import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.SolverManager;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.listener.ServerListener;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.responder.ServerResponse;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;

/**
 * Consumer runnable that is in charge of solving station packing problems from a given queue.
 * @author afrechet
 */
public class ServerSolver implements Runnable {

	private static Logger log = LoggerFactory.getLogger(ServerSolver.class);
	
	private final SolverManager fSolverManager;
	private final BlockingQueue<SolvingJob> fSolvingJobQueue;
	private final BlockingQueue<ServerResponse> fServerResponseQueue;
	private final ServerSolverInterrupter fSolverState;
	
	public ServerSolver(SolverManager aSolverManager, ServerSolverInterrupter aSolverState, BlockingQueue<SolvingJob> aSolvingJobQueue, BlockingQueue<ServerResponse> aServerResponseQueue)
	{
		fSolverManager = aSolverManager;
		
		fSolverState = aSolverState;
		
		fSolvingJobQueue = aSolvingJobQueue;
		
		fServerResponseQueue = aServerResponseQueue;
	}
	
	public static class SolverServerStateInterruptedException extends Exception {
		static final long serialVersionUID = 1L;
		
		public final String jobID;
		
		public SolverServerStateInterruptedException(String jobID) {
			this.jobID = jobID;
		}
	}
	
	public static class SolvingInterrupedException extends Exception {
		private static final long serialVersionUID = 1L;
	}
	
	public static class ProblemInitializingOverheadTimeoutException extends Exception {
		private static final long serialVersionUID = 1L;
		
		public final ServerResponse infoResponse;
		public final ServerResponse answerResponse;
		
		public ProblemInitializingOverheadTimeoutException(ServerResponse infoResponse, ServerResponse answerResponse) {
			this.infoResponse = infoResponse;
			this.answerResponse = answerResponse;
		}
	}
	
	/*
	 * Separate solving from queueing to allow the use of ServerSolver logic without a separate thread.
	 *   
	 * @see SATFCJobClient
	 * @author wtaysom
	 */
	public ServerResponse solve(SolvingJob aSolvingJob) throws SolverServerStateInterruptedException, ProblemInitializingOverheadTimeoutException, SolvingInterrupedException {
		AutoStartStopWatch aOverheadWatch = new AutoStartStopWatch();
		
		String aID = aSolvingJob.getID();
		
		//Check if this job is interrupted.
		if(fSolverState.isInterrupted(aID))
		{
			throw new SolverServerStateInterruptedException(aID);
		}
		
		String aDataFolderName = aSolvingJob.getDataFolderName();
		String aInstanceString = aSolvingJob.getInstanceString();
		
		double aCutoff = aSolvingJob.getCutOff();
		long aSeed = aSolvingJob.getSeed();
		
		InetAddress aSendAddress = aSolvingJob.getSendAddress();
		int aSendPort = aSolvingJob.getSendPort();
		
		log.debug("Got solving job with ID {}.",aID);
		log.debug("Setting up solving environment...");
		
		ISolverBundle aBundle;
		try 
		{
			aBundle = fSolverManager.getData(aDataFolderName);
		} catch (FileNotFoundException e) {
			String aError = "Could not find solving data for "+aSolvingJob.getDataFolderName()+" ("+e.getMessage()+")";
			log.error(aError);
			return new ServerResponse("ERROR"+ServerListener.COMMANDSEP+aError,aSendAddress,aSendPort);
		}

		IStationManager aStationManager = aBundle.getStationManager();
		StationPackingInstance aInstance;
		try {
			aInstance = StationPackingInstance.valueOf(aInstanceString, aStationManager);
		} catch(RuntimeException e)
		{
			e.printStackTrace();
			throw e;
		}

		ISolver aSolver = aBundle.getSolver(aInstance);
		
		double aOverhead = aOverheadWatch.stop()/1000.0;
		log.debug("Overhead of initializing solve command {} s.",aOverhead);
		double aRemainingTime = aCutoff - aOverhead;
		if(aRemainingTime<=0)
		{
			log.warn("Already have spent more than the required cutoff.");
			ServerResponse infoResponse = new ServerResponse("INFO"+ServerListener.COMMANDSEP+"Already have spent more than the required cutoff.", aSendAddress, aSendPort);
			String aAnswer = StringUtils.join(new String[]{"ANSWER",aID,SolverResult.createTimeoutResult(aOverhead).toParsableString()},ServerListener.COMMANDSEP);
			ServerResponse answerResponse = new ServerResponse(aAnswer,aSendAddress,aSendPort);
			throw new ProblemInitializingOverheadTimeoutException(infoResponse, answerResponse);
		}
		
		log.debug("Beginning to solve, cutoff in "+aCutoff+"s...");
						
		//Notify the start of the solving.
		fSolverState.notifyStart(aID, aSolver);
		
		SolverResult aResult;
		try
		{
			try
			{
				aResult = aSolver.solve(aInstance, aCutoff, aSeed);
				log.debug("Problem solved.");
			}
			finally
			{
				//Notify solver state.
				fSolverState.notifyStop(aID);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			String aError = "Got an exception while trying to execute a solving command ("+e.getMessage()+")";
			log.error(aError);
			return new ServerResponse("ERROR"+ServerListener.COMMANDSEP+aError,aSendAddress,aSendPort);
		}
		
		//Send the result if necessary.
		if (aResult.getResult() != SATResult.INTERRUPTED) // do not return if the command was interrupted.
		{
			String aAnswer = StringUtils.join(new String[]{"ANSWER",aID,aResult.toParsableString()},ServerListener.COMMANDSEP);
			return new ServerResponse(aAnswer,aSendAddress,aSendPort);
		}
		else
		{
			throw new SolvingInterrupedException();
		}
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName("Server Solver Thread");
		
		log.info("Server's solver is processing solving commands on a single thread.");
		
			try
			{
				while(true)
				{
					SolvingJob aSolvingJob;
					try
					{
						aSolvingJob = fSolvingJobQueue.take();
					}
					catch(InterruptedException e)
					{
						log.error("Solving job queue take method was interrupted, propagating interruption ({}).",e.getMessage());
						Thread.currentThread().interrupt();
						return;
					}
					
					try {
					ServerResponse aServerResponse = solve(aSolvingJob);
						try
						{
							log.debug("Sending back an answer.");
							fServerResponseQueue.put(aServerResponse);
						}
						catch(InterruptedException e1)
						{
							log.error("Solving job queue take method was interrupted, propagating interruption ({}).",e1.getMessage());
							Thread.currentThread().interrupt();
							return;
						}
					} catch (SolverServerStateInterruptedException e) {
						log.debug("Skipping interrupted job {}.", e.jobID);
					} catch (ProblemInitializingOverheadTimeoutException e) {
						try
						{
							fServerResponseQueue.put(e.infoResponse);
							fServerResponseQueue.put(e.answerResponse);
						}
						catch(InterruptedException e1)
						{
							log.error("Solving job queue take method was interrupted, propagating interruption ({}).",e1.getMessage());
							Thread.currentThread().interrupt();
							return;
						}
					} catch (SolvingInterrupedException e) {
						log.debug("Solve process was (gracefully) interrupted.");
					}
				}
			}
			finally
			{
				log.info("Server's solver thread shutting down.");
				fSolverManager.notifyShutdown();
			}
		}
		
	}


	


