package ca.ubc.cs.beta.stationpacking.daemon.simple.server;

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
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.daemon.simple.datamanager.ISolverFactory;
import ca.ubc.cs.beta.stationpacking.daemon.simple.datamanager.SolverBundle;
import ca.ubc.cs.beta.stationpacking.daemon.simple.datamanager.SolverManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;

/**
 * Wrapper around an ISolver and a StationManager that takes care of receiving problem instances and various misc commands from UDP localhost, and communicate
 * result and other information back.
 * 
 * @author afrechet
 *
 */
public class SolverServer {
	
	private static Logger log = LoggerFactory.getLogger(SolverServer.class);
	
	/*
	 * Solver fields.
	 */
	private final SolverManager fSolverManager;
	private final Random fRandom;
	
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

	public SolverServer(int aServerPort, ISolverFactory solverFactory) throws SocketException, UnknownHostException {
		
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
		fSolverManager = new SolverManager(solverFactory);
		fRandom = new Random();
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
		
		log.info("Solver server is processing requests using a single thread on port {}.",fServerSocket.getLocalPort());
		
		try {
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
					sendLocalMessage(StringUtils.join(new String[]{"TEST","You are the best."},COMMANDSEP),aSendPort);
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
				log.info("NOM NOM NOM MUNCH MUNCH MUNCH");
				
				try
				{
					String[] aMessageParts = aMessage.split(COMMANDSEP);
					if(aMessageParts.length!=4)
					{
						throw new IllegalArgumentException("Solving command does not have necessary additional information.");
					}
					
					String aDataFoldername = aMessageParts[1];
					String aInstanceString = aMessageParts[2];
					double aCutoff = Double.valueOf(aMessageParts[3]);
					
					SolverResult aResult = solve(aDataFoldername, aInstanceString, aCutoff);
					
					String aAnswer = StringUtils.join(new String[]{"ANSWER","SO EASY, JUST SOLVED IT!"},COMMANDSEP);
					try
					{
						sendLocalMessage(aAnswer,aSendPort);
					}
					catch(IOException e1)
					{
						log.warn("Could not send a message back to client ("+e1.getMessage()+").");
					}
					
				}
				catch(Exception e)
				{
					log.warn("Got an exception while trying to execute a solving command ({}).",e.getMessage());
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
	
	private SolverResult solve(String aDataFoldername, String aInstanceString, double aCutoff) throws FileNotFoundException
	{
		SolverBundle bundle = fSolverManager.getData(aDataFoldername);
		
		IStationManager aStationManager = bundle.getStationManager();
		StationPackingInstance aInstance = StationPackingInstance.valueOf(aInstanceString, aStationManager);
							
		ISolver solver = bundle.getSolver();
		SolverResult aResult = solver.solve(aInstance, aCutoff, fRandom.nextInt());
		
		return aResult;
	}


}
