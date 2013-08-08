package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.CNFEncoder_old;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.ICNFEncoder_old;
import ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.IncrementalSolver;
import ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.SATSolver.GlueMiniSatSolver;

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
		ICNFEncoder_old aCNFEncoder = new CNFEncoder_old(aStationManager.getStations());
		
		log.info("Creating library...");
		GlueMiniSatSolver aGMSlibrary = new GlueMiniSatSolver(fLibraryPath);

		log.info("Creating incremental solver...");
		ISolver aSolver = new IncrementalSolver(aConstraintManager, aCNFEncoder, aGMSlibrary);
		
		return aSolver;
	}
}
