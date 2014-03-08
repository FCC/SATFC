package ca.ubc.cs.beta.stationpacking.solvers.tae.cnflookup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;

/**
 * CNF lookup that searches a particular directory for CNFs, and keeps a cached map around for solver results.
 * @author afrechet, narnosti
 *
 */
public class HybridCNFResultLookup implements ICNFResultLookup{
	
	private final String fOutputName;
	private String fCNFDirectory;
	private Map<StationPackingInstance,SolverResult> fResultLookup = new HashMap<StationPackingInstance,SolverResult>();
	
	public HybridCNFResultLookup(String aCNFDirectory, String aOutputName){
		
		//TODO Load the CNF Result lookup if already present?
		
		fOutputName = aOutputName;
		fCNFDirectory = aCNFDirectory;
	
	}
	
	@Override
	public boolean hasSolverResult(StationPackingInstance aInstance) {
		String aInstanceHash = getCNFName(aInstance);
		return fResultLookup.containsKey(aInstanceHash);
	}

	@Override
	public String getCNFNameFor(StationPackingInstance aInstance){
		return 	fCNFDirectory+File.separatorChar+getCNFName(aInstance)+".cnf";
	}

	
	//NA - returns the saved SATResult value corresponding to aInstance, 
	@Override
	public SolverResult getSolverResult(StationPackingInstance aInstance){
		if(hasSolverResult(aInstance))
		{
			String aInstanceHash = getCNFName(aInstance);
			SolverResult aResult = fResultLookup.get(aInstanceHash);
			return aResult;
		}
		else
		{
			throw new IllegalArgumentException("Required a solver result for an instance that was not previously recorded.");
		}
		
	
	}
	
	//NA - returns true if the entry already existed
	@Override
	public boolean putSolverResult(StationPackingInstance aInstance, SolverResult aResult){

		if(hasSolverResult(aInstance))
		{
			SolverResult aExistingResult;
			try {
				aExistingResult = getSolverResult(aInstance);
				if(aExistingResult.getResult() != aResult.getResult())
				{
					throw new Exception("Instance "+aInstance.toString()+" previously determined "+aExistingResult.getResult()+", now declared to be "+aResult.getResult());
				}
				else
				{
					fResultLookup.put(aInstance, aResult);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		}
		else
		{
			fResultLookup.put(aInstance, aResult);

			return false;
		} 
	}
	
	@Override
	public void writeToFile() throws IOException{
		
		DateFormat aDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date aDate = new Date();
		
		String aOutputFilename = fCNFDirectory+File.separatorChar+fOutputName;
		BufferedWriter aWriter = new BufferedWriter(new FileWriter(aOutputFilename,true));
		for(Map.Entry<StationPackingInstance, SolverResult> aEntry : fResultLookup.entrySet()){
			aWriter.write(aDateFormat.format(aDate)+","+aEntry.getKey().toString()+","+aEntry.getValue()+"\n");	
		}
		aWriter.close();
	}
			
	private String getCNFName(StationPackingInstance aInstance){
		return hashforFilename(Station.hashStationSet(aInstance.getStations())+StationPackingInstance.hashChannelSet(aInstance.getChannels()));
	}		
	
	private String hashforFilename(String aString)
	{
		MessageDigest aDigest = DigestUtils.getSha1Digest();
		try {
			byte[] aResult = aDigest.digest(aString.getBytes("UTF-8"));
		    String aResultString = new String(Hex.encodeHex(aResult));	
		    return aResultString;
		}
		catch (UnsupportedEncodingException e) {
		    throw new IllegalStateException("Could not encode filename", e);
		}
	}
	
	
}
