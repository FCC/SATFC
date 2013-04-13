package ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.io.*;

//import ca.ubc.cs.beta.stationpacking.data.Constraint;
import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.data.manager.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.experiment.instance.IInstance;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder.ICNFEncoder;


public class NickCNFEncoder implements ICNFEncoder {
	
	String fUNSAT_CNF = "p cnf 1 1\n 1 -1 0\n";

	@Override
	public String encode(IInstance aInstance, IConstraintManager aConstraintManager) {
		Set<Integer> aChannels = aInstance.getChannelRange();
		Set<Station> aStations = aInstance.getStations();
		int numClauses = 0;
		int numChannels = aChannels.size();
		StringBuilder aBuilder = new StringBuilder();
		//Sort the stations
		TreeSet<Station> Stations = new TreeSet<Station>();
		for(Station aStation : aStations) Stations.add(aStation);
		//Sort the channels
		TreeSet<Integer> Channels = new TreeSet<Integer>();
		for(Integer aChannel : aChannels) Channels.add(aChannel);
		//For each station, add corresponding constraints to string
		Set<Station> aInterferingStations;
		final Set<Integer> aEmpty = new HashSet<Integer>();
		Set<Integer> aNegatedVars = new HashSet<Integer>();
		for(Station aStation : Stations){
			//Write BASE clauses corresponding to the station domain
			for(int j = 0; j < numChannels; j++){
				for(int k = j+1; k < numChannels; k++){
					aNegatedVars.clear();
					aNegatedVars.add(get_variable(Stations.headSet(aStation).size(),j,numChannels));
					aNegatedVars.add(get_variable(Stations.headSet(aStation).size(),k,numChannels));
					numClauses += writeClause(aEmpty, aNegatedVars,aBuilder);
				}
			}
			//get domain of aStation, write corresponding base clause
			Set<Integer> aVars = new HashSet<Integer>();
			Set<Integer> aChannelDomain = aStation.getDomain();
			for(Integer aChannel : aChannelDomain){
				if(Channels.contains(aChannel)) {
					aVars.add(get_variable(Stations.headSet(aStation).size(),Channels.headSet(aChannel).size(),numChannels));
				}
				if(aVars.isEmpty()) return fUNSAT_CNF;
				else numClauses += writeClause(aVars,aEmpty,aBuilder);
			}
			//get COInterferingStations, write a clause for each one in Stations (don't duplicate)
			aInterferingStations = aConstraintManager.getCOInterferingStations(aStation); //NA
			for(Station aStation2 : aInterferingStations){
				if(aStation.getID()<aStation2.getID() && Stations.contains(aStation2)){
					for(int j = 0; j < numChannels; j++){
						aNegatedVars.clear();
						aNegatedVars.add(get_variable(Stations.headSet(aStation).size(),j,numChannels));
						aNegatedVars.add(get_variable(Stations.headSet(aStation2).size(),j,numChannels));
						numClauses += writeClause(aEmpty, aNegatedVars,aBuilder);
					}
				}
			}
			//get ADJplusInterferingStations, write a clause for each one in Stations
			aInterferingStations = aConstraintManager.getADJplusInterferingStations(aStation);
			for(Station aStation2 : aInterferingStations){
				if(Stations.contains(aStation2)){
					for(Integer aChannel : Channels){
						if(Channels.higher(aChannel)==aChannel+1){
							aNegatedVars.clear();
							aNegatedVars.add(get_variable(Stations.headSet(aStation).size(),Channels.headSet(aChannel).size(),numChannels));
							aNegatedVars.add(get_variable(Stations.headSet(aStation2).size(),Channels.headSet(aChannel).size()+1,numChannels));
							numClauses += writeClause(aEmpty, aNegatedVars,aBuilder);
						}
					}
				}
			}	
		}
		return "p cnf "+Stations.size()*Channels.size()+" "+numClauses+"\n"+aBuilder.toString();
	}
	
	//NA - Currently assigns each station a variable for each channel
	private Integer get_variable(int aStationNumber, int aChannelNumber, int numChannels){
		return aStationNumber*numChannels+aChannelNumber+1;
	}
	
	private int writeClause(Set<Integer> aVars, Set<Integer> aNegatedVars, StringBuilder aBuilder){
		for(Integer aVar : aVars){ aBuilder.append(aVar+" "); }
		for(Integer aVar : aNegatedVars){ aBuilder.append("-"+aVar+" "); }
		aBuilder.append("0\n");
		return 1;
	}
	
	public Map<Station,Integer> decode(IInstance aInstance, String aCNFAssignment){
		Set<Integer> aChannels = aInstance.getChannelRange();
		Set<Station> aStations = aInstance.getStations();
		int numChannels = aChannels.size();
		//Sort the stations
		TreeSet<Station> Stations = new TreeSet<Station>();
		for(Station aStation : aStations) Stations.add(aStation);
		//Sort the channels
		TreeSet<Integer> Channels = new TreeSet<Integer>();
		for(Integer aChannel : aChannels) Channels.add(aChannel);
		
		//Figure out variable assignment and ensure that no constraints are violated
		try{
			throw new Exception("Method decode not implemented for class NickCNFEncoder.");
		} catch(Exception e){
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return new HashMap<Station,Integer>();
	}


}
