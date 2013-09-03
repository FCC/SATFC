package ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ClaspSATSolver;

import com.beust.jcommander.Parameter;

@UsageTextField(title="FCC Station Packing Packing Clasp Library Based SAT Solver Parameters",description="Parameters defining a Clasp library based SAT solver.")
public class ClaspLibSATSolverParameters extends AbstractOptions implements ISATSolverParameters{
	
	private final static String ORIGINAL_CONFIG_03_13 = "--eq=0 --trans-ext=all --sat-prepro=0 --sign-def=2 --del-max=10000 --strengthen=local,1 --del-init-r=800,20000 --loops=no --init-watches=0 --heuristic=Berkmin --del-cfl=F,100 --restarts=L,256 --del-algo=basic,0 --deletion=3,66,3.0 --berk-max=256 --del-grow=1.0,100.0,F,128 --update-act --del-glue=4,0 --update-lbd=2 --reverse-arcs=3 --otfs=0 --berk-huang --del-on-restart=50 --contraction=120 --counter-restarts=3 --local-restarts --lookahead=no --save-progress=10 --counter-bump=180";
	private final static String HVHF_SHORT_01_09_13 = "--eq=0 --trans-ext=card --sat-prepro=0 --sign-def=0 --del-max=100000 --strengthen=recursive,1 --vmtf-mtf=8 --del-init-r=800,9000 --loops=no --init-watches=2 --heuristic=Unit --del-cfl=F,128,1000,5 --restarts=L,500,10,100 --del-algo=basic,1 --deletion=1,50,3.0 --berk-max=1024 --del-grow=1.5,10.0,L,32,1000,10 --update-act --del-glue=3,0 --update-lbd=3 --reverse-arcs=0 --vsids-decay=96 --otfs=0 --berk-huang --init-moms --del-on-restart=0 --contraction=120 --counter-restarts=3 --local-restarts --lookahead=hybrid,10 --save-progress=50 --counter-bump=180 --sign-fix";
	
	@Parameter(names = "--library", description = "path to the clasp library.", required=true)
	private String Library;
	
	@UsageTextField(defaultValues="-See code-")
	@Parameter(names = "--configuration", description = "clasp configuration.")
	private String Configuration = ORIGINAL_CONFIG_03_13;
	
	                              
	@Override
	public AbstractCompressedSATSolver getSATSolver() {
		return new ClaspSATSolver(Library, Configuration);
	}

	

}
