package ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ClaspSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ISATSolver;

import com.beust.jcommander.Parameter;

@UsageTextField(title="FCC Station Packing Packing Clasp Library Based SAT Solver Parameters",description="Parameters defining a Clasp library based SAT solver.")
public class ClaspLibSATSolverParameters extends AbstractOptions implements ISATSolverParameters{

	@Parameter(names = "--library", description = "path to the clasp library.", required=true)
	private String Library;
	
	@UsageTextField(defaultValues="Tuned Clasp March 2013 Configuration.")
	@Parameter(names = "--configuration", description = "clasp configuration.")
	private String Configuration = "--eq=0 --trans-ext=all --sat-prepro=0 --sign-def=2 --del-max=10000 --strengthen=local,1 --del-init-r=800,20000 --loops=no --init-watches=0 --heuristic=Berkmin --del-cfl=F,100 --restarts=L,256 --del-algo=basic,0 --deletion=3,66,3.0 --berk-max=256 --del-grow=1.0,100.0,F,128 --update-act --del-glue=4,0 --update-lbd=2 --reverse-arcs=3 --otfs=0 --berk-huang --del-on-restart=50 --contraction=120 --counter-restarts=3 --local-restarts --lookahead=no --save-progress=10 --counter-bump=180";
	
	@Override
	public ISATSolver getSATSolver() {
		return new ClaspSATSolver(Library, Configuration);
	}

	

}
