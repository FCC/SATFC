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
package ca.ubc.cs.beta.stationpacking.facade.datamanager.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.ChannelSpecificConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.UnabridgedFormatConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

/**
 * Manages the data contained in different station config directories to make sure they are only read once.
 * @author afrechet
 */
@Slf4j
public class DataManager {
    
	/**
     * File path suffix for a (station config / interference) domain file.
     */
	public static String DOMAIN_FILE = "Domain.csv";
	/**
	 * File path suffix for a (station config / interference) interference constraints file.
	 */
	public static String INTERFERENCES_FILE = "Interference_Paired.csv";
	
	private HashMap<String, ManagerBundle> fData;
	
	/**
	 * Create a new (empty) data manager.
	 */
	public DataManager()
	{
		fData = new HashMap<String, ManagerBundle>();
	}
	
	/**
	 * Adds the data contained in the path to the data manager.  True if the data was added, false
	 * if it was already contained.
	 * @param path to the folder where the data to add is contained.
	 * @return true if the data was added, false if it was already contained.
	 * @throws FileNotFoundException thrown if a file needed to add the data is not found.
	 */
	public boolean addData(String path) throws FileNotFoundException
	{
		
		ManagerBundle bundle = fData.get(path);
		if (bundle != null)
		{
			return false;
		}
		else
		{
			final IStationManager stationManager = new DomainStationManager(path + File.separator + DOMAIN_FILE);

			final IConstraintManager constraintManager;
			
			
			//Try parsing unabridged.
			Exception uaE = null;
			IConstraintManager unabridgedConstraintManager = null;
			try
			{
				unabridgedConstraintManager= new UnabridgedFormatConstraintManager(stationManager, path + File.separator + INTERFERENCES_FILE);
			}
			catch(Exception e)
			{
				uaE = e;
			}
			

			//Try parsing channel specific.
			Exception csE = null;
			IConstraintManager channelspecificConstraintManager = null;
			try
			{
				channelspecificConstraintManager= new ChannelSpecificConstraintManager(stationManager, path + File.separator + INTERFERENCES_FILE);
			}
			catch(Exception e)
			{
				csE = e;
			}
			
			
			if(uaE != null && csE != null)
			{
				log.error("Could not parse interference data both in unabridged and channel specific formats.");
				
				log.error("Unabridged format exception:",uaE);
				log.error("Channel specific format exception:",csE);
				
				throw new IllegalArgumentException("Unrecognized interference constraint format.");
			}
			else if(uaE == null && csE == null)
			{
				throw new IllegalStateException("Provided interference constraint format satisfies both unabridged and channel specific formats.");
			}
			else if(uaE == null)
			{
				if(unabridgedConstraintManager == null)
				{
					throw new IllegalStateException("Parsing of unabridged formatted interference constraints had no exceptions, but corresponding manager is null.");
				}
				log.info("Unabridged format recognized for interference constraints.");
				constraintManager = unabridgedConstraintManager;
			}
			else if(csE == null)
			{
				if(channelspecificConstraintManager == null)
				{
					throw new IllegalStateException("Parsing of channel specific formatted interference constraints had no exceptions, but corresponding manager is null.");
				}
				log.info("Channel specific format recognized for interference constraints.");
				constraintManager = channelspecificConstraintManager;
			}
			else
			{
				throw new IllegalStateException("Could not parse interference constraints with any recognized format.");
			}
			
			fData.put(path, new ManagerBundle(stationManager, constraintManager));
			return true;
		}
	}

	/**
	 * Returns a manager bundle corresponding to the given directory path.  If the bundle does not exist,
	 * it is added (read) into the data manager and then returned.
	 * @param path path to the directory for which to get the bundle.
	 * @return a manager bundle corresponding to the given directory path.
	 * @throws FileNotFoundException thrown if a file needed to add the data is not found.
	 */
	public ManagerBundle getData(String path) throws FileNotFoundException
	{
		ManagerBundle bundle = fData.get(path);
		if (bundle == null)
		{
			addData(path);
			bundle = fData.get(path);
		}
		return bundle;
	}

}
