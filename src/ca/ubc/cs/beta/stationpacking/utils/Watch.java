package ca.ubc.cs.beta.stationpacking.utils;

/**
 * A lovely watch by Guillaume and Alex <3
 * @author gsauln, alex
 *
 */
public class Watch 
{
	private long time;
	
	public Watch()
	{
		time = -1;
	}
	
	/**
	 * (Re)starts the watch.
	 */
	public void start()
	{
		time = System.currentTimeMillis();
	}
	
	/**
	 * @return the duration in milliseconds since the last call to start. 
	 * The watch needs to be started before every call to stop.
	 * @throws IllegalStateException - if the watch was not started.
	 */
	public long stop()
	{
		if(time>=0)
		{
			long currentTime = time;
			time = -1;
			return System.currentTimeMillis()-currentTime;
		}
		else
		{
			throw new IllegalStateException("Watch was not started.");
		}
	}
	
	
}
