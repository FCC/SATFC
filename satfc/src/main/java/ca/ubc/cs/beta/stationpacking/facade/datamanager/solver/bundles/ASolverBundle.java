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
package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.ManagerBundle;

/**
 * Abstract solver bundles that handles data management.
 * @author afrechet
 */
@Slf4j
public abstract class ASolverBundle implements ISolverBundle {

	private final ManagerBundle managerBundle;

	public ASolverBundle(ManagerBundle managerBundle)
	{
		this.managerBundle = managerBundle;
	}

	@Override
	public IStationManager getStationManager()
	{
		return managerBundle.getStationManager();
	}
	
	@Override
	public IConstraintManager getConstraintManager()
	{
		return managerBundle.getConstraintManager();
	}

}
