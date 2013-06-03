package ca.ubc.cs.beta.stationpacking.experiment.experimentreport;

import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;

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
	public void report(Instance aInstance, SolverResult aRunResult) throws InterruptedException;
	
}
