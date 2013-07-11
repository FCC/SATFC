package ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnflookup;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.experiment.instance.IInstance;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SATResult;

/**
 * CNF lookup that keeps a cached map of CNF identifier to CNF name.
 * @author afrechet
 *
 */
public class CachedCNFLookup implements ICNFLookup{

	private HashMap<Set<Station>,String> fStationsHashtoCNFName;
	
	public CachedCNFLookup()
	{
		fStationsHashtoCNFName = new HashMap<Set<Station>,String>();
	}
	
	private String hashforFilename(String aString)
	{
		MessageDigest aDigest = DigestUtils.getSha1Digest();
	
		try {
		        byte[] aResult = aDigest.digest(aString.getBytes("UTF-8"));
		        String aResultString = new String(Hex.encodeHex(aResult));
		
		        return aResultString;
		}
		catch (UnsupportedEncodingException e) 
		{
		        throw new IllegalStateException("Could not encode filename", e);
		}
	}
	
	public boolean hasCNFfor(Set<Station> aStations, Integer ... aRange) {
		return fStationsHashtoCNFName.containsKey(aStations);
	}

	public String getCNFfor(Set<Station> aStations, Integer ... aRange) throws Exception{
		
		if(!hasCNFfor(aStations))
		{
			throw new IllegalArgumentException("Tried to lookup a CNF for a set of stations that is unavailable.");
		}
		else
		{
			return fStationsHashtoCNFName.get(aStations);
		}
		
	}
	
	/**
	 * Associates input set of stations to input CNF file name.
	 * @param aStations - a set of stations.
	 * @param aCNFFileName - a CNF file name for given station set.
	 * @throws Exception
	 */
	public void addCNFfor(Set<Station> aStations, String aCNFFileName) throws Exception {
		if(fStationsHashtoCNFName.containsKey(aStations)){
			throw new UnsupportedOperationException("Tried to update a CNF for a set of stations that is already present.");
		}
		else
		{
			fStationsHashtoCNFName.put(aStations, aCNFFileName);
		}
	}

	private String getCNFNamefor(Set<Station> aStations, Integer ... aRange) {
		return hashforFilename(Station.hashStationSet(aStations))+".cnf";
	}

	public String addCNFfor(Set<Station> aStations, Integer ...aRange) throws Exception {
		if(fStationsHashtoCNFName.containsKey(aStations)){
			throw new UnsupportedOperationException("Tried to update a CNF for a set of stations that is already present.");
		}
		else
		{
			String aCNFFileName = getCNFNamefor(aStations);
			fStationsHashtoCNFName.put(aStations, aCNFFileName);
			return aCNFFileName;
		}
	}

	@Override
	public boolean hasSATResult(IInstance aInstance) {
		try{
			throw new UnsupportedOperationException("Method hasSATResult is not implemented in class CachedCNFLookup.");
		} catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public SATResult getSATResult(IInstance aInstance) {
		try{
			throw new UnsupportedOperationException("Method getSATResult is not implemented in class CachedCNFLookup.");
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean putSATResult(IInstance aInstance, SATResult aResult) {
		try{
			throw new UnsupportedOperationException("Method putSATResult is not implemented in class CachedCNFLookup.");
		} catch(Exception e){
			e.printStackTrace();
		}		
		return false;
	}

	@Override
	public String getNameFor(IInstance aInstance) {
		try{
			throw new UnsupportedOperationException("Method getNameFor is not implemented in class CachedCNFLookup.");
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void writeToFile() throws IOException {
		try{
			throw new UnsupportedOperationException("Method writeToFile is not implemented in class CachedCNFLookup.");
		} catch(Exception e){
			e.printStackTrace();
		}		
	}
	
	

}
