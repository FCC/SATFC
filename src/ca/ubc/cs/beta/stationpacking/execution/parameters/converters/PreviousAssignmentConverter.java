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
