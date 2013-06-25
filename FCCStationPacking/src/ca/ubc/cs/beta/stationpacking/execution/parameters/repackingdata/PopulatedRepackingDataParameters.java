package ca.ubc.cs.beta.stationpacking.execution.parameters.repackingdata;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.datamanagers.DACConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.PopulatedDomainStationManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;

@UsageTextField(title="FCC Station Packing Packing Data Options",description="Global parameters required in any station packing problem.")
public class PopulatedRepackingDataParameters extends AbstractOptions{
	
	@Parameter(names = "-STATIONS_FILE", description = "Station list filename.", required=true)
	public String StationFilename;
	
	@Parameter(names = "-DOMAINS_FILE", description = "Stations' valid channel domains filename (uses DAC formatting from March 2013).", required=true)
	public String DomainFilename;
	
	@Parameter(names = "-CONSTRAINTS_FILE", description = "Constraints filename (uses DAC formatting from March 2013).", required=true)
	public String ConstraintFilename;
	
	public PopulatedDomainStationManager getDACStationManager() throws Exception
	{
		Logger log = LoggerFactory.getLogger(PopulatedRepackingDataParameters.class);
		log.info("Creating a station manager...");
		return new PopulatedDomainStationManager(StationFilename,DomainFilename);
	}
	
	public IConstraintManager getDACConstraintManager(Set<Station> aStations)
	{
		Logger log = LoggerFactory.getLogger(PopulatedRepackingDataParameters.class);
		log.info("Constraint a constraint manager for the given stations...");
		return new DACConstraintManager(aStations,ConstraintFilename);
	}
	

}
