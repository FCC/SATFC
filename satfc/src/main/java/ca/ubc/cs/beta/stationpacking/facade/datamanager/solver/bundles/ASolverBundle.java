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
package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

/**
 * Abstract solver bundles that handles data management.
 * @author afrechet
 */
public abstract class ASolverBundle implements ISolverBundle{

	private final IStationManager fStationManager;
	private final IConstraintManager fConstraintManager;
	
	/**
	 * Create an abstract solver bundle with the given data management objects.
	 * @param aStationManager - manages stations.
	 * @param aConstraintManager - manages constraints.
	 */
	public ASolverBundle(IStationManager aStationManager, IConstraintManager aConstraintManager)
	{
		fStationManager = aStationManager;
		fConstraintManager = aConstraintManager;
	}
	
	@Override
	public IStationManager getStationManager()
	{
		return fStationManager;
	}
	
	@Override
	public IConstraintManager getConstraintManager()
	{
		return fConstraintManager;
	}
	

}
