/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
 *
 * This file is part of SATFC.
 *
 * SATFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SATFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
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
