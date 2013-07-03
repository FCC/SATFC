package ca.ubc.cs.beta.stationpacking.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility ticker class to print counts.
 * @author afrechet
 *
 */
public class Ticker {

	private static Logger log = LoggerFactory.getLogger(Ticker.class);
	
	private final int fTickRate;
	private final int fMaxTick;

	private final String fLogMessage;
	
	private int fTick;
	
	public Ticker(int aTickRate, int aMaxTick, String aLogMessage)
	{
		fTickRate = aTickRate;
		fMaxTick = aMaxTick;
		fLogMessage = aLogMessage;
		fTick = 0;
	}

	public void tick()
	{
		fTick ++;
		
		if(fTick%fTickRate < (fTick-1)%fTickRate)
		{
			logMessage();
		}
	}
	
	private void logMessage()
	{
		if(fMaxTick<0)
		{
			log.info(fLogMessage,fTick);
		}
		else
		{
			log.info(fLogMessage,fTick,fMaxTick);
		}
	}
	

	
}
