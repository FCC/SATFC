package ca.ubc.cs.beta.stationpacking.execution.daemon.message;

import java.util.Collection;
import java.util.HashSet;

public class SolveMessage implements IMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final HashSet<Integer> fStations;
	private final HashSet<Integer> fChannels;
	private final double fCutoff;
	
	public SolveMessage(Collection<Integer> aStations, Collection<Integer> aChannels, double aCutoff)
	{
		fStations = new HashSet<Integer>(aStations);
		fChannels = new HashSet<Integer>(aChannels);
		fCutoff = aCutoff;
	}
	
	public HashSet<Integer> getStations() {
		return fStations;
	}
	public HashSet<Integer> getChannels() {
		return fChannels;
	}
	public double getCutoff()
	{
		return fCutoff;
	}
	
	@Override
	public String toString()
	{
		return "Instance consisting of stations: "+fStations.toString()+" and channels: "+fChannels.toString();
	}
	
}
