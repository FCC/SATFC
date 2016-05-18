/**
 * Copyright 2016, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
 *
 * This file is part of SATFC.
 *
 * SATFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SATFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeParameter;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.ManagerBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.yaml.*;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.factories.Clasp3LibraryGenerator;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.factories.PythonInterpreterContainer;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.factories.UBCSATLibraryGenerator;
import ca.ubc.cs.beta.stationpacking.polling.IPollingService;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.UNSATLabeller;
import ca.ubc.cs.beta.stationpacking.solvers.VoidSolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.ConstraintGraphNeighborhoodPresolver;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.StationSubsetSATCertifier;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.strategies.*;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.composites.ISolverFactory;
import ca.ubc.cs.beta.stationpacking.solvers.composites.ParallelNoWaitSolverComposite;
import ca.ubc.cs.beta.stationpacking.solvers.composites.ParallelSolverComposite;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.*;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.CacheResultDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.SubsetCacheUNSATDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.SupersetCacheSATDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.consistency.ArcConsistencyEnforcerDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.consistency.ChannelKillerDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.sat.CompressedSATBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.Clasp3SATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.ubcsat.UBCSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.underconstrained.HeuristicUnderconstrainedStationFinder;
import ca.ubc.cs.beta.stationpacking.utils.YAMLUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import java.io.IOException;
import java.util.*;

/**
 * Created by newmanne on 01/10/15.
 * Builds a solver bundle based on a YAML file
 */
@Slf4j
public class YAMLBundle extends AVHFUHFSolverBundle {

    @Getter
    private final ISolver UHFSolver;
    @Getter
    private final ISolver VHFSolver;

    private static boolean skipJython = false;
    private final String checkers;

