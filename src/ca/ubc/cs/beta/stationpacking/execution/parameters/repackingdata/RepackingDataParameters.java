package ca.ubc.cs.beta.stationpacking.execution.parameters.repackingdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.datamanagers.DACConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.IStationManager;

@UsageTextField(title="FCC Station Packing Packing Data Options",description="Global parameters required in any station packing problem.")
public class RepackingDataParameters extends AbstractOptions{
	
	@Parameter(names = "-DOMAINS_FILE", description = "Stations' valid channel domains filename (uses DAC formatting from March 2013).", required=true)
	public String DomainFilename;
	
	@Parameter(names = "-CONSTRAINTS_FILE", description = "Constraints filename (uses DAC formatting from March 2013).", required=true)
	public String ConstraintFilename;
	
	public IStationManager getDACStationManager()
	{
		Logger log = LoggerFactory.getLogger(RepackingDataParameters.class);
		log.info("Creating a station manager...");
		try {
			return new DomainStationManager(DomainFilename);
		} catch (Exception e) {
			throw new IllegalArgumentException("Couldn't create station manager "+e.getMessage());
		}
	}
	
	public IConstraintManager getDACConstraintManager(IStationManager aStationManager)
	{
		Logger log = LoggerFactory.getLogger(RepackingDataParameters.class);
		log.info("Constraint a constraint manager for the given stations...");
		return new DACConstraintManager(aStationManager,ConstraintFilename);
	}
	

}
