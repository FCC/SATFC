package ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.listener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.responder.ServerResponse;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver.ServerSolverInterrupter;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver.SolvingJob;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;

/**
 * Producer runnable that is in charge of listening to a socket for (UDP) command messages and enqueuing those commands or the response to those commands.
 * @author afrechet
 */
public class ServerListener implements Runnable {
	
	private static Logger log = LoggerFactory.getLogger(ServerListener.class);
	
	/*
	 * Command fields.
	 */
	public final static String COMMANDSEP = ":";
	private enum ServerCommand {
		TEST,
		TERMINATE,
		SOLVE,
		INTERRUPT,
		PING;
	}

	
	/*
	 * Communication fields.
	 */
	private final static int MAXPACKETSIZE = 65000;

	private final DatagramSocket fServerSocket;

	private final BlockingQueue<ServerResponse> fServerResponseQueue;
	
	/*
	 * Solving jobs field.
	 */
	private final BlockingQueue<SolvingJob> fSolvingJobQueue;
	
	private final ServerSolverInterrupter fSolverState;
	
	/**
	 * 
	 * @param aSolvingJobQueue - queue to submit job to.
	 * @param aSolverState - a thread-safe solver state to use for job interruption.
	 * @param aServerResponseQueue - queue to submit responses/communication messages to.
	 * @param aServerSocket - should be listening to local host (127.0.0.1).
	 */
	public ServerListener(BlockingQueue<SolvingJob> aSolvingJobQueue, ServerSolverInterrupter aSolverState, BlockingQueue<ServerResponse> aServerResponseQueue, DatagramSocket aServerSocket) {
		
		if(aServerSocket.isClosed())	
		{
			throw new IllegalArgumentException("Provided socket is closed.");
		}
		
		fServerSocket = aServerSocket;

		fServerResponseQueue = aServerResponseQueue;
		
		fSolvingJobQueue = aSolvingJobQueue;
		
		fSolverState = aSolverState;
	}

	@Override
	public void run() {
		
		Thread.currentThread().setName("Server Listener Thread");
		
		// Prevent infinite loop from closed socket re-reading
		if (fServerSocket.isClosed())
		{
			throw new IllegalStateException("Trying to communicate with a closed server socket.");
		}

		log.info("Server listener is listening on {} port {} for commands.",fServerSocket.getLocalAddress(),fServerSocket.getLocalPort());

		try {
			while (true) {

				byte[] aReceiveData = new byte[MAXPACKETSIZE];

				try {
					//Listen to the socket.
					log.info("Listening to socket at "+fServerSocket.getLocalAddress()+" port "+fServerSocket.getLocalPort());
					DatagramPacket aReceivePacket = new DatagramPacket(
							aReceiveData, aReceiveData.length);
					fServerSocket.receive(aReceivePacket);

					//For security, filter non-localhost messages.
					if (!InetAddress.getByName("localhost").equals(aReceivePacket.getAddress())) {
						log.warn(
								"Received request from a non-{}, ignoring request from {}.",
								InetAddress.getByName("localhost"), aReceivePacket.getAddress());
						continue;
					}

					log.info("Received a packet.");
					InetAddress aSendAddress = aReceivePacket.getAddress();
					int aSendPort = aReceivePacket.getPort();
					
					aReceiveData = aReceivePacket.getData();

					
					String aMessage = new String(aReceiveData,"ASCII").trim();

					log.info("Message received: \"{}\"",aMessage);

					//Process message
					try
					{
						if(!processCommand(aMessage, aSendAddress, aSendPort))return;
					}
					catch(InterruptedException e)
					{
						log.error("Process command method was interrupted, propagating interruption ({}).",e.getMessage());
						Thread.currentThread().interrupt();
						return;
					}

				} catch (IOException e) {
					log.error("Unknown exception occured while receiving data, will wait for next request ({}).",e);
				}
			}
		} 
		finally 
		{
			log.info("Server's listener thread shutting down.");
		}
	}
	
	/**
	 * @param aMessage - a command message.
	 * @param aSendPort - the (localhost) port that sent the message.
	 * @return false if (listening thread) must terminate, true otherwise.
	 */
	private boolean processCommand(String aMessage, InetAddress aSendAddress, int aSendPort) throws InterruptedException
	{
		//Switch on the command in the message.
		String aServerCommandString = aMessage.trim().split(COMMANDSEP)[0];
		ServerCommand aServerCommand;
		try
		{
			aServerCommand = ServerCommand.valueOf(aServerCommandString);
		}
		catch(IllegalArgumentException e)
		{
			//Failed recognizing the message.
			log.warn("Could not process server command {} ({}).",aServerCommandString,e.getMessage());
			String aError = "Message received is not recognizable message string ("+aMessage+").";
			log.warn(aError);
			fServerResponseQueue.put(new ServerResponse("ERROR"+COMMANDSEP+aError,aSendAddress,aSendPort));
			return true;
		}

		switch(aServerCommand)
		{
		case TEST:
			return processTestCommand(aMessage,aSendAddress,aSendPort);
		case TERMINATE:
			return processTerminateCommand();
		case SOLVE:
			return processSolveCommand(aMessage,aSendAddress,aSendPort);
		case PING:
			return processPingCommand(aMessage,aSendAddress,aSendPort);
		case INTERRUPT:
			return processInterruptMessage(aMessage,aSendAddress,aSendPort);
		default:
			return processErronousMessage(aServerCommand, aMessage, aSendAddress, aSendPort);
		}
	}
	
