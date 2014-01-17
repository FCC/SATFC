package ca.ubc.cs.beta.stationpacking.execution.daemon;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.SolverManager;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.listener.ServerListener;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.responder.ServerResponder;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.responder.ServerResponse;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver.ServerSolver;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver.ServerSolverInterrupter;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver.SolvingJob;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.daemon.ThreadedSolverServerParameters;
import ca.ubc.cs.beta.stationpacking.utils.RunnableUtils;

import com.beust.jcommander.ParameterException;

public class ThreadedSolverServerExecutor {
	
	private final static AtomicInteger TERMINATION_STATUS = new AtomicInteger(0);
	
	private final static UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER;
		
	static
	{
		/*
		 * Statically define the uncaught exception handler.
		 */

		//Any uncaught exception should terminate current process.
		UNCAUGHT_EXCEPTION_HANDLER = new UncaughtExceptionHandler() 
		{
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				
				e.printStackTrace();
				
				System.err.println("Thread "+t.getName()+" died with an exception ("+e.getMessage()+").");
				
				System.err.println("Stopping service :( .");
				EXECUTOR_SERVICE.shutdownNow();
				
				TERMINATION_STATUS.set(1);
				
			}
		};
	}
	
	private final static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
	
	public static void main(String[] args) {
		
		System.out.println("Launching SATFC.");
		
		//Parse the command line arguments in a parameter object.
		ThreadedSolverServerParameters aParameters = new ThreadedSolverServerParameters();
		//Check for help
		try
		{
			JCommanderHelper.parseCheckingForHelpAndVersion(args, aParameters,TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators());
		}
		catch (ParameterException aParameterException)
		{
			throw aParameterException;
		}
		aParameters.LoggingOptions.initializeLogging();
		Logger log = LoggerFactory.getLogger(ThreadedSolverServerExecutor.class);
		
		//Setup the solver manager.
		SolverManager aSolverManager = aParameters.SolverManagerParameters.getSolverManager();
		
		//Setup server socket.
		int aServerPort = aParameters.Port;
		
		DatagramSocket aServerSocket;
		if(aParameters.AllowAnyone)
		{
			try {
				aServerSocket = new DatagramSocket(aServerPort);
				log.info("Server listening to any request.");
			} catch (SocketException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Could not create server socket ("+e.getMessage()+").");
			}	
		}
		else
		{
			try {
				aServerSocket = new DatagramSocket(aServerPort, InetAddress.getByName("localhost"));
				log.info("Server listening to requests only from localhost.");
			} 
			catch (SocketException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Could not create server socket ("+e.getMessage()+").");
			}
			catch (UnknownHostException e1) {
				e1.printStackTrace();
				throw new IllegalArgumentException("Unknown local host ("+e1.getMessage()+").");
			}
			
		}
		
		//Setup queues and solver state.
		BlockingQueue<SolvingJob> aSolvingJobQueue = new LinkedBlockingQueue<SolvingJob>();
		ServerSolverInterrupter aSolverState = new ServerSolverInterrupter();
		
		BlockingQueue<ServerResponse> aServerResponseQueue = new LinkedBlockingQueue<ServerResponse>();
		
				
		//Setup server runnables.
		ServerListener aServerListener = new ServerListener(aSolvingJobQueue, aSolverState, aServerResponseQueue, aServerSocket);
		ServerResponder aServerResponder = new ServerResponder(aServerResponseQueue, aServerSocket);
		ServerSolver aServerSolver = new ServerSolver(aSolverManager, aSolverState, aSolvingJobQueue, aServerResponseQueue);
		
		
		
		//Submit and start producers and consumers.
		
		RunnableUtils.submitRunnable(EXECUTOR_SERVICE, UNCAUGHT_EXCEPTION_HANDLER, aServerListener);
		RunnableUtils.submitRunnable(EXECUTOR_SERVICE, UNCAUGHT_EXCEPTION_HANDLER, aServerSolver);
		RunnableUtils.submitRunnable(EXECUTOR_SERVICE, UNCAUGHT_EXCEPTION_HANDLER, aServerResponder);
		
		try {
			EXECUTOR_SERVICE.awaitTermination(365*10, TimeUnit.DAYS);
		} catch (InterruptedException e1) {
			log.error("We are really amazed that we're seeing this right now",e1);
			return;
		}
	
		System.exit(TERMINATION_STATUS.get());
		
	}
	
}
