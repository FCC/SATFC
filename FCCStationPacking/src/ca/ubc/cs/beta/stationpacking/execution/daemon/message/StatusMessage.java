package ca.ubc.cs.beta.stationpacking.execution.daemon.message;

import java.io.Serializable;

public class StatusMessage implements IMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public enum Status implements Serializable{
		RUNNING,TERMINATED,WAITING,CRASHED;
	};
	
	private final Status fStatus;
	
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
		return "Status :"+fStatus.toString();
	}

	
}
