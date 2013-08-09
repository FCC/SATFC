package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.cnfencoder.SATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental.IncrementalSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental.SATSolver.GlueMiniSatSolver;

import com.beust.jcommander.Parameter;

@UsageTextField(title="FCC Station Packing Incremental Solver Options",description="Parameters defining an incremental SAT solver feasibility checker.")
public class IncrementalSolverParameters extends AbstractOptions {

    @Parameter(names = "-LIBRARY", description = "Path to incremental SAT library.")
	private String fLibraryPath;

    public String getIncrementalLibraryLocation(){
          return fLibraryPath;
    }
	
	public ISolver getSolver(IStationManager aStationManager, IConstraintManager aConstraintManager)
	{
		Logger log = LoggerFactory.getLogger(IncrementalSolverParameters.class);
		
		log.info("Creating CNF encoder...");
		ISATEncoder aCNFEncoder = new SATEncoder(aStationManager,aConstraintManager);
		
		log.info("Creating library...");
		GlueMiniSatSolver aGMSlibrary = new GlueMiniSatSolver(fLibraryPath);

		log.info("Creating incremental solver...");
		ISolver aSolver = new IncrementalSolver(aConstraintManager, aCNFEncoder, aGMSlibrary);
		
		return aSolver;
	}
}
