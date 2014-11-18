package ca.ubc.cs.beta.stationpacking.facade.datamanager.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.ChannelSpecificConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.UnabridgedFormatConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

/**
 * Manages the data contained in different station config directories to make sure they are only read once.
 * @author afrechet
 */
public class DataManager {
    
    /**
     * File path suffix for a (station config / interference) domain file.
     */
	public static String DOMAIN_FILE = File.separator+"Domain.csv'";
	/**
	 * File path suffix for a (station config / interference) interference constraints file.
	 */
	public static String INTERFERENCES_FILE = File.separator+"Interference_Paired.csv";
	
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
			IStationManager stationManager = new DomainStationManager(path+DOMAIN_FILE);
			//IConstraintManager constraintManager = new ChannelSpecificConstraintManager(stationManager, path+INTERFERENCES_FILE);
			IConstraintManager constraintManager = new UnabridgedFormatConstraintManager(stationManager, path+INTERFERENCES_FILE);
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
