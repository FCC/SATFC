package ca.ubc.cs.beta.stationpacking.execution.parameters.smac;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;

import com.beust.jcommander.Parameter;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Created by newmanne on 11/06/15.
 */
@UsageTextField(title="SMAC PARAMETERS", description = "Not intended for human use", level = OptionLevel.DEVELOPER)
public class SATFCHydraParams extends AbstractOptions {

    public enum SolverType {
        CLASP,
        PRESOLVER,
        UNDERCONSTRAINED,
        CONNECTED_COMPONENTS,
        ARC_CONSISTENCY,
        UNSAT_PRESOLVER,
        NONE
    }

    public enum ClaspConfig {

        H1 (ClaspLibSATSolverParameters.UHF_CONFIG_04_15_h1),
        H2 (ClaspLibSATSolverParameters.UHF_CONFIG_04_15_h2);

        private final String config;

        ClaspConfig(String config) {
            this.config = config;
        }

        public String getConfig() {
            return config;
        }
    }

    public enum PresolverExpansion {
        NEIGHBOURHOOD, UNIFORM_RANDOM
    }
    
    @Parameter(names = "-presolver")
    public boolean presolver;
    @Parameter(names = "-presolverExpansionMethod")
    public PresolverExpansion presolverExpansionMethod;
    @Parameter(names = "-presolverNumNeighbours")
    public int presolverNumNeighbours;
    @Parameter(names = "-presolverIterativelyDeepen")
    public boolean presolverIterativelyDeepen;
    @Parameter(names = "-presolverCutoff")
    public double presolverCutoff;
    @Parameter(names = "-presolverBaseCutoff")
    public double presolverBaseCutoff;
    @Parameter(names = "-presolverScaleFactor")
    public double presolverScaleFactor;

    @Parameter(names = "-UNSATpresolver")
    public boolean UNSATpresolver;
    @Parameter(names = "-UNSATpresolverExpansionMethod")
    public PresolverExpansion UNSATpresolverExpansionMethod;
    @Parameter(names = "-UNSATpresolverNumNeighbours")
    public int UNSATpresolverNumNeighbours;
    @Parameter(names = "-UNSATpresolverIterativelyDeepen")
    public boolean UNSATpresolverIterativelyDeepen;
    @Parameter(names = "-UNSATpresolverCutoff")
    public double UNSATpresolverCutoff;
    @Parameter(names = "-UNSATpresolverBaseCutoff")
    public double UNSATpresolverBaseCutoff;
    @Parameter(names = "-UNSATpresolverScaleFactor")
    public double UNSATpresolverScaleFactor;

    @Parameter(names = "-arcConsistency")
    public boolean arcConsistency;
    @Parameter(names = "-arcConsistencyPriority")
    public int arcConsistencyPriority;

    @Parameter(names = "-underconstrained")
    public boolean underconstrained;
    @Parameter(names = "-underconstrainedPriority")
    public int underconstrainedPriority;
    @Parameter(names = "-underconstrainedExpensive")
    public boolean underconstrainedExpensive;


    @Parameter(names = "-connectedComponents")
    public boolean connectedComponents;
    @Parameter(names = "-connectedComponentsPriority")
    public int connectedComponentsPriority;

    public String claspConfig;

    public List<SolverType> getSolverOrder() {
        final List<SolverType> list = new ArrayList<>();
        final Map<SolverType, Integer> solverChoiceToPriority = getSolverTypePriorityMap();
        solverChoiceToPriority.entrySet().stream().sorted((a, b) -> a.getValue().compareTo(b.getValue())).forEach(entry -> {
            list.add(entry.getKey());
        });
        if (presolver) {
            list.add(SolverType.PRESOLVER);
        } else if (UNSATpresolver) {
            list.add(SolverType.UNSAT_PRESOLVER);
        }
        list.add(SolverType.CLASP);
        return list;
    }

    private Map<SolverType, Integer> getSolverTypePriorityMap() {
        final Map<SolverType, Integer> solverChoiceToPriority = new HashMap<>();
        if (arcConsistency) {
            solverChoiceToPriority.put(SolverType.ARC_CONSISTENCY, arcConsistencyPriority);
        }
        if (underconstrained) {
            solverChoiceToPriority.put(SolverType.UNDERCONSTRAINED, underconstrainedPriority);
        }
        if (connectedComponents) {
            solverChoiceToPriority.put(SolverType.CONNECTED_COMPONENTS, connectedComponentsPriority);
        }
        return solverChoiceToPriority;
    }

    public boolean validate() {
        Preconditions.checkNotNull(claspConfig);
        Preconditions.checkState(!(presolver && UNSATpresolver), "Can only use one presolver!");
        final Map<SolverType, Integer> solverTypePriorityMap = getSolverTypePriorityMap();
        Preconditions.checkState(solverTypePriorityMap.entrySet().size() == new HashSet<>(solverTypePriorityMap.values()).size(), "At least two options have the same priority! " + ImmutableMap.copyOf(solverTypePriorityMap).toString());
        return true;
    }

}
