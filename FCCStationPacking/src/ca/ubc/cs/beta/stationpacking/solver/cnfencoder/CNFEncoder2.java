package ca.ubc.cs.beta.stationpacking.solver.cnfencoder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Clause;
import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;

public class CNFEncoder2 implements ICNFEncoder2 {

	
	static final Set<Clause> fUNSAT_CLAUSES = new HashSet<Clause>();
	static final Set<Integer> aEmpty = new HashSet<Integer>();
	
	public CNFEncoder2(){
		Set<Integer> aSingleVar = new HashSet<Integer>();
		aSingleVar.add(1);
		fUNSAT_CLAUSES.add(new Clause(aSingleVar,aEmpty));
		fUNSAT_CLAUSES.add(new Clause(aEmpty,aSingleVar));
	}
	
	/* NA - the new encode method
	 * Currently, each call to encode is isolated (I store no state)
	 * In order to create consistent encodings across calls, I need consistent internal IDs.
	 * One solution: don't use internal IDs
	 * Another solution: store state in constructor
	 */
	public Set<Clause> encode(Instance aInstance, IConstraintManager aConstraintManager){
		Set<Clause> aClauses = new HashSet<Clause>();
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
	public Map<Integer,Set<Station>> decode(Instance aInstance, Clause aAssignment) throws Exception{
		Map<Integer,Set<Station>> aChannelAssignments = new HashMap<Integer,Set<Station>>();
		
		Set<Integer> aInstanceDomain = aInstance.getChannels();
		for(Integer aChannel : aInstanceDomain){
			aChannelAssignments.put(aChannel, new HashSet<Station>());
		}
		
		Set<Integer> aAssignmentVariables = aAssignment.getVars();
		if(aAssignmentVariables.size()==aInstance.getStations().size()){
			for(Integer aVar : aAssignmentVariables){
				Integer aChannel = varToChannel(aVar);
				Integer aStationID = varToStationID(aVar);
				if(aInstanceDomain.contains(aChannel)){
					aChannelAssignments.get(aChannel).add(aInstance.getStation(aStationID));
				} else {
					throw new Exception("Station "+aStationID+" assigned to channel "+aChannel+", which is not in the packing instance.");
				}
			}
		} else {
			throw new Exception("This assignment makes "+aAssignmentVariables.size()+" assignments, but the instance has "+aInstance.getStations().size()+" stations.");
		}
		return aChannelAssignments;
	}
		
	private Set<Clause> getBaseClauses(Instance aInstance) throws Exception{
		Set<Clause> aBaseClauseSet = new HashSet<Clause>();
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
				aBaseClauseSet.add(new Clause(aStationChannelPairVars,aEmpty));
				
				//Encode that aStation must be assigned to at most one channel in its domain
				for(Integer aVar1 : aStationChannelPairVars){
					for(Integer aVar2 : aStationChannelPairVars){
						if(aVar1 < aVar2){
							Clause aClause = new Clause();
							aClause.addLiteral(aVar1,false);
							aClause.addLiteral(aVar2,false);
							aBaseClauseSet.add(aClause);
						}
					}
				}
			} else {
				throw new Exception("Station "+aStation+" has empty domian.");
			}
		}
		return aBaseClauseSet;
	}
	
	private Set<Clause> getConstraintClauses(Instance aInstance, IConstraintManager aConstraintManager){
		Set<Clause> aConstraintClauseSet = new HashSet<Clause>();
		Set<Integer> aInstanceDomain = aInstance.getChannels();
		for(Station aStation : aInstance.getStations()){
			
			//Encode co-channel constraints involving aStation
			Set<Station> aCOInterferingStations = aConstraintManager.getCOInterferingStations(aStation);
			for(Station aInterferingStation : aCOInterferingStations){
				for(Integer aChannel : aInstanceDomain){
					Clause aClause = new Clause();
					aClause.addLiteral(stationChannelPairToVar(aStation.getID(),aChannel),false);
					aClause.addLiteral(stationChannelPairToVar(aInterferingStation.getID(),aChannel),false);
					aConstraintClauseSet.add(aClause);
				}
			}
			
			//Encode adjacent-channel constraints involving aStation
			Set<Station> aADJInterferingStations = aConstraintManager.getADJplusInterferingStations(aStation);
			for(Station aInterferingStation : aADJInterferingStations){
				for(Integer aChannel : aInstanceDomain){
					if(aInstanceDomain.contains(aChannel+1)){
						Clause aClause = new Clause();
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
		Integer diag = new Double(Math.floor(Math.sqrt(2*var))).intValue();
		return getExternalChannel(var - diag*(diag-1)/2);
	}
	
	private Integer varToStationID(Integer var){
		Integer diag = new Double(Math.floor(Math.sqrt(2*var))).intValue();
		Integer channel = var - diag*(diag-1)/2;
		return getExternalStationID(diag+1-channel);
	}
	
	/* NA - currently no-ops
	 * In general, getInternalStationID(getExternalStationID(x)) = x = getExternalStationID(getInternalStationID(x))
	 * and similarly for channel matchings
	 */
	private Integer getInternalStationID(Integer aStationID){
		return aStationID;
	}
	
	private Integer getExternalStationID(Integer aInternalStationID){
		return aInternalStationID;
	}
		
	private Integer getInternalChannel(Integer aChannel){
		return aChannel;
	}
	
	private Integer getExternalChannel(Integer aInternalChannel){
		return aInternalChannel;
	}

}
