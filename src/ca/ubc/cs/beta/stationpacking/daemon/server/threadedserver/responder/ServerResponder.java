package ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.responder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumer runnable that is in charge of emitting responses from a queue to a given (UDP) socket.
 * @author afrechet
 */
public class ServerResponder implements Runnable {

private final static int MAXPACKETSIZE = 65000;
	
	private static Logger log = LoggerFactory.getLogger(ServerResponder.class);

	private final DatagramSocket fServerSocket;
	
	private final BlockingQueue<ServerResponse> fServerResponseQueue;
	
	/**
	 * @param aServerResponseQueue - queue to submit responses/communication messages to.
	 * @param aServerSocket - should be listening to local host (127.0.0.1).
	 */
	public ServerResponder(BlockingQueue<ServerResponse> aServerResponseQueue, DatagramSocket aServerSocket) {

		if(aServerSocket.isClosed())	
		{
			throw new IllegalArgumentException("Provided socket is closed.");
		}
		
		fServerSocket = aServerSocket;

		fServerResponseQueue = aServerResponseQueue;
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName("Server Responder Thread");
		// Prevent infinite loop from closed socket re-reading
		if (fServerSocket.isClosed())
		{
			throw new IllegalStateException("Trying to communicate with a closed server socket.");
		}

		log.info("Server's responder broadcasts responses through {} port {} for commands.",fServerSocket.getLocalAddress(),fServerSocket.getLocalPort());
		
		try
		{
			while(true)
			{
				ServerResponse aServerResponse;
				try
				{
					aServerResponse = fServerResponseQueue.take();
				}
				catch(InterruptedException e)
				{
					log.error("Server response queue take method was interrupted, propagating interruption ({}).",e.getMessage());
					Thread.currentThread().interrupt();
					return;
				}
				
				try {
					sendMessage(aServerResponse.getMessage(),aServerResponse.getAddress(),aServerResponse.getPort());
				} catch (IOException e) {
					e.printStackTrace();
					log.error("Could not send message ({}).",e.getMessage());
					throw new IllegalStateException("Error trying to send a message back ");
				}
			}
		}
		finally
		{
			log.info("Server's responder thread shutting down.");
		}
	}
	
	/**
	 * Sends a message to the given address and port through the object's socket.
	 * @param aMessage - a string message to send.
	 * @param aAddress - an address to send to.
	 * @param aPort - the port on the given address to send to.
	 * @throws IOException - if message creation objects cannot be initialized (buffers and streams).
	 */
	private void sendMessage(String aMessage, InetAddress aAddress, int aPort) throws IOException{
		ByteArrayOutputStream aBOUT = new ByteArrayOutputStream();
		ObjectOutputStream aOOUT = new ObjectOutputStream(aBOUT);
		aOOUT.writeObject(aMessage);

		byte[] aSendBytes = aMessage.getBytes(Charset.forName("ASCII"));

		log.info("Sending message back to client.");
		
		if (aSendBytes.length > MAXPACKETSIZE) {
			log.error("Response is too big to send to client, please adjust packet size in both client and server ("
					+ aSendBytes.length + " > " + MAXPACKETSIZE + ")");
			log.error("Dropping message.");

			throw new IllegalArgumentException("Solver tried to send a message that is too large.");			
		}

		DatagramPacket sendPacket = new DatagramPacket(aSendBytes,
				aSendBytes.length, aAddress, aPort);
		
		log.debug("Sending message \"{}\" to "+aAddress+" port "+aPort,aMessage);
		fServerSocket.send(sendPacket);
	}


}
