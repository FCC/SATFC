package ca.ubc.cs.beta.stationpacking.execution.daemon.message;

public class ExceptionMessage implements IMessage{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final Exception fException;
	
	/**
	 * Message containing an exception encountered in the solver.
	 * @param aSolverResult
	 */
	public ExceptionMessage(Exception aException)
	{
		fException = aException;
	}
	
	public Exception getException()
	{
		return fException;
	}
	
	@Override
	public String toString()
	{
		return "Exception message - "+fException.toString();
	}
}
