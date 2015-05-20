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

import java.io.File;

import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParameterFile;
import com.beust.jcommander.ParametersDelegate;

/**
 * Converter that reads the content of a given filename in an args string array and tries to parse it as 
 * a O abstract options.
 * @author afrechet
 * @param <O> - the type of main abstract options.
 */
public abstract class AbstractFromFileConverter<O extends AbstractOptions> implements IStringConverter<O>{

	@Override
	public final O convert(String value) {
		
		ConverterOptions aTempOptions = new ConverterOptions();
		
		aTempOptions.OptionsFile = new File(value);
		
		JCommander aParameterParser = new JCommander(aTempOptions);
		try
		{
			aParameterParser.parse();
		}
		catch (ParameterException aParameterException)
		{
			throw new ParameterException("Error while reading file: "+ value + " ("+aParameterException.getMessage()+")");
		}
		return aTempOptions.Options;
	}
	
	/**
	 * Generic implicit converter options that serves as a delegate that is parsed from an options file.
	 * @author afrechet
	 *
	 */
	public class ConverterOptions extends AbstractOptions
	{  
	    /**
	     * File from which generic options are read.
	     */
		@Parameter
		@ParameterFile
		//Parameter is never shown.
		public File OptionsFile;
		
		/**
		 * Generic options read from file.
		 */
		@ParametersDelegate
		public O Options = getInstance();
		
	}
	
	/**
	 * @return constructs an empty options instance to be populated in the file read.
	 */
	protected abstract O getInstance();

}
