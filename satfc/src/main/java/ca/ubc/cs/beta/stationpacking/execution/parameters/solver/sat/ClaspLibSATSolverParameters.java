/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
 *
 * This file is part of satfc.
 *
 * satfc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * satfc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with satfc.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.ClaspSATSolver;

import com.beust.jcommander.Parameter;

/**
 * Clasp SAT solver library parameters.
 * @author afrechet
 */
@UsageTextField(title="FCC Station Packing Packing Clasp Library Based SAT Solver Parameters",description="Parameters defining a Clasp library based SAT solver.")
public class ClaspLibSATSolverParameters extends AbstractOptions implements ISATSolverParameters{
	
    /**
     * Clasp configuration - 03/13.
     */
	public final static String ORIGINAL_CONFIG_03_13 = "--eq=0 --trans-ext=all --sat-prepro=0 --sign-def=2 --del-max=10000 --strengthen=local,1 --del-init-r=800,20000 --loops=no --init-watches=0 --heuristic=Berkmin --del-cfl=F,100 --restarts=L,256 --del-algo=basic,0 --deletion=3,66,3.0 --berk-max=256 --del-grow=1.0,100.0,F,128 --update-act --del-glue=4,0 --update-lbd=2 --reverse-arcs=3 --otfs=0 --berk-huang --del-on-restart=50 --contraction=120 --counter-restarts=3 --local-restarts --lookahead=no --save-progress=10 --counter-bump=180";
	/**
	 * Clasp configuration - 11/13 - all data.
	 */
	public final static String ALL_CONFIG_11_13 =      "--eq=0 --trans-ext=weight --sat-prepro=0 --sign-def=2 --del-max=10000 --strengthen=local,1 --del-init-r=800,20000 --loops=no --reverse-arcs=3 --heuristic=Vsids --del-cfl=F,100 --restarts=L,256 --del-algo=basic,0 --deletion=3,66,3.0 --del-grow=1.0,100.0,F,128 --update-act --del-glue=4,0 --update-lbd=2 --init-watches=2 --vsids-decay=70 --otfs=2 --del-on-restart=50 --contraction=120 --counter-restarts=3 --local-restarts --lookahead=atom,1 --save-progress=180 --counter-bump=180";
	/**
	 * Clasp configuration - 09/13 - UHF data.
	 */
	public final static String UHF_CONFIG_09_13 =      "--eq=0 --trans-ext=no --sat-prepro=0 --sign-def=0 --del-max=10000 --strengthen=local,2 --del-init-r=800,15000 --loops=common --reverse-arcs=3 --heuristic=Vsids --del-cfl=F,128 --restarts=L,32 --del-algo=basic,0 --del-estimate --del-grow=2.0,10.0,x,32,1.1,10 --update-act --del-glue=3,0 --update-lbd=0 --init-watches=0 --deletion=3,25,4.0 --vsids-decay=97 --otfs=0 --del-on-restart=50 --contraction=250 --counter-restarts=5 --lookahead=no --save-progress=180 --counter-bump=4096";
	/**
	 * Clasp configuration - 11/13 - UHF data.
	 */
	public final static String UHF_CONFIG_11_13 =      "--eq=0 --trans-ext=weight --sat-prepro=0 --sign-def=0 --del-max=20000 --strengthen=recursive,1 --del-init-r=800,20000 --loops=no --reverse-arcs=0 --heuristic=Berkmin --del-cfl=F,4096 --restarts=L,256 --del-algo=basic,0 --del-estimate --berk-max=256 --del-grow=1.0,100.0,F,128 --update-act --del-glue=3,0 --update-lbd=2 --init-watches=0 --deletion=3,66,3.0 --otfs=0 --berk-huang --del-on-restart=30 --contraction=120 --counter-restarts=3 --local-restarts --lookahead=no --save-progress=10 --counter-bump=180";
	/**
	 * Clasp configuration - 08/13 - HVHF data.
	 */
	public final static String HVHF_CONFIG_08_13 =     "--eq=5 --trans-ext=choice --sat-prepro=0 --sign-def=2 --strengthen=local,2 --loops=shared --init-watches=2 --heuristic=Vsids --del-cfl=L,1000 --restarts=x,64,1.3,1 --del-algo=basic,1 --deletion=2,25,5.0 --update-act --del-glue=4,0 --update-lbd=0 --reverse-arcs=0 --vsids-decay=97 --otfs=1 --del-on-restart=30 --contraction=200 --counter-restarts=7 --local-restarts --lookahead=no --save-progress=25 --counter-bump=1024";
	/**
	 * Clasp configuration - 09/13 HVHF data.
	 */
	public final static String HVHF_CONFIG_09_13 =     "--backprop --eq=0 --trans-ext=all --sat-prepro=0 --sign-def=0 --del-max=100000 --strengthen=local,1 --del-init-r=500,6000 --loops=common --init-watches=0 --heuristic=Vsids --del-cfl=F,500 --restarts=D,100,0.8,100 --del-algo=inp_sort,1 --deletion=2,75,3.0 --update-act --del-glue=4,0 --update-lbd=0 --reverse-arcs=3 --vsids-decay=96 --otfs=2 --del-on-restart=0 --contraction=500 --local-restarts --lookahead=no --save-progress=50";
	
	/**
	 * Clasp library path.
	 */
	@Parameter(names = "--library", description = "path to the clasp library.", required=true)
	public String Library;
	
	/**
	 * Clasp configuration to use.
	 */
	@UsageTextField(defaultValues="-See code-")
	@Parameter(names = "--configuration", description = "clasp configuration to use (may not be used).")
	public String Configuration = ORIGINAL_CONFIG_03_13;
	
	@Override
	public AbstractCompressedSATSolver getSATSolver() {
		return new ClaspSATSolver(Library, Configuration);
	}

}
