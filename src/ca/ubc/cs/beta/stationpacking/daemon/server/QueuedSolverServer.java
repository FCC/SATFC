package ca.ubc.cs.beta.stationpacking.daemon.server;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.SolverBundle;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.SolverManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;

/**
 * Wrapper around an ISolver and a StationManager that takes care of receiving problem instances and various misc commands from UDP localhost, and communicate
 * result and other information back.
 * 
 * @author afrechet, gsauln
 *
 */
public class QueuedSolverServer {

	private static Logger log = LoggerFactory.getLogger(QueuedSolverServer.class);

	/*
	 * Solving fields.
	 */
	private final SolverManager fSolverManager;
	
	private final LinkedBlockingQueue<SolveMessage> fMessages;
	
	private final SolverRunner fSolverRunner;
	
	private Thread fSolvingThread;
	
	/*
	 * Command fields.
	 */
	private final static String COMMANDSEP = ":";
	private enum ServerCommand {
		TEST,
		TERMINATE,
		SOLVE,
		PING;
	}

	/*
	 * Communication fields.
	 */
	private final DatagramSocket fServerSocket;
	private final InetAddress fIPAdress;
	private final int fServerPort;

	private final static int MAXPACKETSIZE = 65000;

	public QueuedSolverServer(SolverManager aSolverManager,int aServerPort) throws SocketException, UnknownHostException {

		if (aServerPort >= 0 && aServerPort < 1024)
		{
			log.warn("Trying to allocate a port < 1024 which generally requires root priviledges (which aren't necessary and discouraged), this may fail");
		}
		if(aServerPort < -1 || aServerPort > 65535)
		{
			throw new IllegalArgumentException("Port must be in the interval [0,65535]");
		}

		fServerPort = aServerPort;
		fServerSocket = new DatagramSocket(fServerPort);
		fIPAdress = InetAddress.getByName("localhost");

		//Set solver structures needed.
		fSolverManager = aSolverManager;
		fMessages = new LinkedBlockingQueue<SolveMessage>();
		fSolverRunner = new SolverRunner();
		fSolvingThread = new Thread(fSolverRunner);
		fSolverRunner.setThread(fSolvingThread);
	}

	/**
	 * @param aMessage - a string message to send.
	 * @param aPort - the port location on localhost.
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	private void sendLocalMessage(String aMessage, int aPort) throws IOException, IllegalArgumentException {
		ByteArrayOutputStream aBOUT = new ByteArrayOutputStream();
		ObjectOutputStream aOOUT = new ObjectOutputStream(aBOUT);
		aOOUT.writeObject(aMessage);

		byte[] aSendBytes = aMessage.getBytes(Charset.forName("ASCII"));

		if (aSendBytes.length > MAXPACKETSIZE) {
			log.warn("Response is too big to send to client, please adjust packet size in both client and server ("
					+ aSendBytes.length + " > " + MAXPACKETSIZE + ")");
			log.warn("Dropping message.");

			throw new IllegalArgumentException("Solver tried to send a message that is too large.");			
		}

		DatagramPacket sendPacket = new DatagramPacket(aSendBytes,
				aSendBytes.length, fIPAdress, aPort);

		log.info("Sending message \"{}\" back to "+fIPAdress+" port "+aPort,aMessage);
		fServerSocket.send(sendPacket);
	}

	/**
	 * Shutdown the solver server.
	 */
	public void notifyShutdown()
	{
		try 
		{
			fServerSocket.close();
		} 
		catch (RuntimeException e) 
		{
			log.error("Error occured while shutting down", e);
		}

		if(fSolverManager!=null)
		{
			fSolverManager.notifyShutdown();
		}

		terminateSolverRunner();
		log.info("Solver server shutting down.");
	}

