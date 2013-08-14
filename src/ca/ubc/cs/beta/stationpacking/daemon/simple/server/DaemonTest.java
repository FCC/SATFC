package ca.ubc.cs.beta.stationpacking.daemon.simple.server;

import java.net.SocketException;
import java.net.UnknownHostException;

public class DaemonTest {

	public static void main(String[] args) {
		
		SolverServer aSolverServer;
		try {
			aSolverServer = new SolverServer(49149);
		} catch (SocketException | UnknownHostException e) {
			throw new IllegalArgumentException("Could not establish connection ("+e.getMessage()+").");
		}
		aSolverServer.start();
		
	}

}
