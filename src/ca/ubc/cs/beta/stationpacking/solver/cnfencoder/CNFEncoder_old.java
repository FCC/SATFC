package ca.ubc.cs.beta.stationpacking.solver.cnfencoder;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solver.sat.Clause_old;


//NA - I use LinkedHashSet because I want to get a consistent ordering for debugging purposes


public class CNFEncoder_old implements ICNFEncoder_old {

	
	static final Set<Clause_old> fUNSAT_CLAUSES = new HashSet<Clause_old>();
	//static final Set<Integer> aEmpty = new HashSet<Integer>();
	
	Map<Integer,Integer> fExternalToInternal = new HashMap<Integer,Integer>();
	Map<Integer,Integer> fInternalToExternal = new HashMap<Integer,Integer>();
	Integer fMaxChannel = 100;
	
	public CNFEncoder_old(Set<Station> aStations){
		
		//Set<Station> aStations = aInstance.getStations();
		//Set<Integer> aChannels = aInstance.getChannels();
		Set<Integer> aSingleVar = new HashSet<Integer>();
		aSingleVar.add(1);
		fUNSAT_CLAUSES.add(new Clause_old(aSingleVar,new HashSet<Integer>()));
		fUNSAT_CLAUSES.add(new Clause_old(new HashSet<Integer>(),aSingleVar));
		Map<Station,Integer> aInternalIDs = getInternalIDs(aStations);
		for(Station aStation : aInternalIDs.keySet()){
			fExternalToInternal.put(aStation.getID(), aInternalIDs.get(aStation));
			fInternalToExternal.put(aInternalIDs.get(aStation),aStation.getID());
		}
		//for(Integer aChannel : aChannels) if(fMaxChannel < aChannel) fMaxChannel = aChannel;
		
		int aCount = aInternalIDs.size()+1;
		for(int i = 0; i <= 10000; i++){
			if(fExternalToInternal.get(i)==null){
				fExternalToInternal.put(i, aCount);
				fInternalToExternal.put(aCount++, i);
			}
		}
		
	}
	
	/* NA - the new encode method
	 * Currently, each call to encode is isolated (I store no state)
	 * In order to create consistent encodings across calls, I need consistent internal IDs.
	 * One solution: don't use internal IDs
	 * Another solution: store state in constructor
	 */
	public Set<Clause_old> encode(StationPackingInstance aInstance, IConstraintManager aConstraintManager){
		Set<Clause_old> aClauses = new LinkedHashSet<Clause_old>();
		try{
			aClauses.addAll(getBaseClauses(aInstance));
		} catch(Exception e){
			//Thrown if a station has an empty domain
			return fUNSAT_CLAUSES;
		}
		aClauses.addAll(getConstraintClauses(aInstance, aConstraintManager));
		return aClauses;
	}
	
	//Do some checking to make sure that this encodes the right instance?
	//I could eliminate both checks and force caller to verify legality of assignment
	//Or I could pass a ConstraintManager and have decode do full verification (exactly one channel per station, in that station's domain, no constraints violated)
	@Override
	public Map<Integer,Set<Station>> decode(StationPackingInstance aInstance, Clause_old aAssignment){
		
		Set<Integer> aInstanceDomain = aInstance.getChannels();
		Map<Station,Integer> aAssignmentMap = new HashMap<Station,Integer>();
		for(Integer aVar : aAssignment.getVars()){
			Integer aChannel = varToChannel(aVar);
			Station aStation = aInstance.getStation(varToStationID(aVar));
			if(aStation != null){
				if(aInstanceDomain.contains(aChannel)&&aStation.getDomain().contains(aChannel)){
					Integer aPreviousChannel = aAssignmentMap.put(aStation, aChannel);
					if(!(aPreviousChannel==null)){
						//System.out.println(aStation+" assigned to both Channel "+aChannel+" and Channel "+aPreviousChannel);
						throw new IllegalStateException(aStation+" assigned to both Channel "+aChannel+" and Channel "+aPreviousChannel);
					}
				} else {
					throw new IllegalStateException(aStation+" assigned to channel "+aChannel+", which is not feasible.");
				}
			}
		}
		if(aAssignmentMap.size()!=aInstance.getStations().size()) 
			throw new IllegalStateException("Instance has "+aInstance.getStations().size()+" stations, but only "+aAssignmentMap.size()+" were assigned.");
		Map<Integer,Set<Station>> aChannelAssignments = new HashMap<Integer,Set<Station>>();
		
		for(Station aStation : aAssignmentMap.keySet()){
			if(!aChannelAssignments.containsKey(aAssignmentMap.get(aStation)))
			{
				aChannelAssignments.put(aAssignmentMap.get(aStation), new HashSet<Station>());
			}			
			aChannelAssignments.get(aAssignmentMap.get(aStation)).add(aStation);
		}
		return aChannelAssignments;
	}
		
