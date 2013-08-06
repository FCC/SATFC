package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.IStationManager;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.CNFEncoder;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.IncrementalSolver;
import ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.SATLibraries.GlueMiniSatSolver;

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
		ICNFEncoder aCNFEncoder = new CNFEncoder(aStationManager.getStations());
		
		GlueMiniSatSolver aGMSlibrary = new GlueMiniSatSolver(fLibraryPath);
		ISolver aSolver = new IncrementalSolver(aConstraintManager, aCNFEncoder, aGMSlibrary);
		
		return aSolver;
	}
}
