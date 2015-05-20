package ca.ubc.cs.beta.stationpacking.solvers;

import lombok.Value;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

@Value
public class SolverData {
		private final StationPackingInstance instance;
		private final ITerminationCriterion terminationCriterion;
		private final long seed;
		private final Watch watch;
}