    public YAMLBundle(
            @NonNull ManagerBundle managerBundle,
            @NonNull SATFCFacadeParameter parameter,
            @NonNull IPollingService pollingService,
            CloseableHttpAsyncClient httpClient
    ) {
        super(managerBundle);

        log.info("Using the following variables to build the bundle: configFile={}, serverURL={}, clasp={}, SATenstein={}, resultFile={}", parameter.getConfigFile(), parameter.getServerURL(), parameter.getClaspLibrary(), parameter.getSatensteinLibrary(), parameter.getResultFile());

        final SATFCContext context = SATFCContext
                .builder()
                .managerBundle(managerBundle)
                .clasp3LibraryGenerator(new Clasp3LibraryGenerator(parameter.getClaspLibrary()))
                .ubcsatLibraryGenerator(new UBCSATLibraryGenerator(parameter.getSatensteinLibrary()))
                .parameter(parameter)
                .pollingService(pollingService)
                .httpClient(httpClient)
                .build();

        log.info("Reading configuration file {}", parameter.getConfigFile());
        final String configYAMLString = parameter.getConfigFile().getFileAsString();

        log.trace("Running configuration file through YAML parser to handle anchors...");

        // Need to run the text through the YAML class to process YAML anchors properly
        final Yaml yaml = new Yaml();
        final Map<?, ?> normalized = (Map<?, ?>) yaml.load(configYAMLString);
        final String anchoredYAML;
        try {
            anchoredYAML = YAMLUtils.getMapper().writeValueAsString(normalized);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Couldn't parse YAML from file " + parameter.getConfigFile(), e);
        }

        log.info("Parsing configuration file {}", parameter.getConfigFile());

        final YAMLBundleConfig config;
        try {
            config = YAMLUtils.getMapper().readValue(anchoredYAML, YAMLBundleConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't parse YAML from file " + parameter.getConfigFile(), e);
        }

        log.info("Configuration parsed! Building solvers...");
        final List<ISolverConfig> uhf = config.getUHF();
        final List<ISolverConfig> vhf = config.getVHF();
        Preconditions.checkState(uhf != null && !uhf.isEmpty(), "No solver provided for UHF in config file %s", parameter.getConfigFile());
        Preconditions.checkState(vhf != null && !vhf.isEmpty(), "No solver provided for VHF in config file %s", parameter.getConfigFile());

        UHFSolver = concat(uhf, context);
        VHFSolver = concat(vhf, context);

        checkers = Joiner.on(',').join(context.getSolverTypes());
    }

    @Override
    public String getCheckers() {
        return checkers;
    }


    private static ISolver concat(List<ISolverConfig> configs, SATFCContext context) {
        log.debug("Starting with a void solver...");
        ISolver solver = new VoidSolver();
        for (ISolverConfig config : configs) {
            if (!config.shouldSkip(context)) {
                log.debug("Decorating with {} using config of type {}", solver.getClass().getSimpleName(), config.getClass().getSimpleName());
                solver = config.createSolver(context, solver);
                context.getSolverTypes().add(SolverConfigDeserializer.typeToConfigClass.inverse().get(config.getClass()));
            } else {
                log.debug("Skipping decorator {}", config.getClass().getSimpleName());
            }
        }
        return solver;
    }

    @Builder
    @Data
    public static class SATFCContext {
        private final Clasp3LibraryGenerator clasp3LibraryGenerator;
        private final UBCSATLibraryGenerator ubcsatLibraryGenerator;
        private final ManagerBundle managerBundle;
        private final SATFCFacadeParameter parameter;
        private final IPollingService pollingService;
        private final CloseableHttpAsyncClient httpClient;
        private PythonInterpreterContainer python;

        private final Set<SolverType> solverTypes = new HashSet<>();
    }

    /**
     * Parse out separate configurations for VHF and UHF
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YAMLBundleConfig {
        @JsonProperty("UHF")
        private List<ISolverConfig> UHF;
        @JsonProperty("VHF")
        private List<ISolverConfig> VHF;

    }

    @Data
    public static class ClaspConfig implements ISolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            final IConstraintManager constraintManager = context.getManagerBundle().getConstraintManager();
            final Clasp3LibraryGenerator clasp3LibraryGenerator = context.getClasp3LibraryGenerator();
            final AbstractCompressedSATSolver claspSATsolver = new Clasp3SATSolver(clasp3LibraryGenerator.createLibrary(), config, seedOffset, context.getPollingService(), nickname);
            return new CompressedSATBasedSolver(claspSATsolver, new SATCompressor(constraintManager, encodingType));
        }

        private String config;
        private EncodingType encodingType = EncodingType.DIRECT;
        private int seedOffset = 0;
        private String nickname;

    }

    @Data
    public static class UBCSATConfig implements ISolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            final IConstraintManager constraintManager = context.getManagerBundle().getConstraintManager();
            final UBCSATLibraryGenerator ubcsatLibraryGenerator = context.getUbcsatLibraryGenerator();
            final AbstractCompressedSATSolver ubcsatSolver = new UBCSATSolver(ubcsatLibraryGenerator.createLibrary(), config, seedOffset, context.getPollingService(), nickname);
            return new CompressedSATBasedSolver(ubcsatSolver, new SATCompressor(constraintManager, encodingType));
        }

        private String config;
        private EncodingType encodingType = EncodingType.DIRECT;
        private int seedOffset = 0;
        private String nickname;

    }

    @Data
    public static class AssignmentVerifierConfig implements ISolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new AssignmentVerifierDecorator(solverToDecorate, context.getManagerBundle().getConstraintManager(), context.getManagerBundle().getStationManager());
        }

    }

    @Data
    public static class PythonVerifierConfig implements ISolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new PythonAssignmentVerifierDecorator(solverToDecorate, context.getPython());
        }

        @Override
        public boolean shouldSkip(SATFCContext context) {
            if (skipJython) {
                return true;
            } else {
                if (context.getPython() != null) {
                    return false;
                } else {
                    try {
                        final PythonInterpreterContainer pythonInterpreterContainer = new PythonInterpreterContainer(context.getManagerBundle().getInterferenceFolder(), context.getManagerBundle().isCompactInterference());
                        context.setPython(pythonInterpreterContainer);
                        return false;
                    } catch (Exception e) {
                        log.warn("Could not initialize jython. Secondary assignment verifier will be skipped", e);
                        skipJython = true;
                        return true;
                    }
                }
            }
        }

    }

    @Data
    public static class ConnectedComponentsConfig implements ISolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new ConnectedComponentGroupingDecorator(solverToDecorate, new ConstraintGrouper(), context.getManagerBundle().getConstraintManager(), solveEverything);
        }

        private boolean solveEverything = false;
    }

    @Data
    public static class ArcConsistencyConfig implements ISolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new ArcConsistencyEnforcerDecorator(solverToDecorate, context.getManagerBundle().getConstraintManager());
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class CacheConfig extends CacheSolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new CacheResultDecorator(solverToDecorate, createContainmentCacheProxy(context), new CacheResultDecorator.CachingStrategy() {

                private final CacheResultDecorator.CacheConclusiveNewInfoStrategy strategy = new CacheResultDecorator.CacheConclusiveNewInfoStrategy();

                @Override
                public boolean shouldCache(SolverResult result) {
                    if (result.getResult().equals(SATResult.UNSAT) && doNotCacheUNSAT) {
                        return false;
                    } else if (result.getResult().equals(SATResult.SAT) && doNotCacheSAT) {
                        return false;
                    } else if (result.getRuntime() < minTimeToCache) {
                        return false;
                    } else {
                        return strategy.shouldCache(result);
                    }
                }
            });
        }

        private double minTimeToCache = 0;
        private boolean doNotCacheUNSAT = false;
        private boolean doNotCacheSAT = false;

    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class SATCacheConfig extends CacheSolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new SupersetCacheSATDecorator(solverToDecorate, createContainmentCacheProxy(context));
        }

    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class UNSATCacheConfig extends CacheSolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new SubsetCacheUNSATDecorator(solverToDecorate, createContainmentCacheProxy(context));
        }

    }

    @Data
    public static class UnderconstrainedConfig implements ISolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new UnderconstrainedStationRemoverSolverDecorator(solverToDecorate, context.getManagerBundle().getConstraintManager(), new HeuristicUnderconstrainedStationFinder(context.getManagerBundle().getConstraintManager(), expensive), recursive);
        }

        private boolean expensive = true;
        private boolean recursive = true;
    }

    @Data
    public static class ResultSaverConfig implements ISolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new ResultSaverSolverDecorator(solverToDecorate, context.getParameter().getResultFile());
        }

        @Override
        public boolean shouldSkip(SATFCContext context) {
            return context.getParameter().getResultFile() == null;
        }
    }


    @Data
    public static class SATPresolver implements ISolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new ConstraintGraphNeighborhoodPresolver(solverToDecorate,
                    new StationSubsetSATCertifier(solverConfig.createSolver(context)),
                    strategy.createStrategy(),
                    context.getManagerBundle().getConstraintManager());
        };

        private ISolverConfig solverConfig;
        private IStationPackingConfigurationStrategyConfig strategy;

    }

    @Data
    public static class UNSATPresolver implements ISolverConfig {


        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new ConstraintGraphNeighborhoodPresolver(solverToDecorate,
                    new StationSubsetSATCertifier(solverConfig.createSolver(context)),
                    strategy.createStrategy(),
                    context.getManagerBundle().getConstraintManager());
        };


        private ISolverConfig solverConfig;
        private IStationPackingConfigurationStrategyConfig strategy;

    }

    @Data
    public static class ParallelConfig implements ISolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            log.debug("Adding a parallel split");
            List<ISolverFactory> solverFactories = new ArrayList<>();
            for (final List<ISolverConfig> configPath : configs) {
                solverFactories.add(aSolver -> concat(configPath, context));
            }
            if (wait) {
                return new ParallelSolverComposite(solverFactories.size(), solverFactories);
            } else {
                return new ParallelNoWaitSolverComposite(solverFactories.size(), solverFactories);
            }
        }

        private boolean wait = false;
        private List<List<ISolverConfig>> configs;

    }

    @Data
    public static class NeighbourLayerConfig implements IStationAddingStrategyConfig {

        @Override
        public IStationAddingStrategy createStrategy() {
            return new AddNeighbourLayerStrategy(numLayers);
        }

        private int numLayers = Integer.MAX_VALUE;
    }

    @Data
    public static class AddRandomNeighbourConfig implements IStationAddingStrategyConfig {

        @Override
        public IStationAddingStrategy createStrategy() {
            return new AddRandomNeighboursStrategy(numNeighbours);
        }

        private int numNeighbours;
    }

    @Data
    public static class IterativeDeepeningStrategyConfig implements IStationPackingConfigurationStrategyConfig {

        @Override
        public IStationPackingConfigurationStrategy createStrategy() {
            return new IterativeDeepeningConfigurationStrategy(config.createStrategy(), baseCutoff, scalingFactor);
        }

        private double scalingFactor;
        private double baseCutoff;
        private IStationAddingStrategyConfig config;

    }

    @Data
    public static class CNFSaverConfig implements ISolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new CNFSaverSolverDecorator(solverToDecorate, context.getManagerBundle().getConstraintManager(), context.getParameter().getCNFSaver(), encodingType, true);
        }

        @Override
        public boolean shouldSkip(SATFCContext context) {
            return context.getParameter().getCNFSaver() == null;
        }

        EncodingType encodingType = EncodingType.DIRECT;
        
    }


    @Data
    public static class ChannelKillerConfig implements ISolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new ChannelKillerDecorator(solverToDecorate, solverConfig.createSolver(context), context.getManagerBundle().getConstraintManager(), time, recurisve);
        }

        private double time;
        private boolean recurisve;
        private ISolverConfig solverConfig;

    }

    @Data
    public static class DelayedSolverConfig implements ISolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new DelayedSolverDecorator(solverToDecorate, time);
        }

        private double time;
        private double noise;

    }

    @Data
    public static class TimeBoundedSolverConfig implements ISolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new TimeBoundedSolverDecorator(solverToDecorate, solverConfig.createSolver(context), time);
        }

        private ISolverConfig solverConfig;
        private double time;
    }

    @Data
    public static class UNSATLabellerConfig implements ISolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new UNSATLabeller(solverToDecorate);
        }

    }


    @Data
    public static class PreviousAssignmentConfig implements ISolverConfig {

        @Override
        public ISolver createSolver(SATFCContext context, ISolver solverToDecorate) {
            return new PreviousAssignmentContainsAnswerDecorator(solverToDecorate, context.getManagerBundle().getConstraintManager());
        }

    }


    public enum PresolverExpansion {
        NEIGHBOURHOOD, UNIFORM_RANDOM
    }

    public enum TimingChoice {
        ITERATIVE_DEEPEN
    }

    public static abstract class ANameArgsDeserializer<KEYTYPE extends Enum<KEYTYPE>, CLASSTYPE> extends JsonDeserializer<CLASSTYPE> {

        @Override
        public CLASSTYPE deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode root = p.readValueAsTree();
            Preconditions.checkState(root.has("name"), "name is a required field!");
            final String name = root.get("name").asText();
            final KEYTYPE enumValue = stringToEnum(name);
            final Class<? extends CLASSTYPE> targetClass = getMap().get(enumValue);
            Preconditions.checkNotNull(targetClass, "No known conversion class for type %s", enumValue);
            if (root.has("args")) {
                return YAMLUtils.getMapper().treeToValue(root.get("args"), targetClass);
            } else {
                try {
                    return targetClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException("Failed to instantiate an instance of " + targetClass.getSimpleName(), e);
                }
            }
        }

        protected abstract KEYTYPE stringToEnum(String string);

        protected abstract Map<KEYTYPE, Class<? extends CLASSTYPE>> getMap();
    }

    public static class SolverConfigDeserializer extends ANameArgsDeserializer<SolverType, ISolverConfig> {

        public static final BiMap<SolverType, Class<? extends ISolverConfig>> typeToConfigClass =
                ImmutableBiMap.<SolverType, Class<? extends ISolverConfig>>builder()
                        .put(SolverType.CLASP, ClaspConfig.class)
                        .put(SolverType.SATENSTEIN, UBCSATConfig.class)
                        .put(SolverType.SAT_PRESOLVER, SATPresolver.class)
                        .put(SolverType.UNSAT_PRESOLVER, UNSATPresolver.class)
                        .put(SolverType.UNDERCONSTRAINED, UnderconstrainedConfig.class)
                        .put(SolverType.CONNECTED_COMPONENTS, ConnectedComponentsConfig.class)
                        .put(SolverType.ARC_CONSISTENCY, ArcConsistencyConfig.class)
                        .put(SolverType.VERIFIER, AssignmentVerifierConfig.class)
                        .put(SolverType.CACHE, CacheConfig.class)
                        .put(SolverType.SAT_CACHE, SATCacheConfig.class)
                        .put(SolverType.UNSAT_CACHE, UNSATCacheConfig.class)
                        .put(SolverType.PARALLEL, ParallelConfig.class)
                        .put(SolverType.RESULT_SAVER, ResultSaverConfig.class)
                        .put(SolverType.CNF, CNFSaverConfig.class)
                        .put(SolverType.PYTHON_VERIFIER, PythonVerifierConfig.class)
                        .put(SolverType.CHANNEL_KILLER, ChannelKillerConfig.class)
                        .put(SolverType.DELAY, DelayedSolverConfig.class)
                        .put(SolverType.TIME_BOUNDED, TimeBoundedSolverConfig.class)
                        .put(SolverType.PREVIOUS_ASSIGNMENT, PreviousAssignmentConfig.class)
                        .put(SolverType.UNSAT_LABELLER, UNSATLabellerConfig.class)
                        .build();

        @Override
        protected SolverType stringToEnum(String string) {
            return SolverType.valueOf(string);
        }

        @Override
        protected Map<SolverType, Class<? extends ISolverConfig>> getMap() {
            return typeToConfigClass;
        }

    }

    public static class PresolverConfigurationDeserializer extends ANameArgsDeserializer<TimingChoice, IStationPackingConfigurationStrategyConfig> {

        private final Map<TimingChoice, Class<? extends IStationPackingConfigurationStrategyConfig>> typeToConfigClass = ImmutableMap.<TimingChoice, Class<? extends IStationPackingConfigurationStrategyConfig>>builder()
                .put(TimingChoice.ITERATIVE_DEEPEN, YAMLBundle.IterativeDeepeningStrategyConfig.class)
                .build();

        @Override
        protected TimingChoice stringToEnum(String string) {
            return TimingChoice.valueOf(string);
        }

        @Override
        protected Map<TimingChoice, Class<? extends IStationPackingConfigurationStrategyConfig>> getMap() {
            return typeToConfigClass;
        }

    }

    public static class StationAddingStrategyConfigurationDeserializer extends ANameArgsDeserializer<PresolverExpansion, IStationAddingStrategyConfig> {

        private final Map<PresolverExpansion, Class<? extends IStationAddingStrategyConfig>> typeToConfigClass = ImmutableMap.<PresolverExpansion, Class<? extends IStationAddingStrategyConfig>>builder()
                .put(PresolverExpansion.NEIGHBOURHOOD, NeighbourLayerConfig.class)
                .put(PresolverExpansion.UNIFORM_RANDOM, AddRandomNeighbourConfig.class)
                .build();

        @Override
        protected PresolverExpansion stringToEnum(String string) {
            return PresolverExpansion.valueOf(string);
        }

        @Override
        protected Map<PresolverExpansion, Class<? extends IStationAddingStrategyConfig>> getMap() {
            return typeToConfigClass;
        }

    }

}