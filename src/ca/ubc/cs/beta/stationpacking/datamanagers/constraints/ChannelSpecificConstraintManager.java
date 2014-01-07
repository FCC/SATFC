package ca.ubc.cs.beta.stationpacking.datamanagers.constraints;

import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;

public class ChannelSpecificConstraintManager implements IConstraintManager{
	
	
	
	
	
	@Override
	public Set<Station> getCOInterferingStations(Station aStation,
			Set<Integer> aChannelRange) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Set<Station> getADJplusInterferingStations(Station aStation,
			Set<Integer> aChannelRange) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean isSatisfyingAssignment(Map<Integer, Set<Station>> aAssignment) {
		// TODO Auto-generated method stub
		return null;
	}

	

}
