package ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ClaspSATSolver;

import com.beust.jcommander.Parameter;

@UsageTextField(title="FCC Station Packing Packing Clasp Library Based SAT Solver Parameters",description="Parameters defining a Clasp library based SAT solver.")
public class ClaspLibSATSolverParameters extends AbstractOptions implements ISATSolverParameters{
	
	private final static String ORIGINAL_CONFIG_03_13 = "--eq=0 --trans-ext=all --sat-prepro=0 --sign-def=2 --del-max=10000 --strengthen=local,1 --del-init-r=800,20000 --loops=no --init-watches=0 --heuristic=Berkmin --del-cfl=F,100 --restarts=L,256 --del-algo=basic,0 --deletion=3,66,3.0 --berk-max=256 --del-grow=1.0,100.0,F,128 --update-act --del-glue=4,0 --update-lbd=2 --reverse-arcs=3 --otfs=0 --berk-huang --del-on-restart=50 --contraction=120 --counter-restarts=3 --local-restarts --lookahead=no --save-progress=10 --counter-bump=180";
	private final static String HVHF_CONFIG_09_13 = "--backprop --eq=0 --trans-ext=all --sat-prepro=0 --sign-def=0 --del-max=100000 --strengthen=local,1 --del-init-r=500,6000 --loops=common --init-watches=0 --heuristic=Vsids --del-cfl=F,500 --restarts=D,100,0.8,100 --del-algo=inp_sort,1 --deletion=2,75,3.0 --update-act --del-glue=4,0 --update-lbd=0 --reverse-arcs=3 --vsids-decay=96 --otfs=2 --del-on-restart=0 --contraction=500 --local-restarts --lookahead=no --save-progress=50";
	
	@Parameter(names = "--library", description = "path to the clasp library.", required=true)
	private String Library;
	
	@UsageTextField(defaultValues="-See code-")
	@Parameter(names = "--configuration", description = "clasp configuration.")
	private String Configuration = HVHF_CONFIG_09_13;
	
	                              
	@Override
	public AbstractCompressedSATSolver getSATSolver() {
		return new ClaspSATSolver(Library, Configuration);
	}

	

}
