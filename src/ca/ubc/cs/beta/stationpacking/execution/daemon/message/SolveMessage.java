package ca.ubc.cs.beta.stationpacking.execution.daemon.message;

import java.util.Collection;
import java.util.HashSet;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;

/**
 * A message corresponding to a solve request.
 * @author afrechet
 *
 */
public class SolveMessage implements IMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final HashSet<Integer> fStationIDs;
	private final HashSet<Integer> fChannels;
	private final double fCutoff;
	private final long fSeed;
	
	/**
	 * Solve message that embodies a SOLVE command paired with the required data to form a station packing instance (stations to pack, channels to pack in and cutoff time).
	 * @param aStations - set of stations IDs to pack.
	 * @param aChannels - a set of channels to pack in.
	 * @param aCutoff - the allowed time to solve instance.
	 */
	public SolveMessage(Collection<Integer> aStations, Collection<Integer> aChannels, double aCutoff, long aSeed)
	{
		fStationIDs = new HashSet<Integer>(aStations);
		fChannels = new HashSet<Integer>(aChannels);
		fCutoff = aCutoff;
		fSeed = aSeed;
	}
	
	/**
	 * Solve message that embodies a SOLVE command paired with the required data to form a station packing instance (stations to pack, channels to pack in and cutoff time).
	 * @param aInstance - the instance to solve.
	 * @param aCutoff - the allowed time to solve instance.
	 */
	public SolveMessage(StationPackingInstance aInstance, double aCutoff, long aSeed)
	{
		fStationIDs = new HashSet<Integer>();
		for(Station aStation: aInstance.getStations())
		{
			fStationIDs.add(aStation.getID());
		}
		fChannels = aInstance.getChannels();
		fCutoff = aCutoff;
		fSeed = aSeed;
	}
	
	public HashSet<Integer> getStationIDs() {
		return fStationIDs;
	}
	public HashSet<Integer> getChannels() {
		return fChannels;
	}
	public double getCutoff() {
		return fCutoff;
	}
	public long getSeed() {
		return fSeed;
	}
	
	@Override
	public String toString()
	{
		return "Solve message - stations: "+fStationIDs.toString()+" and channels: "+fChannels.toString();
	}
	
}
