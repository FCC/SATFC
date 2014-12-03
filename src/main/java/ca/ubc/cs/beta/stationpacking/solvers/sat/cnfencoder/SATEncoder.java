package ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Clause;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.base.IBijection;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.base.IdentityBijection;

import org.apache.commons.math3.util.Pair;

/**
 * Encodes a problem instance as a propositional satisfiability problem. 
 * A variable of the SAT encoding is a station channel pair, each constraint is trivially
 * encoded as a clause (this station cannot be on this channel when this other station is on this other channel is a two clause with the previous
 * SAT variables), and base clauses are added (each station much be on exactly one channel).
 * 
 * @author afrechet
 */
public class SATEncoder implements ISATEncoder {
	
	private final IConstraintManager fConstraintManager;
	private final IBijection<Long,Long> fBijection;
	
	public SATEncoder(IConstraintManager aConstraintManager)
	{
		this(aConstraintManager, new IdentityBijection<Long>());
	}
	
	public SATEncoder(IConstraintManager aConstraintManager, IBijection<Long, Long> aBijection)
	{
		fConstraintManager = aConstraintManager;
		
		fBijection = aBijection;
	}
	
	
	@Override
	public Pair<CNF,ISATDecoder> encode(StationPackingInstance aInstance){
		
		CNF aCNF = new CNF();
		
		//Encode base clauses,
		aCNF.addAll(encodeBaseClauses(aInstance));
		
		//Encode co-channel constraints
		aCNF.addAll(encodeCoConstraints(aInstance));
		
		//Encode adjacent-channel constraints
		aCNF.addAll(encodeAdjConstraints(aInstance));
		
		//Save station map.
		final Map<Integer,Station> stationMap = new HashMap<Integer,Station>();
		for(Station station : aInstance.getStations())
		{
			stationMap.put(station.getID(), station);
		}
		
		//Create the decoder
		ISATDecoder aDecoder = new ISATDecoder() {
			@Override
			public Pair<Station, Integer> decode(long aVariable) {
				
				//Decode the long variable to station channel pair.
				Pair<Integer,Integer> aStationChannelPair = SATEncoderUtils.SzudzikElegantInversePairing(fBijection.inversemap(aVariable));
				
				//Get station.
				Integer stationID = aStationChannelPair.getKey();
				Station aStation = stationMap.get(stationID);
				
				//Get channel
				Integer aChannel = aStationChannelPair.getValue();
				
				return new Pair<Station,Integer>(aStation,aChannel);
			}
		};
		
		return new Pair<CNF,ISATDecoder>(aCNF,aDecoder);
	}
	
	/**
	 * Get the base SAT clauses of a station packing instances. The base clauses encode the following two constraints:
	 * <ol>
	 * <li> Every station must be on at least one channel in the intersection of its domain and the problem instance's channels. </li>
	 * <li> Every station must be on at most one channel in the intersection of its domain and the problem instance's channels. </li>
	 * <ol>
	 * @param aInstance - a station packing problem instance.
	 * @return A CNF of base clauses.
	 */
	public CNF encodeBaseClauses(StationPackingInstance aInstance)
	{
		CNF aCNF = new CNF();
		
		Set<Station> aInstanceStations = aInstance.getStations();
		Map<Station,Set<Integer>> aInstanceDomains = aInstance.getDomains();
		
		//Each station has its own base clauses.
		for(Station aStation: aInstanceStations)
		{
			ArrayList<Integer> aStationInstanceDomain = new ArrayList<Integer>(aInstanceDomains.get(aStation));
			
			//A station must be on at least one channel,
			Clause aStationValidAssignmentBaseClause = new Clause();
			for(Integer aChannel : aStationInstanceDomain)
			{
				
				aStationValidAssignmentBaseClause.add(new Literal(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aStation.getID(), aChannel)), true));
			}
			aCNF.add(aStationValidAssignmentBaseClause);
			
			//A station can be on at most one channel,
			for(int i=0;i<aStationInstanceDomain.size();i++)
			{
				for(int j=i+1;j<aStationInstanceDomain.size();j++)
				{
					Clause aStationSingleAssignmentBaseClause = new Clause();
					
					Integer aDomainChannel1 = aStationInstanceDomain.get(i);
					aStationSingleAssignmentBaseClause.add(new Literal(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aStation.getID(),aDomainChannel1)),false));
					
					Integer aDomainChannel2 = aStationInstanceDomain.get(j);
					aStationSingleAssignmentBaseClause.add(new Literal(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aStation.getID(),aDomainChannel2)),false));
					
					aCNF.add(aStationSingleAssignmentBaseClause);
				}
			}
		}
		
		return aCNF;
	}
	
	private CNF encodeCoConstraints(StationPackingInstance aInstance)
	{
		CNF aCNF = new CNF();
		
		Set<Station> aInstanceStations = aInstance.getStations();
		Map<Station,Set<Integer>> aInstanceDomains = aInstance.getDomains();
		
		//For every station,
		for(Station aStation : aInstanceStations)
		{		
			//For every channel,
			for(Integer aChannel : aInstanceDomains.get(aStation))
			{
				//Get stations that can interfere on the same channel,
				for(Station aInterferingStation : fConstraintManager.getCOInterferingStations(aStation, aChannel))
				{
					//If interfering station is in this problem, and the channel is in interfering station's domain
					if(aInstanceStations.contains(aInterferingStation) && aInstanceDomains.get(aInterferingStation).contains(aChannel))
					{
						Clause aCoChannelClause = new Clause();
						aCoChannelClause.add(new Literal(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aStation.getID(),aChannel)),false));
						aCoChannelClause.add(new Literal(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aInterferingStation.getID(),aChannel)),false));
						aCNF.add(aCoChannelClause);
					}
				}
			}
		}
		
		return aCNF;
	}
	
	private CNF encodeAdjConstraints(StationPackingInstance aInstance)
	{
		CNF aCNF = new CNF();
		
		Set<Station> aInstanceStations = aInstance.getStations();
		Map<Station,Set<Integer>> aInstanceDomains = aInstance.getDomains();
		
		//For every station,
		for(Station aStation : aInstanceStations)
		{		
			//For every channel,
			for(Integer aChannel : aInstanceDomains.get(aStation))
			{
				//Get interfering channel that is +1 given channel,
				Integer aInterferingChannel = aChannel+1;
				
				//For every interfering station
				for(Station aInterferingStation : fConstraintManager.getADJplusInterferingStations(aStation, aChannel))
				{
					//Make sure instance contains interfering station, and interfering channel is in interfering station's domain.
					if(aInstanceStations.contains(aInterferingStation) && aInstanceDomains.get(aInterferingStation).contains(aInterferingChannel))
					{
						Clause aAdjChannelClause = new Clause();
						aAdjChannelClause.add(new Literal(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aStation.getID(),aChannel)),false));
						aAdjChannelClause.add(new Literal(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aInterferingStation.getID(),aInterferingChannel)),false));
						aCNF.add(aAdjChannelClause);
					}
				}
			}
		}
		
		return aCNF;
	}
	
	

	
	
	
}
