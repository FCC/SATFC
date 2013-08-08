package ca.ubc.cs.beta.stationpacking.solver.reporters;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solver.SolverResult;

/**
 * Report and record experiment results.
 * @author afrechet
 *
 */
public interface IExperimentReporter {
	
	/**
	 * Record an instance-run result pair.
	 * @param aInstance - a problem instance.
	 * @param aRunResult - the result of solving the given instance.
	 * @throws InterruptedException 
	 */
	public void report(StationPackingInstance aInstance, SolverResult aRunResult);
	
}
