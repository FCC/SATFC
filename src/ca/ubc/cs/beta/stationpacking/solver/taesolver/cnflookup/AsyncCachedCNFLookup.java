package ca.ubc.cs.beta.stationpacking.solver.taesolver.cnflookup;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.HashMap;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.datastructures.StationPackingInstance;

	/**
	 * A CNF (only) lookup for the asynchronous solver. 
	 * <i> We do not use a standard CNF/Result lookup because we inherently will use asynchronous TAE with workers which contain they're own lookup process. </i>
	 * @author afrechet
	 *
	 */
	public class AsyncCachedCNFLookup implements ICNFResultLookup
	{
		
		private HashMap<StationPackingInstance,String> fCNFMap;
		private String fCNFDirectory;
		/**
		 * Construct an asynchronous cached CNF llookup.
		 */
		public AsyncCachedCNFLookup(String aCNFDirectory)
		{
			fCNFMap = new HashMap<StationPackingInstance,String>();
			fCNFDirectory = aCNFDirectory;
		}
		
		//Private function to create hashed filenames for CNF.
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
		
		private String getNameCNF(StationPackingInstance aInstance)
		{
			return hashforFilename(Station.hashStationSet(aInstance.getStations())+StationPackingInstance.hashChannelSet(aInstance.getChannels()));
		}
		
		/**
		 * Return the CNF name for an instance.
		 * @param aInstance - problem instance to get CNF name for.
		 * @return the string CNF name for the problem instance.
		 */
		public String getCNFNameFor(StationPackingInstance aInstance)
		{
			return fCNFDirectory+File.separatorChar+getNameCNF(aInstance)+".cnf";
		}
		
		/**
		 * Return true if the instance was saved by the lookup.
		 * @param aInstance - an instance to look for.
		 * @return true if the instance is present in the lookup, false otherwise.
		 */
		public boolean hasCNFFor(StationPackingInstance aInstance)
		{
			return fCNFMap.containsKey(aInstance);
		}
		
		/**
		 * Put an instance in the lookup.
		 * @param aInstance - the instance to put in the lookup.
		 * @return ture if the instance was already there, false otherwise.
		 */
		public boolean putCNFfor(StationPackingInstance aInstance)
		{
			return (fCNFMap.put(aInstance, getNameCNF(aInstance))!=null);
		}

		@Override
		public boolean hasSolverResult(StationPackingInstance aInstance) {
			throw new UnsupportedOperationException("Async cached CNF lookup is not a result lookup.");
		}

		@Override
		public SolverResult getSolverResult(StationPackingInstance aInstance)
				throws Exception {
			throw new UnsupportedOperationException("Async cached CNF lookup is not a result lookup.");
		}

		@Override
		public boolean putSolverResult(StationPackingInstance aInstance, SolverResult aResult) {
			throw new UnsupportedOperationException("Async cached CNF lookup is not a result lookup.");
		}

		@Override
		public void writeToFile() throws IOException {
			throw new UnsupportedOperationException("Lookup is asynchronous and cached, cannot be written to file.");
		}	
	}