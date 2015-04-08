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
package ca.ubc.cs.beta.stationpacking.execution.parameters.converters;

import java.util.HashMap;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

/**
 * Converter that transforms an integer to integer map string (e.g. "1:2;2:4") to its map representation.  
 * @author afrechet
 */
public class PreviousAssignmentConverter implements IStringConverter<HashMap<Integer,Integer>>{

	@Override
	public final HashMap<Integer,Integer> convert(String value) {
		
		HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
		
		try
		{
			for(String entries : value.split(";"))
			{
				String[] entriesparts = entries.split(":");
				String k = entriesparts[0];
				String v = entriesparts[1];
				
				Integer intk = Integer.valueOf(k);
				Integer intv = Integer.valueOf(v);
				
				map.put(intk, intv);
			}
		}
		catch(ArrayIndexOutOfBoundsException | NumberFormatException e)
		{
			throw new ParameterException("Cannot convert "+value+" to an integer to set of integer map ("+e.getMessage()+")");
		}
		
		return map;
		
	}
		

}
