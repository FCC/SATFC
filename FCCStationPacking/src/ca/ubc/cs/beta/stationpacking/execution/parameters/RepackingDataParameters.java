package ca.ubc.cs.beta.stationpacking.execution.parameters;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;

@UsageTextField(title="FCC StationPacking Data Options",description="Parameters required for the creation of station packing problems.")
public class RepackingDataParameters extends AbstractOptions{
	
	@Parameter(names = "-STATIONS_FILE", description = "Station list filename.", required=true)
	private String fStationFilename;
	public String getStationFilename()
	{
		return fStationFilename;
	}
	
	@Parameter(names = "-DOMAINS_FILE", description = "Stations' valid channel domains filename.", required=true)
	private String fDomainFilename;
	public String getDomainFilename()
	{
		return fDomainFilename;
	}
	
	@Parameter(names = "-CONSTRAINTS_FILE", description = "Constraints filename.", required=true)
	private String fConstraintFilename;
	public String getConstraintFilename()
	{
		return fConstraintFilename;
	}

}