	/**
	 * Start the solver server. It will now listen indefinitely to its given port for messages.
	 */
	public void start() {

		// Prevent infinite loop from closed socket re-reading
		if (fServerSocket.isClosed())
		{
			throw new IllegalStateException("Trying to communicate with a closed server socket.");
		}

		log.info("Solver server is processing requests using a single thread on localhost port {}.",fServerSocket.getInetAddress(),fServerSocket.getLocalPort());

		try {
			fSolvingThread.start();
			while (true) {

				byte[] aReceiveData = new byte[MAXPACKETSIZE];

				try {
					//Listen to the socket.
					log.info("Listening to socket at "+fIPAdress+" port "+fServerPort);
					DatagramPacket aReceivePacket = new DatagramPacket(
							aReceiveData, aReceiveData.length);
					fServerSocket.receive(aReceivePacket);

					if (!fIPAdress.equals(aReceivePacket.getAddress())) {
						log.warn(
								"Received request from a non-{}, ignoring request from {}.",
								fIPAdress, aReceivePacket.getAddress());
						continue;
					}

					log.info("Received a packet.");
					aReceiveData = aReceivePacket.getData();

					int aSendPort = aReceivePacket.getPort();
					String aMessage = new String(aReceiveData,"ASCII").trim();

					log.info("Message received: \"{}\"",aMessage);

					//Process message
					if(!processCommand(aMessage, aSendPort))return;

				} catch (IOException e) {
					log.error("Unknown exception occured while receiving data, will wait for next request ({}).",e);
				}
			}
		} 
		finally 
		{
			notifyShutdown();
		}
	}

	/**
	 * @param aMessage - a command message.
	 * @param aSendPort - the (localhost) port that sent the message.
	 * @return false if must terminate, true otherwise.
	 */
	private boolean processCommand(String aMessage, int aSendPort)
	{

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
			try
			{
				sendLocalMessage("ERROR"+COMMANDSEP+aError,aSendPort);
			}
			catch(IOException e1)
			{
				log.warn("Could not send a message back to client ("+e1.getMessage()+").");
			}
			return true;
		}

