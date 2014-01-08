package ca.ubc.cs.beta.stationpacking.execution.parameters.repackingdata;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.ChannelSpecificConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

import com.beust.jcommander.Parameter;

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
		} catch (IOException e) {
			throw new IllegalArgumentException("Couldn't create station manager ("+e.getMessage()+").");
		}
	}
	
	public IConstraintManager getDACConstraintManager(IStationManager aStationManager)
	{
		Logger log = LoggerFactory.getLogger(RepackingDataParameters.class);
		log.info("Constraint a constraint manager for the given stations...");
		try
		{
			return new ChannelSpecificConstraintManager(aStationManager,ConstraintFilename);
		}
		catch(IOException e)
		{
			throw new IllegalArgumentException("Couldn't create constraint manager ("+e.getMessage()+").");
		}
	}
	

}
