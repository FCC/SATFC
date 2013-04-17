package ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnflookup;


import java.io.IOException;

import ca.ubc.cs.beta.stationpacking.experiment.instance.IInstance;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SATResult;


/**
 * Records and looks up previously existing CNF instance names based on their corresponding set of stations.
 * Improves space management (fewer CNF files) and
 * speed (when executed on an identical previously solved CNF, certain solvers return previous run time).
 * @author afrechet, narnosti
 *
 */
public interface ICNFLookup {
	
	//NA - why are these all public? Which ones are actually called?
	
	/**
	 * @param aStations - a set of stations.
	 * @return True if it contains a CNF for given station set.
	 */
	public boolean hasSATResult(IInstance aInstance);
	
	public SATResult getSATResult(IInstance aInstance);

	public boolean putSATResult(IInstance aInstance, SATResult aResult);
	/**
	 * @param aStations - a set of stations.
	 * @return CNF instance file name corresponding to input station set, if any.
	 * @throws Exception - if input station set has no corresponding CNF name.
	 */
	public String getNameFor(IInstance aInstance);
	
	public void writeToFile() throws IOException;

	//public boolean writeResult(IInstance aInstance, SATResult aResult);
	
	/**
	 * @param aStations - a set of stations.
	 * @return The name it would attribute to a CNF corresponding to input station set.
	 */
	//NA - commented out because it's only called internally
	//public String getCNFNamefor(Set<Station> aStations,Integer ... aRange);
	/**
	 * Associates input set of stations to automatically generated CNF file name (see getCNFNamefor() method).
	 * @param aStations - a set of stations.
	 * @return The CNF name attributed to input station set.
	 * @throws Exception
	 */
	//public String addCNFfor(IInstance aInstance) throws Exception;

}
