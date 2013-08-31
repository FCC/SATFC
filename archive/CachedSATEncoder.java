package ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.DACConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Clause;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Litteral;

/**
 * Optimized SAT encoder that creates and caches all interference clauses on construction, speeding up encoding time.
 * @author afrechet
 */
public class CachedSATEncoder implements ISATEncoder{
	
	private static Logger log = LoggerFactory.getLogger(CachedSATEncoder.class);
	
	private final HashMap<Pair<Station,Station>,HashMap<Integer,Clause>> fCoInterferenceClauses;
	private final HashMap<Pair<Station,Station>,HashMap<Pair<Integer,Integer>,HashSet<Clause>>> fAdjInterferenceClauses;
	
	private final IStationManager fStationManager;
	
	/**
	 * Create a Cached SAT Encoder. All interference clauses are created on construction for all stations in the provided station manager, using the constraints in
	 * the provided constraint manager. The channel sets used to query constraints are the LVHF, UVHF and UHF channel bands specified in DACConstraintManager.
	 * @param aStationManager - station manager used to create all interference clauses.
	 * @param aConstraintManager - constraint manager used to create all interference clauses.
	 */
	public CachedSATEncoder(IStationManager aStationManager, IConstraintManager aConstraintManager)
	{
		fStationManager = aStationManager;
		//Clauses will be created for all stations in station manager,
		Collection<Station> aStations = aStationManager.getStations();
		//and all channel bands.
		ArrayList<HashSet<Integer>> aChannelSets = new ArrayList<HashSet<Integer>>(Arrays.asList(DACConstraintManager.LVHF_CHANNELS,DACConstraintManager.UVHF_CHANNELS,DACConstraintManager.UHF_CHANNELS));
		
		fCoInterferenceClauses = new HashMap<Pair<Station,Station>,HashMap<Integer,Clause>>();
		fAdjInterferenceClauses = new HashMap<Pair<Station,Station>,HashMap<Pair<Integer,Integer>,HashSet<Clause>>>();
		for(HashSet<Integer> aChannels : aChannelSets)
		{	
			for(Station aStation : aStations)
			{
				//Create co-channel constraints.
				@SuppressWarnings("unchecked")
				Collection<Station> aCoInterferingStations = CollectionUtils.retainAll(
						aConstraintManager.getCOInterferingStations(aStation, aChannels),
						aStations);
				for(Station aInterferingStation : aCoInterferingStations)
				{
					for(Integer aChannel : aChannels)
					{
						if(aStation.getDomain().contains(aChannel) && aInterferingStation.getDomain().contains(aChannel))
						{
							Clause aCoChannelClause = new Clause();
							aCoChannelClause.add(new Litteral(SATEncoderUtils.SzudzikElegantPairing(aStation.getID(),aChannel),false));
							aCoChannelClause.add(new Litteral(SATEncoderUtils.SzudzikElegantPairing(aInterferingStation.getID(),aChannel),false));
							
							Pair<Station,Station> aKeyStationPair = getKeyStationPair(aStation, aInterferingStation);
							if(!fCoInterferenceClauses.containsKey(aKeyStationPair))
							{
								fCoInterferenceClauses.put(aKeyStationPair, new HashMap<Integer,Clause>());
							}
							fCoInterferenceClauses.get(aKeyStationPair).put(aChannel, aCoChannelClause);
						}
					}
				}
				
				//Create adj-channel constraints.
				@SuppressWarnings("unchecked")
				Collection<Station> aAdjInterferingStations = CollectionUtils.retainAll(
						aConstraintManager.getADJplusInterferingStations(aStation, aChannels),
						aStations);
				
				for(Station aInterferingStation : aAdjInterferingStations)
				{
					for(Integer aChannel : aChannels)
					{
						Integer aInterferingChannel = aChannel+1;
						if(aStation.getDomain().contains(aChannel) && aInterferingStation.getDomain().contains(aInterferingChannel) && aChannels.contains(aInterferingChannel))
						{
							Clause aAdjChannelClause = new Clause();
							aAdjChannelClause.add(new Litteral(SATEncoderUtils.SzudzikElegantPairing(aStation.getID(),aChannel),false));
							aAdjChannelClause.add(new Litteral(SATEncoderUtils.SzudzikElegantPairing(aInterferingStation.getID(),aInterferingChannel),false));
							
							Pair<Station,Station> aKeyStationPair = getKeyStationPair(aStation, aInterferingStation);
							Pair<Integer,Integer> aKeyChannelPair = getKeyChannelPair(aChannel, aInterferingChannel);
							
							if(!fAdjInterferenceClauses.containsKey(aKeyStationPair))
							{
								fAdjInterferenceClauses.put(aKeyStationPair, new HashMap<Pair<Integer,Integer>,HashSet<Clause>>());
							}
							
							if(!fAdjInterferenceClauses.get(aKeyStationPair).containsKey(aKeyChannelPair))
							{
								fAdjInterferenceClauses.get(aKeyStationPair).put(aKeyChannelPair, new HashSet<Clause>());
							}
							fAdjInterferenceClauses.get(aKeyStationPair).get(aKeyChannelPair).add(aAdjChannelClause);
							
						}
					}
				}
			}
		}
	}

