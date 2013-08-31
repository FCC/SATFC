package ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder;

import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import com.google.common.collect.Sets;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Clause;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Litteral;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.base.IBijection;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.base.IdentityBijection;

/**
 * Encodes a problem instance as a propositional satisfiability problem. 
 * A variable of the SAT encoding is a station channel pair, each constraint is trivially
 * encoded as a clause (this station cannot be on this channel when this other station is on this other channel is a two clause with the previous
 * SAT variables), and base clauses are added (each station much be on exactly one channel).
 * 
 * @author afrechet
 */
public class SATEncoder implements ISATEncoder {
	
	private final IStationManager fStationManager;
	private final IConstraintManager fConstraintManager;
	
	private final IBijection<Long,Long> fBijection;
	
	public SATEncoder(IStationManager aStationManager, IConstraintManager aConstraintManager)
	{
		this(aStationManager,aConstraintManager, new IdentityBijection<Long>());
	}
	
	public SATEncoder(IStationManager aStationManager, IConstraintManager aConstraintManager, IBijection<Long, Long> aBijection)
	{
		fStationManager = aStationManager;
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
		
		//Create the decoder
		
		ISATDecoder aDecoder = new ISATDecoder() {
			
			@Override
			public Pair<Station, Integer> decode(long aVariable) {
				Pair<Integer,Integer> aStationChannelPair = SATEncoderUtils.SzudzikElegantInversePairing(fBijection.inversemap(aVariable));
				
				Station aStation = fStationManager.getStationfromID(aStationChannelPair.getKey());
				
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
		
		Set<Integer> aInstanceChannels = aInstance.getChannels();
		Set<Station> aInstanceStations = aInstance.getStations();
		
		//Each station has its own base clauses.
		for(Station aStation: aInstanceStations)
		{
			ArrayList<Integer> aStationInstanceDomain = new ArrayList<Integer>(Sets.intersection(aInstanceChannels, aStation.getDomain()));
			
			//A station must be on at least one channel,
			Clause aStationValidAssignmentBaseClause = new Clause();
			for(Integer aChannel : aStationInstanceDomain)
			{
				
				aStationValidAssignmentBaseClause.add(new Litteral(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aStation.getID(), aChannel)), true));
			}
			aCNF.add(aStationValidAssignmentBaseClause);
			
			//A station can be on at most one channel,
			for(int i=0;i<aStationInstanceDomain.size();i++)
			{
				for(int j=i+1;j<aStationInstanceDomain.size();j++)
				{
					Clause aStationSingleAssignmentBaseClause = new Clause();
					
					Integer aDomainChannel1 = aStationInstanceDomain.get(i);
					aStationSingleAssignmentBaseClause.add(new Litteral(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aStation.getID(),aDomainChannel1)),false));
					
					Integer aDomainChannel2 = aStationInstanceDomain.get(j);
					aStationSingleAssignmentBaseClause.add(new Litteral(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aStation.getID(),aDomainChannel2)),false));
					
					aCNF.add(aStationSingleAssignmentBaseClause);
				}
			}
		}
		
		return aCNF;
	}
	
	private CNF encodeCoConstraints(StationPackingInstance aInstance)
	{
		CNF aCNF = new CNF();
		
		Set<Integer> aInstanceChannels = aInstance.getChannels();
		Set<Station> aInstanceStations = aInstance.getStations();
		
		for(Station aStation : aInstanceStations)
		{		
			for(Station aInterferingStation : fConstraintManager.getCOInterferingStations(aStation, aInstanceChannels))
			{
				if(aInstanceStations.contains(aInterferingStation))
				{
					for(Integer aChannel : aInstanceChannels)
					{
						if(aStation.getID()<aInterferingStation.getID() && aStation.getDomain().contains(aChannel) && aInterferingStation.getDomain().contains(aChannel))
						{
							Clause aCoChannelClause = new Clause();
							aCoChannelClause.add(new Litteral(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aStation.getID(),aChannel)),false));
							aCoChannelClause.add(new Litteral(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aInterferingStation.getID(),aChannel)),false));
							aCNF.add(aCoChannelClause);
						}
					}
				}
			}
		}
		
		return aCNF;
	}
	
	private CNF encodeAdjConstraints(StationPackingInstance aInstance)
	{
		CNF aCNF = new CNF();
		
		Set<Integer> aInstanceChannels = aInstance.getChannels();
		Set<Station> aInstanceStations = aInstance.getStations();
		
		for(Station aStation : aInstanceStations)
		{			
			for(Station aInterferingStation : fConstraintManager.getADJplusInterferingStations(aStation, aInstanceChannels))
			{
				if(aInstanceStations.contains(aInterferingStation))
				{
					for(Integer aChannel : aInstanceChannels)
					{
						Integer aInterferingChannel = aChannel+1;
						if( aStation.getDomain().contains(aChannel) && aInterferingStation.getDomain().contains(aInterferingChannel) && aInstanceChannels.contains(aInterferingChannel))
						{
							Clause aAdjChannelClause = new Clause();
							aAdjChannelClause.add(new Litteral(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aStation.getID(),aChannel)),false));
							aAdjChannelClause.add(new Litteral(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aInterferingStation.getID(),aInterferingChannel)),false));
							aCNF.add(aAdjChannelClause);
						}
					}
				}
			}
		}
		
		return aCNF;
	}
	
	

	
	
	
}