		switch(aServerCommand)
		{
		case TEST:
			log.info("Received a TEST message: {}",aMessage);
			try
			{
				sendLocalMessage(StringUtils.join(new String[]{"TEST","Got a test message."},COMMANDSEP),aSendPort);
			}
			catch(IOException e1)
			{
				log.warn("Could not send a message back to client ("+e1.getMessage()+").");
			}

			return true;
		case TERMINATE:
			log.info("Got a termination command, terminating.");
			return false;
		case SOLVE:
			log.info("Got a solving command, solving.");
			try
			{
				String[] aMessageParts = aMessage.split(COMMANDSEP);
				if(aMessageParts.length!=5)
				{
					throw new IllegalArgumentException("Solving command does not have necessary additional information.");
				}
				
				String aDataFoldername = aMessageParts[1];
				log.info("Getting data from {}.",aDataFoldername);
				
				//Check if we have the required data.
				if(!fSolverManager.hasData(aDataFoldername))
				{
					sendLocalMessage("INFO"+COMMANDSEP+"Warning, daemon solver did not have the problem data loaded, it will try to load it.", aSendPort);
				}
				
				String aInstanceString = aMessageParts[2];
				log.info("Solving instance {},",aInstanceString);

				double aCutoff = Double.valueOf(aMessageParts[3]);
				log.info("with cutoff {}, and",aCutoff);

				long aSeed = Long.valueOf(aMessageParts[4]);
				log.info("with seed {}.",aSeed);

				SolveMessage message = new SolveMessage(aSendPort, aDataFoldername, aInstanceString, aCutoff, aSeed);
				
				fMessages.add(message);
				
			}
			catch(Exception e)
			{
				log.warn("Got an exception while trying to execute a solving command ({}).",e.getMessage());
				e.printStackTrace();
				try
				{
					sendLocalMessage("ERROR"+COMMANDSEP+e.getMessage(), aSendPort);
				}
				catch(IOException e1)
				{
					log.warn("Could not send a message back to client ("+e1.getMessage()+").");
				}
			}

			return true;

		case PING:
			log.info("Got a ping command, sending back an acknowledgement.");
			String aAcknowledgement = ServerCommand.PING.toString();
			try
			{
				sendLocalMessage(aAcknowledgement, aSendPort);
			}
			catch(IOException e1)
			{
				log.warn("Could not send a message back to client ("+e1.getMessage()+").");
			}

			return true;
		default:
			String aError = "Unrecognized command "+aServerCommand+" in message.";
			log.warn(aError);
			try
			{
				sendLocalMessage("ERROR"+COMMANDSEP+aError,aSendPort);
			}
			catch(IOException e1)
			{
				log.warn("Could not send a message back to client ("+e1.getMessage()+").");
			}
			return true;
		}
	}

	private SolverResult solve(String aDataFoldername, String aInstanceString, double aCutoff, long aSeed) throws FileNotFoundException
	{
		SolverBundle bundle = fSolverManager.getData(aDataFoldername);

		IStationManager aStationManager = bundle.getStationManager();
		StationPackingInstance aInstance = StationPackingInstance.valueOf(aInstanceString, aStationManager);

		ISolver solver = bundle.getSolver();
		SolverResult aResult = solver.solve(aInstance, aCutoff, aSeed);

		return aResult;
	}

	protected void terminateSolverRunner()
	{
		fSolverRunner.stop();
		fSolvingThread.interrupt();
	}

	protected class SolveMessage
	{
		private int fSendPort;
		private String fDataFolderName;
		private String fInstanceString;
		private double fCutOff;
		private long fSeed;

		public SolveMessage(int sendPort, String aDataFoldername, String aInstanceString, double aCutoff, long aSeed)
		{
			fSendPort = sendPort;
			fDataFolderName = aDataFoldername;
			fInstanceString = aInstanceString;
			fCutOff = aCutoff;
			fSeed = aSeed;
		}

		public int getSendPort()
		{
			return fSendPort;
		}

		public String getDataFolderName()
		{
			return fDataFolderName;
		}

		public String getInstanceString()
		{
			return fInstanceString;
		}

		public double getCutOff()
		{
			return fCutOff;
		}

		public long getSeed()
		{
			return fSeed;
		}

		public SolveMessage copy()
		{
			return new SolveMessage(fSendPort, fDataFolderName, fInstanceString, fCutOff, fSeed);
		}

		private QueuedSolverServer getOuterType() {
			return QueuedSolverServer.this;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			long temp;
			temp = Double.doubleToLongBits(fCutOff);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime
					* result
					+ ((fDataFolderName == null) ? 0 : fDataFolderName
							.hashCode());
			result = prime
					* result
					+ ((fInstanceString == null) ? 0 : fInstanceString
							.hashCode());
			result = prime * result + (int) (fSeed ^ (fSeed >>> 32));
			result = prime * result + fSendPort;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SolveMessage other = (SolveMessage) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (Double.doubleToLongBits(fCutOff) != Double
					.doubleToLongBits(other.fCutOff))
				return false;
			if (fDataFolderName == null) {
				if (other.fDataFolderName != null)
					return false;
			} else if (!fDataFolderName.equals(other.fDataFolderName))
				return false;
			if (fInstanceString == null) {
				if (other.fInstanceString != null)
					return false;
			} else if (!fInstanceString.equals(other.fInstanceString))
				return false;
			if (fSeed != other.fSeed)
				return false;
			if (fSendPort != other.fSendPort)
				return false;
			return true;
		}
		

	}

	protected class SolverRunner implements Runnable
	{
		private boolean fRunning = true;

		private Thread fThread = null;
		
		public void setThread(Thread thread)
		{
			fThread = thread;
		}
		
		public void stop()
		{
			fRunning = false;
			if (fThread != null)
			{
				fThread.interrupt();
			}
			else
			{
				throw new IllegalStateException("Cannot call stop on if setThread() has not been called!");
			}
		}

		@Override
		public void run() {
			while (fRunning)
			{
				SolveMessage aMessage;
				
				try
				{
					aMessage = fMessages.take();
				}
				catch (InterruptedException e)
				{
					continue;
				}
				
				// solve the command if possible
				try
				{
					SolverResult aResult = solve(aMessage.getDataFolderName(), aMessage.getInstanceString(), aMessage.getCutOff(), aMessage.getSeed());
					
					// send the result if needed
					if (aResult.getResult() != SATResult.INTERRUPTED) // do not return if the command was killed.
					{
						String aAnswer = StringUtils.join(new String[]{"ANSWER",aResult.toParsableString()},COMMANDSEP);
						try
						{
							sendLocalMessage(aAnswer,aMessage.getSendPort());
						}
						catch(IOException e1)
						{
							log.warn("Could not send a message back to client ("+e1.getMessage()+").");
						}
					}
				}
				catch(Exception e)
				{
					log.warn("Got an exception while trying to execute a solving command ({}).",e.getMessage());
					try
					{
						sendLocalMessage("ERROR"+COMMANDSEP+e.getMessage(), aMessage.getSendPort());
					}
					catch(IOException e1)
					{
						log.warn("Could not send a message back to client ("+e1.getMessage()+").");
					}
				}
			}

		}

	}
}
