package ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnflookup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import au.com.bytecode.opencsv.CSVReader;

import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.experiment.instance.IInstance;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SATResult;

/**
 * CNF lookup that searches a particular directory for CNFs.
 * @author afrechet, narnosti
 *
 */
public class NDirCNFLookup implements ICNFLookup{
	
	private final String fOutputName = "CNFOutput";
	private String fCNFDirectory;
	private Map<String,SATResult> fResultLookup = new HashMap<String,SATResult>();
	
	public NDirCNFLookup(String aCNFDirectory){
		fCNFDirectory = aCNFDirectory;
		try{
			File aOutputFile = new File(fCNFDirectory+File.separatorChar+fOutputName);
			String[] aLine;
			if(aOutputFile.exists()){
				CSVReader aReader = new CSVReader(new FileReader(aOutputFile),',');
				while((aLine = aReader.readNext())!=null){	//NA - perform some sanity checks
					if(aLine.length != 2) {
						aReader.close();
						throw new Exception("Output file exists but is not in the correct format. Continuing without reading past results.");
					}
					else fResultLookup.put(aLine[0],stringToSATResult(aLine[1]));
				}
				aReader.close();
			}		
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean hasSATResult(IInstance aInstance) {
		return (getSATResult(aInstance)!=null);
	}

	@Override
	public String getNameFor(IInstance aInstance){
		return 	fCNFDirectory+File.separatorChar+getCNFName(aInstance)+".cnf";
	}

	
	//NA - returns the saved SATResult value corresponding to aInstance, returns null if it has no record
	@Override
	public SATResult getSATResult(IInstance aInstance){
		String aInstanceHash = getCNFName(aInstance);
		return fResultLookup.get(aInstanceHash);
	}
	
	//NA - returns true if the entry already existed
	@Override
	public boolean putSATResult(IInstance aInstance, SATResult aResult){
		SATResult aExistingResult = getSATResult(aInstance);
		if(aExistingResult!=null){
			try{
				switch(aExistingResult){
				case SAT:
					if(aResult==SATResult.UNSAT) 
						throw new Exception("Instance previously determined to be in SAT, now declared to be in UNSAT.");
					break;
				case UNSAT:
					if(aResult==SATResult.SAT) 
						throw new Exception("Instance previously determined to be in SAT, now declared to be in UNSAT.");
					break;
				default: 
					fResultLookup.put(getCNFName(aInstance), aResult);
				}
			} catch(Exception e){
				e.printStackTrace();
			}
			return true;
		} else {
			fResultLookup.put(getCNFName(aInstance), aResult);
			return false;
		}
	}
	
	@Override
	public void writeToFile() throws IOException{
		String aOutputFilename = fCNFDirectory+File.separatorChar+fOutputName;
		BufferedWriter aWriter = new BufferedWriter(new FileWriter(aOutputFilename));
		for(Map.Entry<String, SATResult> aEntry : fResultLookup.entrySet()){
			aWriter.write(aEntry.getKey()+","+aEntry.getValue()+"\n");	
		}
		aWriter.close();
	}
			
	private String getCNFName(IInstance aInstance){
		return 	hashforFilename(Station.hashStationSet(aInstance.getStations()))+aInstance.getChannelRange().hashCode();
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
	
	private SATResult stringToSATResult(String aStringResult) throws Exception{
		SATResult aResult;
		switch(aStringResult){
		case "SAT":
			aResult = SATResult.SAT;
			break;
		case "UNSAT":
			aResult = SATResult.UNSAT;
			break;
		case "TIMEOUT":
			aResult = SATResult.TIMEOUT;
			break;
		case "CRASHED":
			aResult = SATResult.CRASHED;
			break;
		default : 
			throw new Exception("Input to stringToSATResult could not be interpreted as a SATResult.");
		}
		return aResult;
	}
	
}
