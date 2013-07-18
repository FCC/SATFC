package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.IStationManager;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.CNFEncoder;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.IncrementalSolver;
import ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.SATLibraries.GlueMiniSatLibrary;

import com.beust.jcommander.Parameter;

public class IncrementalSolverParameters implements ISolverParameters {
	
	
	@Parameter(names = "-LIBRARY", description = "Path to incremental SAT library.")
	private String fLibraryPath;
	
	
	public ISolver getSolver(IStationManager aStationManager, IConstraintManager aConstraintManager)
	{
		Logger log = LoggerFactory.getLogger(IncrementalSolverParameters.class);
		
		log.info("Creating CNF encoder...");
		ICNFEncoder aCNFEncoder = new CNFEncoder(aStationManager.getStations());
		
		return new IncrementalSolver(aConstraintManager, aCNFEncoder, new GlueMiniSatLibrary(fLibraryPath));
	}
}