	/*
	 * Individual process methods for each of the possible commands.
	 */
	
	private boolean processTestCommand(String aMessage, InetAddress aSendAddress, int aSendPort) throws InterruptedException
	{
		log.info("Received a TEST message: {}",aMessage);
		fServerResponseQueue.put(new ServerResponse(StringUtils.join(new String[]{"TEST","Got a test message."},COMMANDSEP),aSendAddress,aSendPort));
		return true;
	}
	
	private boolean processTerminateCommand()
	{
		log.info("Got a termination command, terminating.");
		return false;
	}
	
	private boolean processSolveCommand(String aMessage, InetAddress aSendAddress, int aSendPort) throws InterruptedException
	{
		log.info("Got a solving command, enqueuing solving run.");
		
		AutoStartStopWatch aOverheadWatch = new AutoStartStopWatch();
		
		//Parse solve message
		String aID;
		String aDataFoldername;
		String aInstanceString;
		double aCutoff;
		long aSeed;
		try
		{
			String[] aMessageParts = aMessage.split(COMMANDSEP);
			if(aMessageParts.length==5)
			{
				//Assuming old job message with no id. ID will be attributed by itself.
				String[] aOldMessageParts = aMessageParts;
				aMessageParts = new String[6];
				aMessageParts[0] = ServerCommand.SOLVE.toString();
				aMessageParts[1] = RandomStringUtils.randomAlphanumeric(10);
				log.warn("Could not find job ID in the message, job ID was (randomly) attributed to {}.",aMessageParts[1]);
				for(int i=1;i<aOldMessageParts.length;i++)
				{
					aMessageParts[i+1]=aOldMessageParts[i];
				}
			}
			if(aMessageParts.length!=6)
			{
				throw new IllegalArgumentException("Solving command does not have necessary additional information.");
			}
			
			aID = aMessageParts[1].trim();
			log.info("Problem ID {}, with",aID);
			
			aDataFoldername = aMessageParts[2];
			log.info("data from {}, and",aDataFoldername);
			
			aInstanceString = aMessageParts[3];
			log.info("instance {}, and",aInstanceString);

			aCutoff = Double.valueOf(aMessageParts[4]);
			log.info("cutoff {} s, and",aCutoff);

			aSeed = Long.valueOf(aMessageParts[5]);
			log.info("seed {}, and",aSeed);
		}
		catch(Exception e)
		{
			log.warn("There was an exception while parsing the solve message ({}).",e.getMessage());
			fServerResponseQueue.put(new ServerResponse("ERROR"+COMMANDSEP+e.getMessage(),aSendAddress,aSendPort));
			return true;
		}
		
		double aOverhead = aOverheadWatch.stop()/1000.0;
		log.debug("Overhead of processing solve message {} ms.",aOverhead);
		double aRemainingTime = aCutoff-aOverhead;
		if(aRemainingTime<=0)
		{
			log.warn("Already have spent more than the required cutoff.");
			fServerResponseQueue.put(new ServerResponse("INFO"+COMMANDSEP+"Already have spent more than the required cutoff.", aSendAddress, aSendPort));
			String aAnswer = StringUtils.join(new String[]{"ANSWER",aID,SolverResult.createTimeoutResult(aOverhead).toParsableString()},COMMANDSEP);
			fServerResponseQueue.put(new ServerResponse(aAnswer,aSendAddress,aSendPort));
		}
		else
		{
			if(aID.equals("feascheck"))
			{
				log.debug("Received a feascheck job, making sure solver is available by interrupting current job.");
				fSolverState.interruptCurrentJob();
			}
			
			log.info("Enqueuing instance with ID {}.",aID);
			fSolvingJobQueue.put(new SolvingJob(aID, aDataFoldername, aInstanceString, aRemainingTime, aSeed, aSendAddress, aSendPort));
		}

		return true;
	}
	
	private boolean processPingCommand(String aMessage, InetAddress aSendAddress,  int aSendPort) throws InterruptedException
	{
		log.info("Got a ping command, sending back an acknowledgement.");
		String aAcknowledgement = ServerCommand.PING.toString();
		fServerResponseQueue.put(new ServerResponse(aAcknowledgement, aSendAddress, aSendPort));
		return true;
	}
	
	private boolean processErronousMessage(ServerCommand aServerCommand, String aMessage, InetAddress aSendAddress, int aSendPort) throws InterruptedException
	{
		String aError = "Unrecognized command "+aServerCommand+" in message.";
		log.warn(aError);
		fServerResponseQueue.put(new ServerResponse("ERROR"+COMMANDSEP+aError,aSendAddress,aSendPort));
		return true;
	}
	
	private boolean processInterruptMessage(String aMessage, InetAddress aSendAddress, int aSendPort) throws InterruptedException
	{
		String aID;
		try
		{
			String[] aMessageParts = aMessage.split(COMMANDSEP);
			aID = aMessageParts[1];
			
			log.info("Interrupting job with ID {}.",aID);
			
			fSolverState.interrupt(aID);
		}
		catch(Exception e)
		{
			log.warn("There was an exception while parsing the interrupt message ({}).",e.getMessage());
			fServerResponseQueue.put(new ServerResponse("ERROR"+COMMANDSEP+e.getMessage(),aSendAddress,aSendPort));
		}
		return true;
	}

	

}
