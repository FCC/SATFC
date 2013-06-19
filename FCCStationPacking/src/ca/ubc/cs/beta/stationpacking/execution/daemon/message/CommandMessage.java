package ca.ubc.cs.beta.stationpacking.execution.daemon.message;

import java.io.Serializable;

public class CommandMessage implements IMessage{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Possible solver commands.
	 * @author afrechet
	 *
	 */
	public enum Command implements Serializable
	{
		TERMINATE;
	}
	
	private final Command fCommand;
	
	/**
	 * Message containing a specific command for solver.
	 * @param aStatus
	 */
	public CommandMessage(Command aCommand)
	{
		fCommand = aCommand;
	}
	
	public Command getCommand()
	{
		return fCommand;
	}
	
	@Override
	public String toString()
	{
		return "Command message - "+fCommand.toString();
	}
	
}
