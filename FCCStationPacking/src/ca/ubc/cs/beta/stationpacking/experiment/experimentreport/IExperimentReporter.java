package ca.ubc.cs.beta.stationpacking.experiment.experimentreport;

import ca.ubc.cs.beta.stationpacking.experiment.instance.IInstance;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SolverResult;

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
	public void report(IInstance aInstance, SolverResult aRunResult) throws InterruptedException;
	
}
