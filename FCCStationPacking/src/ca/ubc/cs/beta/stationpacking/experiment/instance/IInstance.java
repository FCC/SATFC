package ca.ubc.cs.beta.stationpacking.experiment.instance;

import java.util.ArrayList;

/**
 * A station packing problem instance container object.
 * @author afrechet
 *
 */
public interface IInstance {
	
	
	@Override
	public String toString();
	
	/**
	 * @return The CNF file names associated with the problem instance.
	 */
	public ArrayList<String> getCNFs();
	
	/**
	 * @return Return the number of stations represented by instance.
	 */
	public int getNumberofStations();
	
	
}
