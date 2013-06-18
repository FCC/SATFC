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
import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.execution.daemon.client.ClientCommunicationMechanism;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.ExceptionMessage;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.IMessage;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.SolveMessage;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.SolverResultMessage;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.StatusMessage;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;

public class SolverServer {

	private static Logger log = LoggerFactory
			.getLogger(ClientCommunicationMechanism.class);

	private final ISolver fSolver;
	private final IStationManager fStationManager;

	private final DatagramSocket fServerSocket;
	private final InetAddress fIPAdress;
	private final int fServerPort;

	private final static int MAXPACKETSIZE = 65000;

	public SolverServer(ISolver aSolver, IStationManager aStationManager, int aServerPort) throws SocketException, UnknownHostException {
		
		fSolver = aSolver;
		fStationManager = aStationManager;
		
		fServerPort = aServerPort;
		fServerSocket = new DatagramSocket(fServerPort);
		fIPAdress = InetAddress.getByName("localhost");

	}

	private void sendMessage(IMessage aMessage, int aPort) throws IOException {
		ByteArrayOutputStream aBOUT = new ByteArrayOutputStream();
		ObjectOutputStream aOOUT = new ObjectOutputStream(aBOUT);
		aOOUT.writeObject(aMessage);

		byte[] aSendBytes = aBOUT.toByteArray();

		if (aSendBytes.length > MAXPACKETSIZE) {
			
			sendMessage(new ExceptionMessage(new IllegalArgumentException("Solver tried to send a message that is too large.")), aPort);
			
			log.warn("Response is too big to send to client, please adjust packet size in both client and server ("
					+ aSendBytes.length + " > " + MAXPACKETSIZE + ")");
			log.warn("Dropping message");
			return;
		}

		DatagramPacket sendPacket = new DatagramPacket(aSendBytes,
				aSendBytes.length, fIPAdress, aPort);

		fServerSocket.send(sendPacket);

	}

	public void start() {
		log.info(
				"Solver server is processing requests using a single thread on port {}",
				fServerSocket.getLocalPort());
		try {
			while (true) {
				byte[] aReceiveData = new byte[MAXPACKETSIZE];

				try {
					// Prevent infinite loop from closed socket re-reading
					if (fServerSocket.isClosed())
						return;

					DatagramPacket aReceivePacket = new DatagramPacket(
							aReceiveData, aReceiveData.length);

					fServerSocket.receive(aReceivePacket);

					aReceiveData = aReceivePacket.getData();

					if (!fIPAdress.equals(aReceivePacket.getAddress())) {
						log.warn(
								"Received request from a non-{}, ignoring request from {}",
								fIPAdress, aReceivePacket.getAddress());
						continue;
					}
					log.info("Received a message...");

					ByteArrayInputStream aBIN = new ByteArrayInputStream(
							aReceiveData);
					ObjectInputStream aOIN = new ObjectInputStream(aBIN);

					Object aObject = aOIN.readObject();

					// Process message and prepare to send answer back

					int aSendPort = aReceivePacket.getPort();

					if (aObject instanceof SolveMessage) {
						SolveMessage aSolveMessage = (SolveMessage) aObject;
						log.info("Received solve request {}", aSolveMessage);

						// Send status message
						sendMessage(new StatusMessage(
								StatusMessage.Status.RUNNING), aSendPort);

						HashSet<Integer> aStationIDs = aSolveMessage.getStations();
						HashSet<Integer> aChannels = aSolveMessage.getChannels();

						log.info("Building instance...");
						
						Set<Station> aStations = new HashSet<Station>();
						Station aStation;
						
						
						// Build instance
						for (Integer aID : aStationIDs) {
							if ((aStation = fStationManager.get(aID)) != null)
								aStations.add(aStation);
						}
						Instance aInstance = null;
						if (aStations.size() <= 0 || aChannels.size() <= 0) 
						{
							sendMessage(new ExceptionMessage(new IllegalArgumentException(
									"Invalid Instance: recognized station set is: "
											+ aStations + ", channels are: "
											+ aChannels)),aSendPort);
							
						} 
						else 
						{
							aInstance = new Instance(aStations, aChannels);
					
							log.info("Solving the instance...");
							
							// Solve instance
							SolverResult aSolverResult;
							try {
								aSolverResult = fSolver.solve(aInstance, aSolveMessage.getCutoff());
								
								log.info("Instance solved : "+aSolverResult);
								log.info("Sending answer back");
								// Send back answer
								sendMessage(new SolverResultMessage(aSolverResult), aSendPort);
							
							} catch (Exception e) {
								sendMessage(new ExceptionMessage(e),aSendPort);
							}
						}
						
					} else {
						sendMessage(
								new ExceptionMessage(
										new IllegalStateException(
												"Object received is of no recognizable message type - "+aObject.toString())),
								aSendPort);
						log.warn("Object received is of no recognizable message type.");
					}

				} catch (IOException e) {
					log.error(
							"Unknown IOException occured while recieving data, will wait for next request {}",
							e);
				} catch (ClassNotFoundException e) {
					log.error(
							"Unknown ClassCastException occured while recieving data, will wait for next request {}, (probably garbage data)",
							e);
				}

			}
		} finally {
			try {
				fServerSocket.close();
			} catch (RuntimeException e) {
				log.error("Error occured while shutting down", e);
			}
			log.info("Server shutting down");
		}
	}

}
