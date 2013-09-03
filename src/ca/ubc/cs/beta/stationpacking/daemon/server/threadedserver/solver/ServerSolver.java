package ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver;

import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.SolverBundle;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.SolverManager;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.listener.ServerListener;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.responder.ServerResponse;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;

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
				AutoStartStopWatch aOverheadWatch = new AutoStartStopWatch();
				
				String aID = aSolvingJob.getID();
				
				//Check if this job is interrupted.
				if(fSolverState.isInterrupted(aID))
				{
					log.debug("Skipping interrupted job {}.",aID);
					continue;
				}
				
				String aDataFolderName = aSolvingJob.getDataFolderName();
				String aInstanceString = aSolvingJob.getInstanceString();
				
				double aCutoff = aSolvingJob.getCutOff();
				long aSeed = aSolvingJob.getSeed();
				
				InetAddress aSendAddress = aSolvingJob.getSendAddress();
				int aSendPort = aSolvingJob.getSendPort();
				
				log.info("Got solving job with ID {}.",aID);
				log.info("Beginning to solve...");
				
				SolverBundle aBundle;
				try 
				{
					aBundle = fSolverManager.getData(aDataFolderName);
				} catch (FileNotFoundException e) {
					String aError = "Could not find solving data for "+aSolvingJob.getDataFolderName()+" ("+e.getMessage()+")";
					log.error(aError);
					try
					{
						fServerResponseQueue.put(new ServerResponse("ERROR"+ServerListener.COMMANDSEP+aError,aSendAddress,aSendPort));
					}
					catch(InterruptedException e1)
					{
						log.error("Solving job queue take method was interrupted, propagating interruption ({}).",e1.getMessage());
						Thread.currentThread().interrupt();
						return;
					}
					continue;
				}

				IStationManager aStationManager = aBundle.getStationManager();
				StationPackingInstance aInstance = StationPackingInstance.valueOf(aInstanceString, aStationManager);

				ISolver aSolver = aBundle.getSolver();
				
				double aOverhead = aOverheadWatch.stop()/1000.0;
				log.debug("Overhead of initializing solve command {} ms.",aOverhead);
				double aRemainingTime = aCutoff - aOverhead;
				if(aRemainingTime<=0)
				{
					try
					{
						log.warn("Already have spent more than the required cutoff.");
						fServerResponseQueue.put(new ServerResponse("INFO"+ServerListener.COMMANDSEP+"Already have spent more than the required cutoff.", aSendAddress, aSendPort));
						String aAnswer = StringUtils.join(new String[]{"ANSWER",aID,SolverResult.createTimeoutResult(aOverhead).toParsableString()},ServerListener.COMMANDSEP);
						fServerResponseQueue.put(new ServerResponse(aAnswer,aSendAddress,aSendPort));
					}
					catch(InterruptedException e1)
					{
						log.error("Solving job queue take method was interrupted, propagating interruption ({}).",e1.getMessage());
						Thread.currentThread().interrupt();
						return;
					}
					continue;
				}
				
				log.info("Beginning to solve...");
								
				//Notify the start of the solving.
				fSolverState.notifyStart(aID, aSolver);
				
				
				SolverResult aResult;
				try
				{
					try
					{
						aResult = aSolver.solve(aInstance, aCutoff, aSeed);
					}
					finally
					{
						//Notify solver state.
						fSolverState.notifyStop(aID);
					}
				}
				catch(Exception e)
				{
					String aError = "Got an exception while trying to execute a solving command ("+e.getMessage()+")";
					log.error(aError);
					try
					{
						fServerResponseQueue.put(new ServerResponse("ERROR"+ServerListener.COMMANDSEP+aError,aSendAddress,aSendPort));
					}
					catch(InterruptedException e1)
					{
						log.error("Solving job queue take method was interrupted, propagating interruption ({}).",e1.getMessage());
						Thread.currentThread().interrupt();
						return;
					}
					
					continue;
				}
				
				//Send the result if necessary.
				if (aResult.getResult() != SATResult.INTERRUPTED) // do not return if the command was killed.
				{
					String aAnswer = StringUtils.join(new String[]{"ANSWER",aID,aResult.toParsableString()},ServerListener.COMMANDSEP);
					try
					{
						fServerResponseQueue.put(new ServerResponse(aAnswer,aSendAddress,aSendPort));
					}
					catch(InterruptedException e1)
					{
						log.error("Solving job queue take method was interrupted, propagating interruption ({}).",e1.getMessage());
						Thread.currentThread().interrupt();
						return;
					}
				}
				else
				{
					log.debug("Solve process was (gracefully) interrupted.");
				}
			}
		}
		finally
		{
			notifyShutdown();
			log.info("Server's solver thread shutting down.");
		}
	}
	
	public void notifyShutdown()
	{
		log.info("Shutting down.");
		fSolverManager.notifyShutdown();
	}

	

}
