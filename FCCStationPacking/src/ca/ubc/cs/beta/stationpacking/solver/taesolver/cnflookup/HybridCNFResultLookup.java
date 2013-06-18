package ca.ubc.cs.beta.stationpacking.solver.taesolver.cnflookup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;



import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;

/**
 * CNF lookup that searches a particular directory for CNFs, and keeps a cached map around for solver results.
 * @author afrechet, narnosti
 *
 */
public class HybridCNFResultLookup implements ICNFResultLookup{
	
	private final String fOutputName;
	private String fCNFDirectory;
	private Map<String,SolverResult> fResultLookup = new HashMap<String,SolverResult>();
	
	public HybridCNFResultLookup(String aCNFDirectory, String aOutputName){
		
		fOutputName = aOutputName;
		fCNFDirectory = aCNFDirectory;
		
		/*
		try{
			File aOutputFile = new File(fCNFDirectory+File.separatorChar+fOutputName);
			String[] aLine;
			if(aOutputFile.exists()){
				CSVReader aReader = new CSVReader(new FileReader(aOutputFile),',');
				while((aLine = aReader.readNext())!=null){	//NA - perform some sanity checks
					if(aLine.length != 3) {
						aReader.close();
						throw new Exception("Output file exists but is not in the correct format. Continuing without reading past results.");
					}
					else fResultLookup.put(aLine[0], new SolverResult(SATResult.valueOf(aLine[1]),Double.parseDouble(aLine[2])));
				}
				aReader.close();
			}		
		} catch(Exception e){
			e.printStackTrace();
		}
		*/
	}
	
	@Override
	public boolean hasSolverResult(Instance aInstance) {
		String aInstanceHash = getCNFName(aInstance);
		return fResultLookup.containsKey(aInstanceHash);
	}

	@Override
	public String getCNFNameFor(Instance aInstance){
		return 	fCNFDirectory+File.separatorChar+getCNFName(aInstance)+".cnf";
	}

	
	//NA - returns the saved SATResult value corresponding to aInstance, 
	@Override
	public SolverResult getSolverResult(Instance aInstance) throws Exception{
		if(hasSolverResult(aInstance))
		{
			String aInstanceHash = getCNFName(aInstance);
			SolverResult aResult = fResultLookup.get(aInstanceHash);
			return aResult;
		}
		else
		{
			throw new Exception("Required a solver result for an instance that was not previously recorded.");
		}
		
	
	}
	
	//NA - returns true if the entry already existed
	@Override
	public boolean putSolverResult(Instance aInstance, SolverResult aResult){
		//System.out.println("\n\n\n testing \n\n\n");

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
					fResultLookup.put(getCNFName(aInstance), aResult);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		}
		else
		{
			fResultLookup.put(getCNFName(aInstance), aResult);
			try{
				String aOutputFilename = fCNFDirectory+File.separatorChar+fOutputName;
				FileUtils.write(new File(aOutputFilename), getCNFName(aInstance)+".cnf,"+aResult+"\n",true);
			} catch(Exception e){
				e.printStackTrace();
			}

			return false;
		} 
	}
	
	@Override
	public void writeToFile() throws IOException{
		String aOutputFilename = fCNFDirectory+File.separatorChar+fOutputName;
		BufferedWriter aWriter = new BufferedWriter(new FileWriter(aOutputFilename));
		for(Map.Entry<String, SolverResult> aEntry : fResultLookup.entrySet()){
			aWriter.write(aEntry.getKey()+","+aEntry.getValue()+"\n");	
		}
		aWriter.close();
	}
			
	private String getCNFName(Instance aInstance){
		return hashforFilename(Station.hashStationSet(aInstance.getStations())+Instance.hashChannelSet(aInstance.getChannels()));
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
