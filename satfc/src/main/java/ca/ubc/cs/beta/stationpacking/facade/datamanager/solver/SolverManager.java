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
package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.ManagerBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ISolverBundleFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages the solvers & data corresponding to different directories to make sure it is only read once.
 */
@Slf4j
public class SolverManager implements AutoCloseable {

	private HashMap<String, ISolverBundle> fSolverData;
	private ISolverBundleFactory fSolverBundleFactory;
	private DataManager fDataManager;
	
	/**
	 * Creates a solver manager that will use the given factory to create the solvers when needed.
	 * @param aSolverBundleFactory a solver bundle factory to create solver bundles.
	 */
    public SolverManager(ISolverBundleFactory aSolverBundleFactory, DataManager aDataManager) {
        fDataManager = aDataManager;
        fSolverBundleFactory = aSolverBundleFactory;
        fSolverData = new HashMap<>();
    }

    /**
	 * Adds the data (domain, interferences) contained in the path and a corresponding new solver to the manager. 
	 * @param path path to add the data from.
	 * @return true if the data was added and solver created, false if it was already contained.
	 * @throws FileNotFoundException thrown if a file needed to add the data is not found.
	 */
	public boolean addData(String path) throws FileNotFoundException
	{	
		log.debug("Adding data from {} to solver manager.",path);
		
		ISolverBundle bundle = fSolverData.get(path);
		if (bundle != null)
		{
			return false;
		}
		else
		{
			ManagerBundle dataBundle = fDataManager.getData(path);
			ISolverBundle solverbundle = fSolverBundleFactory.getBundle(dataBundle);
			
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
			log.warn("Requested data from {} not available, will try to add it.",path);
			addData(path);
			bundle = fSolverData.get(path);
		}
		return bundle;
	}

	@Override
	public void close() throws Exception
	{
		Set<String> keys = new HashSet<String>(fSolverData.keySet());
		for (String path : keys)
		{
			ISolverBundle bundle = fSolverData.get(path);
			bundle.close();
			fSolverData.remove(path);
		}
	}
	
}