	private Set<Clause_old> getBaseClauses(StationPackingInstance aInstance) throws Exception{
		Set<Clause_old> aBaseClauseSet = new LinkedHashSet<Clause_old>();
		final Set<Integer> aInstanceDomain = aInstance.getChannels();
		
		for(Station aStation : aInstance.getStations()){
			
			//Get variables corresponding to the pair (aStation,aChannel) for each aChannel that is in aInstance and is in the domain of aStation
			Set<Integer> aStationChannelPairVars = new HashSet<Integer>();
			Set<Integer> aStationDomain = aStation.getDomain();
			aStationDomain.retainAll(aInstanceDomain);
			for(Integer aChannel : aStationDomain){
				aStationChannelPairVars.add(stationChannelPairToVar(aStation.getID(),aChannel));
			}
			
			//Check to see if aStation can be legally assigned (ignoring constraints)
			if(! aStationChannelPairVars.isEmpty()){
				
				//Encode that aStation must be assigned to at least one channel in its domain
				aBaseClauseSet.add(new Clause_old(aStationChannelPairVars,new HashSet<Integer>()));
				//Encode that aStation must be assigned to at most one channel in its domain
				for(Integer aVar1 : aStationChannelPairVars){
					for(Integer aVar2 : aStationChannelPairVars){
						if(aVar1 < aVar2){
							Clause_old aClause = new Clause_old();
							aClause.addLiteral(aVar1,false);
							aClause.addLiteral(aVar2,false);
							aBaseClauseSet.add(aClause);
						}
					}
				}
			} else {
				throw new IllegalStateException("Station "+aStation+" has empty domian.");
			}
		}
		return aBaseClauseSet;
	}
	
	/* NA: Used for debugging
	public void translate(Clause aAssignment,Map<Integer,Integer> aMap){
		for(Integer aVar : aAssignment.getVars()){
			System.out.print("["+aMap.get(aVar)+"]"+aVar+"("+varToStationID(aVar)+":"+varToChannel(aVar)+"),");
		}
		System.out.println();
	}
	*/
	
	private Set<Clause_old> getConstraintClauses(StationPackingInstance aInstance, IConstraintManager aConstraintManager){
		Set<Clause_old> aConstraintClauseSet = new LinkedHashSet<Clause_old>();
		Set<Integer> aInstanceDomain = aInstance.getChannels();
		for(Station aStation : aInstance.getStations()){
			
			//Encode co-channel constraints involving aStation
			Set<Station> aCOInterferingStations = aConstraintManager.getCOInterferingStations(aStation,aInstanceDomain);
			aCOInterferingStations.retainAll(aInstance.getStations());
			for(Station aInterferingStation : aCOInterferingStations){
				for(Integer aChannel : aInstanceDomain){
					//Is this check worth doing? If we didn't do it, the encoding is still valid, but we include superfluous variables
					if(aStation.getDomain().contains(aChannel)&&aInterferingStation.getDomain().contains(aChannel)){
						Clause_old aClause = new Clause_old();
						aClause.addLiteral(stationChannelPairToVar(aStation.getID(),aChannel),false);
						aClause.addLiteral(stationChannelPairToVar(aInterferingStation.getID(),aChannel),false);
						aConstraintClauseSet.add(aClause);
					}
				}
			}
			
			//Encode adjacent-channel constraints involving aStation
			Set<Station> aADJInterferingStations = aConstraintManager.getADJplusInterferingStations(aStation, aInstanceDomain);
			aADJInterferingStations.retainAll(aInstance.getStations());
			for(Station aInterferingStation : aADJInterferingStations){
				for(Integer aChannel : aInstanceDomain){
					//Is this check worth doing? If we didn't do it, the encoding is still valid, but we include superfluous variables
					if(aInstanceDomain.contains(aChannel+1)&&aStation.getDomain().contains(aChannel)&&aInterferingStation.getDomain().contains(aChannel+1)){
						Clause_old aClause = new Clause_old();
						aClause.addLiteral(stationChannelPairToVar(aStation.getID(),aChannel),false);
						aClause.addLiteral(stationChannelPairToVar(aInterferingStation.getID(),aChannel+1),false);
						aConstraintClauseSet.add(aClause);
					}
				}
			}
		}
		
		return aConstraintClauseSet;
	}
	
