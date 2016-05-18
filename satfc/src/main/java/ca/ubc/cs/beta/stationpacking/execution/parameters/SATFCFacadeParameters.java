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
package ca.ubc.cs.beta.stationpacking.execution.parameters;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.base.InstanceParameters;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeParameter.SolverChoice;
import ch.qos.logback.classic.Level;

/**
 * SATFC facade parameters.
 * @author afrechet
 */
@UsageTextField(title="SATFC Facade Parameters",description="Parameters needed to execute SATFC facade on a single instance.")
public class SATFCFacadeParameters extends AbstractOptions {

    @Parameter(names={"--help-level"}, description="Show options at this level or lower")
    public OptionLevel helpLevel = OptionLevel.BASIC;

    /**
     * Parameters for the instance to solve.
     */
	@ParametersDelegate
	public InstanceParameters fInstanceParameters = new InstanceParameters();

    @UsageTextField(level = OptionLevel.DEVELOPER)
    @Parameter(names = "-CNF-DIR", description = "folder for storing cnf results")
    public String fCNFDir;

    @ParametersDelegate
    public SATFCCachingParameters cachingParams = new SATFCCachingParameters();

    @ParametersDelegate
    public RedisParameters fRedisParameters = new RedisParameters();

    @UsageTextField(level = OptionLevel.DEVELOPER)
    @Parameter(names = "-SRPK-FILE", description = "single srpk file to run")
    public String fsrpkFile;
    @UsageTextField(level = OptionLevel.DEVELOPER)
    @Parameter(names = "-INSTANCES-FILE", description = "file listing each instance file on a separate line")
    public String fFileOfInstanceFiles;
    @UsageTextField(level = OptionLevel.DEVELOPER)
    @Parameter(names = "-CSV-ROOT", description = "Root of CSV auction directory")
    public String fCsvRoot;
    @UsageTextField(level = OptionLevel.DEVELOPER)
    @Parameter(names = "-CHECK-FOR-SOLUTION", description = "Test for a solution in redis matching instancename, used to parse metrics files into cache")
    public boolean checkForSolution = false;
    @UsageTextField(level = OptionLevel.DEVELOPER)
    @Parameter(names = {"-METRICS-FILE", "-OUTPUT-FILE"}, description = "Causes the FileMetricWriter to be used, outputs a file with metrics (may cause performance loss)")
    public String fMetricsFile;
    @UsageTextField(level = OptionLevel.DEVELOPER)
    @Parameter(names = "-INTERFERENCES-FOLDER", description = "folder containing all the other interference folders")
    public String fInterferencesFolder;
    @UsageTextField(level = OptionLevel.DEVELOPER)
    @Parameter(names = "-CUTOFF-FILE", description = "file listing each instance and the corresponding cutoff")
    public String fCutoffFile;

    /**
	 * Clasp library to use (optional - can be automatically detected).
	 */
	@Parameter(names = "-CLASP-LIBRARY",description = "clasp library file")
	public String fClaspLibrary;

	/**
     * SATenstein library to use (optional - can be automatically detected).
     */
    @Parameter(names = "-SATENSTEIN-LIBRARY",description = "SATenstein library file")
    public String fSATensteinLibrary;

	/**
	 * Logging options.
	 */
	@Parameter(names={"--log-level","--logLevel", "-LOG-LEVEL"},description="messages will only be logged if they are of this severity or higher.")
	private String logLevel = "INFO";
    @Parameter(names={"-LOG-FILE"},description="Log file name")
    public String logFileName = "SATFC.log";

    public Level getLogLevel() {
        return Level.valueOf(logLevel);
    }

    @Parameter(names = "-CONFIG-FILE")
    public String configFile;
    
    @Parameter(names = "-SOLVER-CHOICE", hidden=true)
    public SolverChoice solverChoice = SolverChoice.YAML;

}
