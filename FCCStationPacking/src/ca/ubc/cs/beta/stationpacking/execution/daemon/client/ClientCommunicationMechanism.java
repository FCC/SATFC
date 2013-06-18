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
	
	public ClientCommunicationMechanism(int aPort) throws SocketException,UnknownHostException
	{
		fClientSocket = new DatagramSocket();
		fIPAdress = InetAddress.getByName("localhost");
		fServerPort = aPort;
	}
	
	/**
	 * 
	 * @param aSendMessage
	 * @return
	 * @throws IOException
	 */
	public IMessage communicate(IMessage aSendMessage) throws ClassNotFoundException, IOException
	{
		
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
		log.info("Sending solver the message "+aSendMessage);
		fClientSocket.send(aSendPacket);
		
		//Receive response messages
		boolean aIsTerminated = false;
		
		IMessage aOutputMessage = null;
		
		//Wait for answer.
		while(!aIsTerminated)
		{
			byte[] aReceiveData = new byte[MAXPACKETSIZE];
			DatagramPacket aReceivePacket = new DatagramPacket(aReceiveData, aReceiveData.length);

			try{
				fClientSocket.receive(aReceivePacket);
				
				log.info("Received a messsage");
				
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
			
			
			
			if(aObject instanceof ExceptionMessage)
			{
				ExceptionMessage aExceptionMessage = (ExceptionMessage) aObject;
				log.error("Solver ran in exception: "+ aExceptionMessage.getException());
				aOutputMessage = aExceptionMessage;
				aIsTerminated = true;
				
			}
			else if(aObject instanceof SolverResultMessage)
			{
				SolverResultMessage aSolverResultMessage = (SolverResultMessage) aObject;
				aOutputMessage =  aSolverResultMessage;
				aIsTerminated = true;
			}
			else if(aObject instanceof StatusMessage)
			{
				StatusMessage aStatusMessage = (StatusMessage) aObject;
				log.info("Solver response: "+aStatusMessage);
			}
			else
			{
				throw new IllegalStateException("Object received is of no recognizable message type.");
			}			
		}
		//Close socket and return final message
		fClientSocket.close();
		
		return aOutputMessage;
		
		
	}
	
	
}
