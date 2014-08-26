package ca.ubc.cs.beta.stationpacking.modelcount;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;

/**
 * A model count estimator.
 * 
 * @author tqichen
 *
 */
public interface IModelCountSolver {
    
    /**
     * 
     * Produces a count (or estimate) of the total number of SAT results involving any
     * <i>station subset</i> of the given instance, excluding the empty subset.
     * 
     * @param aInstance - the instance to be counted.
     * @param aSeed - the execution seed.
     * @return
     */
    public Long countSatisfiablePackings(StationPackingInstance aInstance, long aSeed);
    
    /**
     * Tries to stop the solve call if implemented, if not throws an UnsupportedOperationException.
     */
    public void interrupt() throws UnsupportedOperationException;
    
    /**
     * Ask the solver to shutdown.
     */
    public void notifyShutdown();

}