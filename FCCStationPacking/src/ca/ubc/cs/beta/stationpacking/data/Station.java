package ca.ubc.cs.beta.stationpacking.data;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Container class for the station object.
 * Uniquely identified by its integer ID.
 * @author afrechet
 *
 */
public class Station implements Comparable<Station>{
	
	private final Integer fID;
	private final Integer fPop;
	private final Set<Integer> fDomain;
	
	/*
	public Station(Integer aID)
	{
		fID = aID;
		fPop = -1;
		fDomain = new HashSet<Integer>();
		try{ 
			throw new Exception("No Domain given for station "+fID);
		} catch(Exception e){
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public Station(Integer aID,Integer aPop){
		fID = aID;
		fPop = aPop;
		fDomain = new HashSet<Integer>();
		try{ 
			throw new Exception("No Domain given for station "+fID);
		} catch(Exception e){
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	*/
	
	public Station(Integer aID, Set<Integer> aDomain, Integer aPop){
		fID = aID;
		fPop = aPop;
		fDomain = aDomain;
	}
	/*
	public Station(Integer aID, Set<Integer> aDomain){
		fID = aID;
		fDomain = aDomain;
	}
	*/
	
	public int getID(){
		return fID;
	}
	
	public int getPop(){
		return fPop;
	}
	
	/*
	public void setPop(Integer aPop){
		if(fPop==null) fPop = aPop;
		else{
			try{ throw new Exception("Cannot modify population of Station "+fID+", which has population "+fPop); }
			catch(Exception e){ e.printStackTrace(); }
		}  
	}
	*/
	
	@Override
	public String toString(){
		return "Station "+fID;
	}
	
	public Set<Integer> getDomain(){
		return fDomain;
	}
	
	public boolean removeFromDomain(Integer aChannel){
		return fDomain.remove(aChannel);
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Station))
		{
			return false;
		}
		else
		{
			Station aStation = (Station) o;
			return fID.equals(aStation.fID);
		}
	}
	
	@Override
	public int hashCode(){
		return fID.hashCode();
	}
	
	public static String hashStationSet(Set<Station> aStations)
	{
		LinkedList<Station> aStationsList = new LinkedList<Station>(aStations);
		Collections.sort(aStationsList, new StationComparator());
		
		String aResult = "";
		Iterator<Station> aStationIterator = aStationsList.iterator();
		while(aStationIterator.hasNext()){
			Station aStation = aStationIterator.next();
			aResult += aStation.getID();
			if(aStationIterator.hasNext())
			{
				aResult+="-";
			}
		}
		return aResult;	
	}

	@Override
	public int compareTo(Station o) {
		return fID - o.getID();
	}

}
