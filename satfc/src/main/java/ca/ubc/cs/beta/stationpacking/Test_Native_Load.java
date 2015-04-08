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
package ca.ubc.cs.beta.stationpacking;

import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries.GlueMiniSATLibrary;

import com.sun.jna.Native;

public class Test_Native_Load {

	/**
	 * Run this main class to check if you are able to load libraries using jna.
	 * @param args args[0] path to the library you are trying to load with jna.
	 */
	public static void main(String[] args)
	{
		String lib = args[0];
		Native.loadLibrary(lib, GlueMiniSATLibrary.class);
	}
	
}
