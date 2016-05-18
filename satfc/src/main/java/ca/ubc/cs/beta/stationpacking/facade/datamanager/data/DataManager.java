/**
 * Copyright 2016, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

import ca.ubc.cs.beta.stationpacking.cache.CacheCoordinate;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.ChannelSpecificConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.UnabridgedFormatConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
	@Getter
	private Map<CacheCoordinate, ManagerBundle> coordinateToBundle;

	/**
	 * Create a new (empty) data manager.
	 */
	public DataManager()
	{
		fData = new HashMap<>();
		coordinateToBundle = new HashMap<>();
	}

	public void loadMultipleConstraintSets(String constraintFolder) {
		log.info("Looking in {} for station configuration folders", constraintFolder);
		final File[] stationConfigurationFolders = new File(constraintFolder).listFiles(File::isDirectory);
		log.info("Found {} station configuration folders", stationConfigurationFolders.length);
		Arrays.stream(stationConfigurationFolders).forEach(folder -> {
			try {
				final String path = folder.getAbsolutePath();
				log.info("Adding data for station configuration folder {}", path);
				addData(folder.getAbsolutePath());
				// add cache coordinate to map
				final ManagerBundle bundle = getData(folder.getAbsolutePath());
				log.info("Folder {} corresponds to coordinate {}", folder.getAbsolutePath(), bundle.getCacheCoordinate());
			} catch (FileNotFoundException e) {
				throw new IllegalStateException(folder.getAbsolutePath() + " is not a valid station configuration folder (missing Domain or Interference files?)", e);
			}
		});
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
                log.info("Unabridged format recognized for interference constraints.");
				constraintManager = unabridgedConstraintManager;
			}
			else {
                log.info("Channel specific format recognized for interference constraints.");
				constraintManager = channelspecificConstraintManager;
			}

			final ManagerBundle managerBundle = new ManagerBundle(stationManager, constraintManager, path);
			fData.put(path, managerBundle);
			coordinateToBundle.put(managerBundle.getCacheCoordinate(), managerBundle);
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

	public ManagerBundle getData(CacheCoordinate coordinate) {
		ManagerBundle bundle = coordinateToBundle.get(coordinate);
		Preconditions.checkNotNull(bundle, "Unknown coordinate %s, known coordinates %s", coordinate, coordinateToBundle.keySet());
		return bundle;
	}

}
