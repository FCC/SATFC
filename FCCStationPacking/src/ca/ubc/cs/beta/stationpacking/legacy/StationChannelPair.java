package ca.ubc.cs.beta.stationpacking.legacy;

import ca.ubc.cs.beta.stationpacking.data.Station;

public class StationChannelPair {
	
	private Station fStation;
	private Integer fChannel;
	
	public StationChannelPair(Station aStation, Integer aChannel)
	{
		fStation = aStation;
		fChannel = aChannel;
	}
	
	public Station getStation()
	{
		return fStation;
	}
	
	public Integer getChannel()
	{
		return fChannel;
	}
	
	@Override
	public String toString()
	{
		return "("+fStation+","+fChannel+")";
	}
	
	@Override
	public int hashCode() {
    	int aStationHash = fStation != null ? fStation.hashCode() : 0;
    	int aChannelHash = fChannel != null ? fChannel.hashCode() : 0;

    	return (aStationHash + aChannelHash) * aChannelHash + aStationHash;
    }
	
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof StationChannelPair))
		{
			return false;
		}
		else
		{
			StationChannelPair aStationChannelPair = (StationChannelPair) o;
			return fStation.equals(aStationChannelPair.getStation()) && fChannel.equals(aStationChannelPair.getChannel());
		}
	}
}
