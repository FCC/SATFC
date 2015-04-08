/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
 *
 * This file is part of satfc.
 *
 * satfc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * satfc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with satfc.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.utils;


/**
 * A pauseable walltime watch.
 * @author afrechet
 */
public class Watch 
{	
	private long fStartTime;
	private double fDuration;
	private boolean fStopped;
	
	/**
	 * Create a watch, initially stopped.
	 */
	public Watch()
	{
		fStartTime = -1;
		fDuration = 0.0;
		fStopped = true;
	}
	
	/**
	 * @return a watch that has been started.
	 */
	public static Watch constructAutoStartWatch()
	{
		Watch watch = new Watch();
		watch.start();
		return watch;
	}
	
	/**
	 * Stopped the watch.
	 * @return True if and only if the watch was running and then was stopped. False if the watch was already stopped.
	 */
	public boolean stop()
	{
		if(!fStopped)
		{
			fDuration += (System.currentTimeMillis()-fStartTime)/1000.0;
			
			fStartTime = -1;
			fStopped = true;
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Start the watch.
	 * @return True if and only if the watch was stopped and then was started. False if the watch was already started.
	 */
	public boolean start()
	{
		if(fStopped)
		{
			fStartTime = System.currentTimeMillis();
			fStopped = false;
			return true;
		}
		else
		{
			return false;
		}
	
	}
	
	/**
	 * Reset the watch.
	 */
	public void reset()
	{
		fStartTime = -1;
		fDuration = 0.0;
		fStopped = true;
	}
	
	/**
	 * @return the ellapsed time (s) between since the initial start (or reset) of the watch, the last end time being now if the watch
	 * is still running.
	 */
	public double getElapsedTime()
	{
		if(fStopped)
		{
			return fDuration;
		}
		else
		{
			return fDuration + (System.currentTimeMillis()-fStartTime)/1000.0;
		}
		
		
	}
	
	
	
}
