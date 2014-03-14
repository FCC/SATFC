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
