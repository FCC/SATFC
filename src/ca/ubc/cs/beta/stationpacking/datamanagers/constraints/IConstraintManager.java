package ca.ubc.cs.beta.stationpacking.datamanagers.constraints;

import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;

/**
 * Manages co- and adjacent- channel constraints.
 * @author afrechet
 */
public interface IConstraintManager {
	
	/**
	 * @param aStation - a (source) station of interest.
	 * @param aChannel - a channel on which we wish to know interfering stations.
	 * @return all the (target) stations that cannot be on the same given channel, <i> i.e. </i> if s is the
	 * source station and c the given channel, then the set of stations T returned is such that, for all t in T,
	 * <p>
	 * s and t cannot be both on c
	 * </p>
	 */
	public Set<Station> getCOInterferingStations(Station aStation, int aChannel);
	
	/**
	 * @param aStation - a (source) station of interest.
	 * @param aChannel - a channel on which we wish to know interfering stations.
	 * @return all the (target) stations that cannot be on a channel that is one above the given channel on which the source station is, <i> i.e. </i> if s is the
	 * source station and c the given channel, then the set of stations T returned is such that, for all t in T,
	 * <p>
	 * s cannot be on c at the same time as t is on c+1 for all c in C
	 * </p> 
	 */
	public Set<Station> getADJplusInterferingStations(Station aStation, int aChannel);
	
	/**
	 * @param aAssignment - an assignment of channels to (set of) stations.
	 * @return true if and only if the assignment satisfies all the constraints represented by the constraint manager.
	 */
	public Boolean isSatisfyingAssignment(Map<Integer,Set<Station>> aAssignment);
	

	
}
