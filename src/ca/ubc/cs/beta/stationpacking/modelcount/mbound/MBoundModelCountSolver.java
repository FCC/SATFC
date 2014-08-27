package ca.ubc.cs.beta.stationpacking.modelcount.mbound;

import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.modelcount.IModelCountSolver;
import ca.ubc.cs.beta.stationpacking.modelcount.mbound.algorithm.MBoundAlgorithm;
import ca.ubc.cs.beta.stationpacking.modelcount.mbound.base.MBoundResult;
import ca.ubc.cs.beta.stationpacking.modelcount.mbound.parameters.MBoundParameters;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ISATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

public class MBoundModelCountSolver implements IModelCountSolver {

    private static final Logger log = LoggerFactory.getLogger(MBoundModelCountSolver.class);

    /**
     * A dummy channel which has no interferences associated to it.
     */
    private static final int HAPPYCHANNEL = 0;

    private final ISATEncoder fSATEncoder;
    private final ISATSolver fSATSolver;
    private final ITerminationCriterion fSATSolverTerminationCriterion;


    /**
     * @param aSATEncoder - an encoder which encodes station packing instances into CNFs.
     * @param aSATSolver - a SAT solver which solves CNFs.
     * @param aTerminationCriterion - the termination criterion for a single SAT solver execution (usually cutoff time based).
     */
    public MBoundModelCountSolver(ISATEncoder aSATEncoder, ISATSolver aSATSolver, ITerminationCriterion aTerminationCriterion) {
        fSATEncoder = aSATEncoder;
        fSATSolver = aSATSolver;
        fSATSolverTerminationCriterion = aTerminationCriterion;
    }

    @Override
    public Long countSatisfiablePackings(StationPackingInstance aInstance, long aSeed) {

        // Add a non-interference channel to every station.
        // This allows us to find solutions which only contain a subset of the stations.
        // With the exception of every station choosing the happy channel, every solution is valid.

        Map<Station, Set<Integer>> domains = aInstance.getDomains();
        for (Station s : domains.keySet()) {
            Set<Integer> channels = domains.get(s);
            channels.add(HAPPYCHANNEL);
            domains.put(s, channels);
        }

        StationPackingInstance instanceWithHappyChannels = new StationPackingInstance(domains, aInstance.getPreviousAssignment());

        CNF encodedInstance = fSATEncoder.encode(instanceWithHappyChannels).getFirst();

        Pair<Double, Double> lowerUpperBounds = getLowerAndUpperBounds(encodedInstance, aSeed);

        Double lower = lowerUpperBounds.getFirst();
        Double upper = lowerUpperBounds.getSecond();

        // Minus one for the empty station subset solution.

        if (Double.isNaN(lower) && Double.isNaN(upper)) {
            return null;

        } else if (Double.isNaN(lower)) {
            return Math.round(upper)-1;

        } else if (Double.isNaN(upper)) {
            return Math.round(lower)-1;

        } else {
            return Math.round((lower + upper) / 2.0)-1;

        }
    }

    private Pair<Double, Double> getLowerAndUpperBounds(CNF aCNF, Long aSeed) {
        // Construct MBound parameters.
        // TODO: make smarter.
        MBoundParameters parameters = new MBoundParameters(0.2, 10, 10, 0.1, 1.5);

        log.debug("MBound has error probability {}.", MBoundAlgorithm.calculateMaximumErrorProbability(parameters));

        MBoundResult result = MBoundAlgorithm.solve(parameters, aCNF, fSATSolver, fSATSolverTerminationCriterion, aSeed);

        log.debug("MBound result {}.", result);

        return new Pair<Double,Double>(result.getBound(), result.getBound());
    }

    @Override
    public Long countSatisfiablePackingsContainingStation(StationPackingInstance aInstance, Station aStation, long aSeed) {

        // Add a non-interference channel to every station except the target station.

        Map<Station, Set<Integer>> domains = aInstance.getDomains();
        for (Station s : domains.keySet()) {
            Set<Integer> channels = domains.get(s);
            
            if (!s.equals(aStation)) {
                channels.add(HAPPYCHANNEL);
            }
            domains.put(s, channels);
        }

        StationPackingInstance instanceWithHappyChannels = new StationPackingInstance(domains, aInstance.getPreviousAssignment());

        CNF encodedInstance = fSATEncoder.encode(instanceWithHappyChannels).getFirst();

        Pair<Double, Double> lowerUpperBounds = getLowerAndUpperBounds(encodedInstance, aSeed);

        Double lower = lowerUpperBounds.getFirst();
        Double upper = lowerUpperBounds.getSecond();


        if (Double.isNaN(lower) && Double.isNaN(upper)) {
            return null;

        } else if (Double.isNaN(lower)) {
            return Math.round(upper);

        } else if (Double.isNaN(upper)) {
            return Math.round(lower);

        } else {
            return Math.round((lower + upper) / 2.0);

        }
        
    }

    @Override
    public void interrupt() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void notifyShutdown() {
        fSATSolver.notifyShutdown();
    }

}
