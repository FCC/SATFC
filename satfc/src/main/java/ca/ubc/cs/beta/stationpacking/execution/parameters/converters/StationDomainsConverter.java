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
package ca.ubc.cs.beta.stationpacking.execution.parameters.converters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

/**
 * Converter that transforms an integer to set of integers map string (e.g. "1:1,2,3;2:2,3,4") to its map representation.  
 * @author afrechet
 */
public class StationDomainsConverter implements IStringConverter<HashMap<Integer,Set<Integer>>>{

	@Override
	public final HashMap<Integer,Set<Integer>> convert(String value) {
		
		HashMap<Integer,Set<Integer>> map = new HashMap<Integer,Set<Integer>>();
		
		try
		{
			for(String entries : value.split(";"))
			{
				String[] entriesparts = entries.split(":");
				String k = entriesparts[0];
				
				Integer intk = Integer.valueOf(k);
				
				String vs = entriesparts[1];
				
				HashSet<Integer> intvs = new HashSet<Integer>();
				for(String v : vs.split(","))
				{
					Integer intv = Integer.valueOf(v);
					intvs.add(intv);
				}
				
				map.put(intk, intvs);
			}
		}
		catch(ArrayIndexOutOfBoundsException | NumberFormatException e)
		{
			throw new ParameterException("Cannot convert "+value+" to an integer to set of integer map.");
		}
		
		return map;
		
	}
		

}
