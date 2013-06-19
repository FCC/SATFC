package ca.ubc.cs.beta.stationpacking.execution.daemon.message;

/**
 * An exception message class.
 * @author afrechet
 *
 */
public class ExceptionMessage implements IMessage{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final Exception fException;
	
	/**
	 * Message containing an exception encountered in the solver.
	 * @param aException - the contained exception.
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