	private static Pair<Station,Station> getKeyStationPair(Station aStation1, Station aStation2)
	{
		if(aStation1.getID()<=aStation2.getID())
		{
			return new Pair<Station,Station>(aStation1,aStation2);
		}
		else
		{
			return new Pair<Station,Station>(aStation2,aStation1);
		}
	}
	private static Pair<Integer,Integer> getKeyChannelPair(Integer aChannel1, Integer aChannel2)
	{
		if(aChannel1 <= aChannel2)
		{
			return new Pair<Integer,Integer>(aChannel1,aChannel2);
		}
		else
		{
			return new Pair<Integer,Integer>(aChannel2,aChannel1);
		}
	}
	
	@Override
	public CNF encode(StationPackingInstance aInstance) {
		
		log.info("Encoding problem...");
		log.info("Getting base clauses...");
		//Get base clauses.
		CNF aCNF = SATEncoder.encodeBaseClauses(aInstance);
		log.info("done.");
		
		HashSet<Integer> aInstanceChannels = aInstance.getChannels();
		ArrayList<Station> aInstanceStations = new ArrayList<Station>(aInstance.getStations());
		log.info("Getting interference clauses...");
		log.info("{} stations, {} channels.",aInstanceStations.size(),aInstanceChannels.size());
		
		//Get interference clauses.
		for(int i=0;i<aInstanceStations.size();i++)
		{	
			Station aStation1 = aInstanceStations.get(i);
			for(int j=i+1;j<aInstanceStations.size();j++)
			{
				Station aStation2 = aInstanceStations.get(j);
				Pair<Station,Station> aStationKeyPair = getKeyStationPair(aStation1, aStation2);
				
				for(Integer aChannel1 : aInstanceChannels)
				{
					
					//Check for co-channel clauses.
					if(fCoInterferenceClauses.containsKey(aStationKeyPair) && fCoInterferenceClauses.get(aStationKeyPair).containsKey(aChannel1))
					{
						aCNF.add(fCoInterferenceClauses.get(aStationKeyPair).get(aChannel1));
					}
					
					//Check for adj-channel clauses.
					if(fAdjInterferenceClauses.containsKey(aStationKeyPair))
					{
						HashMap<Pair<Integer,Integer>,HashSet<Clause>> aPossibleAdjClauses = fAdjInterferenceClauses.get(aStationKeyPair);
						
						Integer aChannel2  = aChannel1+1;
						Pair<Integer,Integer> aChannelKeyPair = getKeyChannelPair(aChannel1, aChannel2);
						if(aInstanceChannels.contains(aChannel2) && aPossibleAdjClauses.containsKey(aChannelKeyPair))
						{
							aCNF.addAll(aPossibleAdjClauses.get(aChannelKeyPair));
						}
	
						aChannel2  = aChannel1-1;
						aChannelKeyPair = getKeyChannelPair(aChannel1, aChannel2);
						if(aInstanceChannels.contains(aChannel2) && aPossibleAdjClauses.containsKey(aChannelKeyPair))
						{
							aCNF.addAll(aPossibleAdjClauses.get(aChannelKeyPair));
						}
					}
					
				}
			}
		}
		log.info("done.");
		
		return aCNF;
	}

	@Override
	public Pair<Station, Integer> decode(long aVariable) {
		Pair<Integer,Integer> aStationChannelPair = SATEncoderUtils.SzudzikElegantInversePairing(aVariable);
		
		Station aStation = fStationManager.getStationfromID(aStationChannelPair.getKey());
		
		Integer aChannel = aStationChannelPair.getValue();
		
		return new Pair<Station,Integer>(aStation,aChannel);
	}

}
