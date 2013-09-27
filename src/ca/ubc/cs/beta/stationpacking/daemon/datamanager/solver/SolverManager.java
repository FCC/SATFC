package ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.daemon.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.data.ManagerBundle;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.ISolverBundleFactory;

/**
 * Manages the solvers & data corresponding to different directories to make sure it is only read once.
 */
public class SolverManager {
	private static Logger log = LoggerFactory.getLogger(SolverManager.class);
	
	private HashMap<String, ISolverBundle> fSolverData;
	private ISolverBundleFactory fSolverBundleFactory;
	private DataManager fDataManager;
	
	/**
	 * Creates a solver manager that will use the given factory to create the solvers when needed.
	 * @param aSolverBundleFactory a solver bundle factory to create solver bundles.
	 */
	public SolverManager(ISolverBundleFactory aSolverBundleFactory)
	{
		fSolverBundleFactory = aSolverBundleFactory;
		fSolverData = new HashMap<String, ISolverBundle>();
		fDataManager = new DataManager();
	}
	
	/**
	 * @param path - a data path.
	 * @return true if and only if the solver manager contains solving data for the provided path.
	 */
	public boolean hasData(String path)
	{
		return fSolverData.containsKey(path);
	}
	
	/**
	 * Adds the data (domain, interferences) contained in the path and a corresponding new solver to the manager. 
	 * @param path path to add the data from.
	 * @return true if the data was added and solver created, false if it was already contained.
	 * @throws FileNotFoundException thrown if a file needed to add the data is not found.
	 */
	public boolean addData(String path) throws FileNotFoundException
	{	
		log.info("Adding data from {} to solver manager.",path);
		
		ISolverBundle bundle = fSolverData.get(path);
		if (bundle != null)
		{
			return false;
		}
		else
		{
			ManagerBundle dataBundle = fDataManager.getData(path);
			ISolverBundle solverbundle = fSolverBundleFactory.getBundle(dataBundle.getStationManager(), dataBundle.getConstraintManager());
			
			fSolverData.put(path, solverbundle);
			return true;
		}
	}
	
	/**
	 * Returns a solver bundle corresponding to the given directory path.  If the bundle does not exist,
	 * it is added (read) into the manager and then returned.
	 * @param path path to the directory for which to get the bundle.
	 * @return a solver bundle corresponding to the given directory path.
	 * @throws FileNotFoundException thrown if a file needed to add the data is not found.
	 */
	public ISolverBundle getData(String path) throws FileNotFoundException
	{
		ISolverBundle bundle = fSolverData.get(path);
		if (bundle == null)
		{
			log.info("Requested data from {} not available, will try to add it.",path);
			addData(path);
			bundle = fSolverData.get(path);
		}
		return bundle;
	}

	/**
	 * Calls notify shutdown on all the solvers contained in the manager and removes them.
	 */
	public void notifyShutdown()
	{
		HashSet<String> keys = new HashSet<String>(fSolverData.keySet());
		for (String path : keys)
		{
			ISolverBundle bundle = fSolverData.get(path);
			bundle.notifyShutdown();
			fSolverData.remove(path);
		}
	}
	
}