	//Nifty diagonalization trick - could also map to internalIDs if we wanted
	private Integer stationChannelPairToVar(Integer aStationID, Integer aChannel){
		
		Integer aStationIDInternal = getInternalStationID(aStationID);
		Integer aChannelInternal = getInternalChannel(aChannel);
		Integer diag = aStationIDInternal+aChannelInternal-1;
		return diag*(diag-1)/2+aChannelInternal;
		
	}
	
	private Integer varToChannel(Integer var){
		/*
		return var - (fMaxChannel+1)*(var/(fMaxChannel+1));
		 */
		
		Integer diag = new Double(Math.floor(Math.sqrt(2*var))).intValue();
		if(diag*(diag+1)/2<var) diag = diag+1;
		return getExternalChannel(var - diag*(diag-1)/2);
		
	}
	
	private Integer varToStationID(Integer var){
		//return getExternalStationID(var/(fMaxChannel+1));
		
		Integer diag = new Double(Math.floor(Math.sqrt(2*var))).intValue();
		if(diag*(diag+1)/2<var) diag = diag+1;
		Integer channel = var - diag*(diag-1)/2;
		Integer aExternalStationID = getExternalStationID(diag+1-channel);
		if(aExternalStationID==null)
			System.out.println("Trying to decode variable "+var);
		return aExternalStationID;
		
	}
	
	/* NA - currently no-ops
	 * In general, getInternalStationID(getExternalStationID(x)) = x = getExternalStationID(getInternalStationID(x))
	 * and similarly for channel matchings
	 */
	private Integer getInternalStationID(Integer aStationID){
		Integer aInternalID = fExternalToInternal.get(aStationID);
		if(aInternalID==null) try{
			/*
			aInternalID = fExternalToInternal.size()+1;
			fExternalToInternal.put(aStationID,aInternalID);
			fInternalToExternal.put(aInternalID, aStationID);
			*/
			throw new IllegalArgumentException("Station "+aStationID+" not recognized in CNFEncoder.");	
		} catch(Exception e){
			e.printStackTrace();
			System.out.println(fExternalToInternal);
		}
		return aInternalID;
	}
	
	private Integer getExternalStationID(Integer aInternalStationID){
		Integer aExternalID = fInternalToExternal.get(aInternalStationID);
		if(aExternalID==null) try{
			throw new IllegalStateException("Cannot decode internal ID "+aInternalStationID+" in CNFEncoder");	
		} catch(Exception e){
			e.printStackTrace();
			System.out.println(fExternalToInternal);
		}
		return aExternalID;
	}
		
	private Integer getInternalChannel(Integer aChannel){
		return aChannel;
	}
	
	private Integer getExternalChannel(Integer aInternalChannel){
		return aInternalChannel;
	}

	
	//NA - returns a map from items of type T to internal ID numbers used to represent them.
	private <T extends Comparable<T>> Map<T,Integer> getInternalIDs(Set<T> aSet){
		List<T> aSortedSet = new ArrayList<T>(aSet);
		Collections.sort(aSortedSet);
		Map<T,Integer> aInternalID = new HashMap<T,Integer>();
		for(int i = 0; i < aSortedSet.size(); i++){
			aInternalID.put(aSortedSet.get(i),i+1);
		}
		return aInternalID;
	}
}