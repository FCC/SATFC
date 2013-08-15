package ca.ubc.cs.beta.stationpacking.execution.parameters.converters;

import java.io.File;

import ca.ubc.cs.beta.aclib.options.AbstractOptions;

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
	
	public class ConverterOptions extends AbstractOptions
	{
		@Parameter
		@ParameterFile
		//Parameter is never shown.
		public File OptionsFile;
		
		@ParametersDelegate
		public O Options = getInstance();
		
	}
	
	protected abstract O getInstance();

}
