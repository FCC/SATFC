/**
 * Copyright 2016, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
 *
 * This file is part of SATFC.
 *
 * SATFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SATFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

/**
 * Clasp SAT solver library parameters.
 * @author afrechet
 */
@UsageTextField(title="FCC Station Packing Packing Clasp Library Based SAT Solver Parameters",description="Parameters defining a Clasp library based SAT solver.")
public class ClaspLibSATSolverParameters extends AbstractOptions {
	
    /**
	 * Clasp3 configurations
     */
	
	/*
	 * Adapted from HVHF_CONFIG_09_13 for clasp3 (by removing parameters until it worked).
	 */
    public final static String HVHF_CONFIG_09_13_MODIFIED =     "--backprop --eq=0 --trans-ext=all --sat-prepro=0 --sign-def=0 --del-max=100000 --strengthen=local,1 --loops=common --init-watches=0 --heuristic=Vsids,96 --del-cfl=F,500 --restarts=D,100,0.8,100 --update-act --del-glue=4,0 --update-lbd=0 --reverse-arcs=3 --otfs=2 --del-on-restart=0 --contraction=500 --local-restarts --lookahead=no --save-progress=50";

    
    /*
     * First UHF clasp configuration that is intended to be run on full instances. 
     */
    public final static String UHF_CONFIG_04_15_h1 =      "--sat-prepro=0 --init-watches=0 --rand-freq=0.02 --sign-def=2 --del-init=5.0,10,2500 --strengthen=local,2 --lookahead=hybrid,1 --otfs=1 --reverse-arcs=3 --save-progress=180 --del-glue=2,0 --del-cfl=L,2000 --restarts=F,1600 --local-restarts --update-lbd=3 --heuristic=Vsids,92 --deletion=ipSort,75,2 --contraction=100 --del-grow=1.1,20.0 --del-on-restart=50 --del-max=32767";
    
    /*
     * Second UHF clasp configuration that is intended to be run on decomposed instances.
     */
    public final static String UHF_CONFIG_04_15_h2 =      "--sat-prepro=0 --init-watches=2 --rand-freq=0.0 --sign-def=2 --del-init=5.0,10,2500 --strengthen=local,2 --lookahead=hybrid,1 --otfs=2 --reverse-arcs=3 --save-progress=180 --del-glue=2,0 --del-cfl=L,2000 --restarts=F,1600 --local-restarts --update-lbd=1 --heuristic=Vsids,92 --deletion=ipSort,75,2 --contraction=166 --del-grow=0 --del-on-restart=50 --del-max=32767";

}
