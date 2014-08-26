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
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATDecoder;
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
        
        // Construct MBound parameters.
        // TODO: make smarter.
        MBoundParameters parameters = new MBoundParameters(0.2, 10, 10, 0.1, 1.5);
        
        log.debug("MBound has error probability {}.", MBoundAlgorithm.calculateMaximumErrorProbability(parameters));
        
        Pair<CNF, ISATDecoder> encodedInstance = fSATEncoder.encode(instanceWithHappyChannels);
        
        MBoundResult result = MBoundAlgorithm.solve(parameters, encodedInstance.getFirst(), fSATSolver, fSATSolverTerminationCriterion, aSeed);
        
        log.debug("MBound result {}.", result);
        
        // Minus one for the empty station subset introduced by the happy channels.
        return Math.round(result.getBound())-1;
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
