package ca.ubc.cs.beta.stationpacking.daemon.java.message;

import java.io.Serializable;

/**
 * Message containing status information.
 * @author afrechet
 *
 */
public class StatusMessage implements IMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Possible solver statuses.
	 * @author afrechet
	 *
	 */
	public enum Status implements Serializable{
		RUNNING,TERMINATED,WAITING,CRASHED;
	};
	
	private final Status fStatus;
	
	/**
	 * Message containing status information.
	 * @param aStatus
	 */
	public StatusMessage(Status aStatus)
	{
		fStatus = aStatus;
	}
	
	public Status getStatus()
	{
		return fStatus;
	}
	
	@Override
	public String toString()
	{
		return "Status message :"+fStatus.toString();
	}

	
}
