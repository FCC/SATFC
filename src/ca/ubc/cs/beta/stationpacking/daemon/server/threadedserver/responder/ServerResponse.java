package ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.responder;

import java.net.InetAddress;

public class ServerResponse {

	private final String fMessage;
	private final InetAddress fAddress;
	private final int fPort;
	
	public ServerResponse(String aMessage, InetAddress aAddress, int aPort)
	{
		fMessage = aMessage;
		fAddress = aAddress;
		fPort = aPort;
	}

	public String getMessage() {
		return fMessage;
	}

	public InetAddress getAddress() {
		return fAddress;
	}

	public int getPort() {
		return fPort;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fAddress == null) ? 0 : fAddress.hashCode());
		result = prime * result
				+ ((fMessage == null) ? 0 : fMessage.hashCode());
		result = prime * result + fPort;
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
		ServerResponse other = (ServerResponse) obj;
		if (fAddress == null) {
			if (other.fAddress != null)
				return false;
		} else if (!fAddress.equals(other.fAddress))
			return false;
		if (fMessage == null) {
			if (other.fMessage != null)
				return false;
		} else if (!fMessage.equals(other.fMessage))
			return false;
		if (fPort != other.fPort)
			return false;
		return true;
	}

	

}
