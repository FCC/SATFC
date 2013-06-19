package ca.ubc.cs.beta.stationpacking.execution.daemon.client;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.execution.daemon.message.ExceptionMessage;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.IMessage;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.SolverResultMessage;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.StatusMessage;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.StatusMessage.Status;
import ca.ubc.cs.beta.stationpacking.experiment.InstanceGenerationExecutor;

/**
 * Sends messages across UDP. Communication mechanism for daemon solver interface.
 * 
 * @author afrechet
 *
 */
public class ClientCommunicationMechanism {
	
	private static Logger log = LoggerFactory.getLogger(ClientCommunicationMechanism.class);
	
	private final DatagramSocket fClientSocket;
	
	private final InetAddress fIPAdress;
	private final int fServerPort;
	
	private final static int MAXPACKETSIZE = 65000;
	
	/**
	 * Create a mechanism that sends messages to local host at a given port.
	 * @param aServerPort - port to communicate to.
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public ClientCommunicationMechanism(int aServerPort) throws SocketException,UnknownHostException
	{
		fClientSocket = new DatagramSocket();
		fIPAdress = InetAddress.getByName("localhost");
		
		if (aServerPort >= 0 && aServerPort < 1024)
		{
		log.warn("Trying to allocate a port < 1024 which generally requires root priviledges (which aren't necessary and discouraged), this may fail");
		}
		if(aServerPort < -1 || aServerPort > 65535)
		{
		throw new IllegalArgumentException("Port must be in the interval [0,65535]");
		}
		
		fServerPort = aServerPort;
	}
	
	/**
	 * 
	 * @param aSendMessage
	 * @return
	 * @throws IOException
	 */
	public IMessage communicate(IMessage aSendMessage) throws ClassNotFoundException, IOException
	{
		//Check for a closed client.
		if(fClientSocket.isClosed())
		{
			throw new IllegalStateException("Trying to communicate with a used message sender - must create a new object each time.");
		}
		
		//Encode the outgoing message
		ByteArrayOutputStream aBOUT = new ByteArrayOutputStream();
		ObjectOutputStream aOOUT = new ObjectOutputStream(aBOUT);
		aOOUT.writeObject(aSendMessage);
		
		byte[] aSendData = aBOUT.toByteArray();
		if (aSendData.length > MAXPACKETSIZE)
		{
			   throw new IllegalStateException("Response is too big to send to client, please adjust packet size in both client and server (" + aSendData.length + " > " + MAXPACKETSIZE+")");	   
		}
		DatagramPacket aSendPacket = new DatagramPacket(aSendData, aSendData.length, fIPAdress, fServerPort);
		
		//Send the message
		log.info("Sending server the message: "+aSendMessage);
		fClientSocket.send(aSendPacket);
		
		//Receive response messages.
		while(true)
		{
			byte[] aReceiveData = new byte[MAXPACKETSIZE];
			DatagramPacket aReceivePacket = new DatagramPacket(aReceiveData, aReceiveData.length);

			try{
				fClientSocket.receive(aReceivePacket);
				log.info("Received an object");
			}
			catch(SocketException e)
			{
				log.error("Request timed out retrying... (sending to port " + fServerPort +")");
				if(Thread.interrupted())
				{
					//This code is a mess, we do this here for debugging cause generally the JVM is going to shutdown
					throw new IllegalStateException("Thread interrupted");
				}
				return new StatusMessage(Status.CRASHED);
			}
			aReceiveData = aReceivePacket.getData();
			ByteArrayInputStream aBIN = new ByteArrayInputStream(aReceiveData);
			ObjectInputStream aOIN = new ObjectInputStream(aBIN);
			Object aObject = aOIN.readObject();

			if(aObject instanceof Exception)
			{
				ExceptionMessage aExceptionMessage = (ExceptionMessage) aObject;
				fClientSocket.close();
				return aExceptionMessage;
				
			}
			else if(aObject instanceof SolverResultMessage)
			{
				SolverResultMessage aSolverResultMessage = (SolverResultMessage) aObject;
				fClientSocket.close();
				return aSolverResultMessage;
			}
			else if(aObject instanceof StatusMessage)
			{
				StatusMessage aStatusMessage = (StatusMessage) aObject;
				log.info(aStatusMessage.toString());
				
				if(aStatusMessage.getStatus().equals(StatusMessage.Status.TERMINATED) || aStatusMessage.getStatus().equals(StatusMessage.Status.CRASHED))
				{
					fClientSocket.close();
					return aStatusMessage;
				}
			}
			else
			{
				throw new IllegalStateException("Object received is of no recognizable message type.");
			}	
		}

		
	}
	
	
}
