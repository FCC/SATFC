package ca.ubc.cs.beta.stationpacking.solvers.cnfencoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.math3.util.Pair;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Clause;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Litteral;

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
	
	public SATEncoder(IStationManager aStationManager, IConstraintManager aConstraintManager)
	{
		fStationManager = aStationManager;
		fConstraintManager = aConstraintManager;
	}
	
	
	@Override
	public CNF encode(StationPackingInstance aInstance){
		
		CNF aCNF = new CNF();
		
		//Encode base clauses,
		aCNF.addAll(encodeBaseClauses(aInstance));
		
		//Encode co-channel constraints
		aCNF.addAll(encodeCoConstraints(aInstance));
		
		//Encode adjacent-channel constraints
		aCNF.addAll(encodeAdjConstraints(aInstance));
		
		return aCNF;
	}
	
	private CNF encodeBaseClauses(StationPackingInstance aInstance)
	{
		CNF aCNF = new CNF();
		
		HashSet<Integer> aInstanceChannels = aInstance.getChannels();
		HashSet<Station> aInstanceStations = aInstance.getStations();
		
		//Each station has its own base clauses.
		for(Station aStation: aInstanceStations)
		{
			@SuppressWarnings("unchecked")
			ArrayList<Integer> aStationInstanceDomain = new ArrayList<Integer>(CollectionUtils.retainAll(aInstanceChannels, aStation.getDomain()));
			
			//A station must be on at least one channel,
			Clause aStationValidAssignmentBaseClause = new Clause();
			for(Integer aChannel : aStationInstanceDomain)
			{
				aStationValidAssignmentBaseClause.add(new Litteral(Pair(aStation.getID(), aChannel), true));
			}
			aCNF.add(aStationValidAssignmentBaseClause);
			
			
			//A station can be on at most one channel,
			for(int i=0;i<aStationInstanceDomain.size();i++)
			{
				for(int j=i+1;j<aStationInstanceDomain.size();j++)
				{
					Clause aStationSingleAssignmentBaseClause = new Clause();
					
					Integer aDomainChannel1 = aStationInstanceDomain.get(i);
					aStationSingleAssignmentBaseClause.add(new Litteral(Pair(aStation.getID(),aDomainChannel1),false));
					
					Integer aDomainChannel2 = aStationInstanceDomain.get(j);
					aStationSingleAssignmentBaseClause.add(new Litteral(Pair(aStation.getID(),aDomainChannel2),false));
					
					aCNF.add(aStationSingleAssignmentBaseClause);
				}
			}
		}
		
		return aCNF;
		
		
	}
	
	private CNF encodeCoConstraints(StationPackingInstance aInstance)
	{
		CNF aCNF = new CNF();
		
		HashSet<Integer> aInstanceChannels = aInstance.getChannels();
		HashSet<Station> aInstanceStations = aInstance.getStations();
		
		for(Station aStation : aInstanceStations)
		{
			@SuppressWarnings("unchecked")
			Collection<Station> aInterferingStations = CollectionUtils.retainAll(
					fConstraintManager.getCOInterferingStations(aStation, aInstanceChannels),
					aInstanceStations);
			
			for(Station aInterferingStation : aInterferingStations)
			{
				for(Integer aChannel : aInstanceChannels)
				{
					if(aStation.getDomain().contains(aChannel) && aInterferingStation.getDomain().contains(aChannel))
					{
						Clause aCoChannelClause = new Clause();
						aCoChannelClause.add(new Litteral(Pair(aStation.getID(),aChannel),false));
						aCoChannelClause.add(new Litteral(Pair(aInterferingStation.getID(),aChannel),false));
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
		
		HashSet<Integer> aInstanceChannels = aInstance.getChannels();
		HashSet<Station> aInstanceStations = aInstance.getStations();
		
		for(Station aStation : aInstanceStations)
		{
			@SuppressWarnings("unchecked")
			Collection<Station> aInterferingStations = CollectionUtils.retainAll(
					fConstraintManager.getADJplusInterferingStations(aStation, aInstanceChannels),
					aInstanceStations);
			
			for(Station aInterferingStation : aInterferingStations)
			{
				for(Integer aChannel : aInstanceChannels)
				{
					Integer aInterferingChannel = aChannel+1;
					if(aStation.getDomain().contains(aChannel) && aInterferingStation.getDomain().contains(aInterferingChannel))
					{
						Clause aAdjChannelClause = new Clause();
						aAdjChannelClause.add(new Litteral(Pair(aStation.getID(),aChannel),false));
						aAdjChannelClause.add(new Litteral(Pair(aInterferingStation.getID(),aInterferingChannel),false));
						aCNF.add(aAdjChannelClause);
					}
				}
			}
		}
		
		return aCNF;
	}
	
	
	@Override
	public Pair<Station,Integer> decode(long aVariable){
		
		Pair<Integer,Integer> aStationChannelPair = Unpair(aVariable);
		
		Station aStation = fStationManager.getStationfromID(aStationChannelPair.getKey());
		
		Integer aChannel = aStationChannelPair.getValue();
		
		return new Pair<Station,Integer>(aStation,aChannel);
		
	}
	
	/*
	 * Szudzik's elegant pairing function (http://szudzik.com/ElegantPairing.pdf)
	 * that acts as a bijection between our station channel pairs and the SAT variables.
	 */
	private static long Pair(Integer x, Integer y)
	{
		long X = (long) x;
		long Y = (long) y;
		
		long Z;
		if(X<Y)
		{
			Z= Y*Y+X;
		}
		else
		{
			Z = X*X+X+Y;
		}
		
		return Z;
	}
	
	private static Pair<Integer,Integer> Unpair(long z)
	{
		long a = (long) (z-Math.pow(Math.floor(Math.sqrt(z)),2));
		long b =(long) Math.floor(Math.sqrt(z));
		
		if(a>Integer.MAX_VALUE || a<Integer.MIN_VALUE || b>Integer.MAX_VALUE || b<Integer.MIN_VALUE || (a-b) > Integer.MAX_VALUE || (a-b) < Integer.MAX_VALUE)
		{
			throw new IllegalArgumentException("Cannot unpair "+z+" to integer pairing components.");
		}
		
		if(a<b)
		{
			return new Pair<Integer,Integer>((int)a,(int)b);
			
		}
		else
		{
			return new Pair<Integer,Integer>((int)b,(int)(a-b));
		}
		
	}
	
	
	
}
