package ca.ubc.cs.beta.stationpacking.solver.IncrementalSolver;

import java.util.HashSet;
import java.util.Set;


public class InstanceSubmitter {

	public InstanceSubmitter(String[] instanceEncoding){
		if(instanceEncoding.length == 2){
			String[] aStationStrings = instanceEncoding[0].substring(instanceEncoding[0].indexOf(":")+1).split(",");
			Set<Integer> aStationIDs = stringArrayToIntegerSet(aStationStrings);
			String[] aChannelStrings = instanceEncoding[1].substring(instanceEncoding[1].indexOf(":")+1).split(",");
			Set<Integer> aChannels = stringArrayToIntegerSet(aChannelStrings);
			//Send message with aStationIDs, aChannels
			//Receive assignment back and output it in some format
		} else {
			//Throw some error - bad parameters provided
		}
	}
	
	private static Set<Integer> stringArrayToIntegerSet(String[] aStrings){
		Set<Integer> aIntegers = new HashSet<Integer>();
		for(int i = 0; i < aStrings.length; i++){
			if(aStrings[i].length() > 0){
				Integer aID = Integer.parseInt(aStrings[i]);
				aIntegers.add(aID);
			}
		}
		//Catch bad format if parseInt fails
		return aIntegers;
	}
}
