package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

<<<<<<< HEAD
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
=======

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.IStationManager;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.CNFEncoder;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.IncrementalSolver;
import ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.SATLibraries.GlueMiniSatLibrary;

>>>>>>> bb62b121321ea6a18ddba11d5b59dbaf2e930cc2
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

<<<<<<< HEAD
public class IncrementalSolverParameters extends AbstractOptions {


    //ExecutableSolver parameters
    @ParametersDelegate
    public ExecutableSolverParameters ExecutableSolverParameters = new ExecutableSolverParameters();

    @Parameter(names = "-LIBRARY", description = "Path to incremental SAT library.")
=======
public class IncrementalSolverParameters implements ISolverParameters {
	
	
	@Parameter(names = "-LIBRARY", description = "Path to incremental SAT library.")
>>>>>>> bb62b121321ea6a18ddba11d5b59dbaf2e930cc2
	private String fLibraryPath;
	
	
	public ISolver getSolver(IStationManager aStationManager, IConstraintManager aConstraintManager)
	{
		Logger log = LoggerFactory.getLogger(IncrementalSolverParameters.class);
		
		log.info("Creating CNF encoder...");
		ICNFEncoder aCNFEncoder = new CNFEncoder(aStationManager.getStations());
		
		return new IncrementalSolver(aConstraintManager, aCNFEncoder, new GlueMiniSatLibrary(fLibraryPath));
	}
}
