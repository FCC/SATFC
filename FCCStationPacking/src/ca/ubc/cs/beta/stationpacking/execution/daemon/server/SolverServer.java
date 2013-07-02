package ca.ubc.cs.beta.stationpacking.execution.daemon.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.datamanagers.IStationManager;
import ca.ubc.cs.beta.stationpacking.datastructures.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.execution.daemon.client.ClientCommunicationMechanism;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.CommandMessage;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.CommandMessage.Command;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.ExceptionMessage;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.IMessage;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.SolveMessage;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.SolverResultMessage;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.StatusMessage;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;

/**
 * Wrapper around an ISolver and a StationManager that takes care of receiving problem instances and various misc commands from UDP localhost, and communicate
 * result and other information back.
 * 
 * @author afrechet
 *
 */
public class SolverServer {

	private static Logger log = LoggerFactory.getLogger(ClientCommunicationMechanism.class);

	private final ISolver fSolver;
	private final IStationManager fStationManager;

	private final DatagramSocket fServerSocket;
	private final InetAddress fIPAdress;
	private final int fServerPort;

	private final static int MAXPACKETSIZE = 65000;

	public SolverServer(ISolver aSolver, IStationManager aStationManager, int aServerPort) throws SocketException, UnknownHostException {
		
		fSolver = aSolver;
		fStationManager = aStationManager;
		
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

	}
	
	/**
	 * @param aMessage - a message to send.
	 * @param aPort - the port location on localhost.
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	private void sendMessage(IMessage aMessage, int aPort) throws IOException,IllegalArgumentException {
		ByteArrayOutputStream aBOUT = new ByteArrayOutputStream();
		ObjectOutputStream aOOUT = new ObjectOutputStream(aBOUT);
		aOOUT.writeObject(aMessage);

		byte[] aSendBytes = aBOUT.toByteArray();

		if (aSendBytes.length > MAXPACKETSIZE) {
			log.warn("Response is too big to send to client, please adjust packet size in both client and server ("
					+ aSendBytes.length + " > " + MAXPACKETSIZE + ")");
			log.warn("Dropping message");
			
			throw new IllegalArgumentException("Solver tried to send a message that is too large.");			
		}

		DatagramPacket sendPacket = new DatagramPacket(aSendBytes,
				aSendBytes.length, fIPAdress, aPort);

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
		log.info("Server shutting down");
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
		
		log.info("Solver server is processing requests using a single thread on port {}",fServerSocket.getLocalPort());
		
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
								"Received request from a non-{}, ignoring request from {}",
								fIPAdress, aReceivePacket.getAddress());
						continue;
					}
					
					log.info("Received a packet.");
					aReceiveData = aReceivePacket.getData();
					ByteArrayInputStream aBIN = new ByteArrayInputStream(
							aReceiveData);
					ObjectInputStream aOIN = new ObjectInputStream(aBIN);
					Object aObject = aOIN.readObject();
					
					int aSendPort = aReceivePacket.getPort();

					//Process message
					if (aObject instanceof SolveMessage) {
						//Solve message
						SolveMessage aSolveMessage = (SolveMessage) aObject;
						log.info(aSolveMessage.toString());

						// Send back status message
						sendMessage(new StatusMessage(StatusMessage.Status.RUNNING), aSendPort);

						//Build instance & solve
						log.info("Building instance...");
						HashSet<Integer> aStationIDs = aSolveMessage.getStationIDs();
						HashSet<Integer> aChannels = aSolveMessage.getChannels();
						Set<Station> aStations = new HashSet<Station>();
						Station aStation;
						for (Integer aID : aStationIDs) {
							if ((aStation = fStationManager.get(aID)) != null)
								aStations.add(aStation);
						}
						StationPackingInstance aInstance = null;
						if (aStations.size() <= 0 || aChannels.size() <= 0) 
						{
							sendMessage(new ExceptionMessage(new IllegalArgumentException("Invalid Instance: recognized station set is: "+ aStations + ", channels are: "+ aChannels)),aSendPort);
						} 
						else 
						{
							aInstance = new StationPackingInstance(aStations, aChannels);
							log.info("Solving the instance...");
							SolverResult aSolverResult;
							try {
								aSolverResult = fSolver.solve(aInstance, aSolveMessage.getCutoff(), aSolveMessage.getSeed());
								
								//Send the result back
								log.info("Instance solved");
								log.info(aSolverResult.toString());
								log.info("Sending answer back");
								try
								{
									sendMessage(new SolverResultMessage(aSolverResult), aSendPort);
								}
								catch(IllegalArgumentException e)
								{
									log.warn("Failed to send solver result ({})",e);
									sendMessage(new ExceptionMessage(e), aSendPort);
								}
							
							} catch (Exception e) {
								log.warn("Failed to solve the instance ({})",e);
								sendMessage(new ExceptionMessage(e),aSendPort);
							}
						}
						
					} 
					else if (aObject instanceof CommandMessage)
					{
						//Command message
						CommandMessage aCommandMessage = (CommandMessage) aObject;
					
						log.info(aCommandMessage.toString());
						
						Command aCommand = aCommandMessage.getCommand();
						switch(aCommand)
						{
							case TERMINATE:
								sendMessage(new StatusMessage(StatusMessage.Status.TERMINATED), aSendPort);
								notifyShutdown();
								return;
							default:
								break;
						}
						
					}
					else {
						//Failed recognizing the message.
						sendMessage(new ExceptionMessage(new IllegalStateException("Object received is of no recognizable message type - "+aObject.toString())),aSendPort);
						log.warn("Object received is of no recognizable message type.");
					}

				} catch (IOException e) {
					log.error("Unknown IOException occured while recieving data, will wait for next request {}",e);
				} catch (ClassNotFoundException e) {
					log.error("Unknown ClassCastException occured while recieving data, will wait for next request {}, (probably garbage data)",e);
				}

			}
		} 
		finally 
		{
			notifyShutdown();
		}
	}

}
