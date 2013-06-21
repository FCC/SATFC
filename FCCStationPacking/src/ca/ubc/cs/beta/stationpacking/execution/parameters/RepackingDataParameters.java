package ca.ubc.cs.beta.stationpacking.execution.parameters;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.datamanagers.DACConstraintManager2;
import ca.ubc.cs.beta.stationpacking.datamanagers.DACStationManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;

@UsageTextField(title="FCC Station Packing Packing Data Options",description="Global parameters required in any station packing problem.")
public class RepackingDataParameters extends AbstractOptions{
	
	@Parameter(names = "-STATIONS_FILE", description = "Station list filename.", required=true)
	public String StationFilename;
	
	@Parameter(names = "-DOMAINS_FILE", description = "Stations' valid channel domains filename.", required=true)
	public String DomainFilename;
	
	@Parameter(names = "-CONSTRAINTS_FILE", description = "Constraints filename.", required=true)
	public String ConstraintFilename;
	
	public DACStationManager getDACStationManager() throws Exception
	{
		Logger log = LoggerFactory.getLogger(RepackingDataParameters.class);
		log.info("Creating a station manager...");
		return new DACStationManager(StationFilename,DomainFilename);
	}
	
	public DACConstraintManager2 getDACConstraintManager(Set<Station> aStations)
	{
		Logger log = LoggerFactory.getLogger(RepackingDataParameters.class);
		log.info("Constraint a constraint manager for the given stations...");
		return new DACConstraintManager2(aStations,ConstraintFilename);
	}
	

}
