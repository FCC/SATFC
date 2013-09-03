package ca.ubc.cs.beta.stationpacking.execution.parameters.solver.daemon;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class PortValidator implements IParameterValidator
	{

		@Override
		public void validate(String name, String value)
				throws ParameterException {
			
			int aPort = Integer.valueOf(value);
			if (aPort >= 0 && aPort < 1024)
			{
				throw new ParameterException("Trying to allocate a port < 1024 which generally requires root priviledges (which aren't necessary and discouraged), this may fail");
			}
			if(aPort < -1 || aPort > 65535)
			{
				throw new ParameterException("Port must be in the interval [0,65535]");
			}
			
			
		}
		
	}