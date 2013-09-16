package ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ClaspSATSolver;

import com.beust.jcommander.Parameter;

@UsageTextField(title="FCC Station Packing Packing Clasp Library Based SAT Solver Parameters",description="Parameters defining a Clasp library based SAT solver.")
public class ClaspLibSATSolverParameters extends AbstractOptions implements ISATSolverParameters{
	
	private final static String ORIGINAL_CONFIG_03_13 = "--eq=0 --trans-ext=all --sat-prepro=0 --sign-def=2 --del-max=10000 --strengthen=local,1 --del-init-r=800,20000 --loops=no --init-watches=0 --heuristic=Berkmin --del-cfl=F,100 --restarts=L,256 --del-algo=basic,0 --deletion=3,66,3.0 --berk-max=256 --del-grow=1.0,100.0,F,128 --update-act --del-glue=4,0 --update-lbd=2 --reverse-arcs=3 --otfs=0 --berk-huang --del-on-restart=50 --contraction=120 --counter-restarts=3 --local-restarts --lookahead=no --save-progress=10 --counter-bump=180";
	private final static String HVHF_CONFIG_08_13 = "--eq=5 --trans-ext=choice --sat-prepro=0 --sign-def=2 --strengthen=local,2 --loops=shared --init-watches=2 --heuristic=Vsids --del-cfl=L,1000 --restarts=x,64,1.3,1 --del-algo=basic,1 --deletion=2,25,5.0 --update-act --del-glue=4,0 --update-lbd=0 --reverse-arcs=0 --vsids-decay=97 --otfs=1 --del-on-restart=30 --contraction=200 --counter-restarts=7 --local-restarts --lookahead=no --save-progress=25 --counter-bump=1024";
	
	@Parameter(names = "--library", description = "path to the clasp library.", required=true)
	private String Library;
	
	@UsageTextField(defaultValues="-See code-")
	@Parameter(names = "--configuration", description = "clasp configuration.")
	private String Configuration = HVHF_CONFIG_08_13;
	
	                              
	@Override
	public AbstractCompressedSATSolver getSATSolver() {
		return new ClaspSATSolver(Library, Configuration);
	}

	

}
