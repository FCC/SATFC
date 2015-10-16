/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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
package ca.ubc.cs.beta.stationpacking.solvers.mip;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Status;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.Constraint;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult.SolvedBy;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

/**
 * A MIP based feasibility checker. Encodes a feasibility checking instance in the feasibility MIP
 * and solves the latter using CPLEX.
 * 
 * The feasibility MIP is defined as follows:
 * 
 * ================================================================================================
 * 
 *  \max 1 
 *  
 *  x_{s,c} + x_{s,c'} \leq 1 \forall c,c' \in D(s), \forall s								(1)
 *  
 *  \sum_{c \in D(s)} x_{s,c} = 1 \forall s													(2)
 * 
 *  x_{s,c} + x_{s',c'} \leq 1 \forall stations s,s' and channels c, c' in D(s) and D(s')	(3)
 *  respectively that are prohibited, that is s and s' broadcasting together on c, c' 
 *  respectively would create interference.
 *  
 *  x_{s,c} \in \{0,1\} \forall c \in D(s), \forall s										(4)
 *  
 * ================================================================================================
 *  
 *  where D(s) is the channel domain in which we want to pack station s for that particular instance.
 *  
 *  Constraints (1), (2) are base constraints to have a proper assignment of channels to stations,
 *  constraint (3) guarantees a non-interfering assignment, and channel (4) enforces that our variables
 *  are boolean.
 *  
 * @author afrechet
 */
public class MIPBasedSolver implements ISolver
{
	/*
	* Running this in Eclipse requires that the following be added to Run Configuration -> WM arguments
	* 
	* -Djava.library.path="<CPLEX directory>/cplex/bin/<architecture>"
	* 
	* so that Eclipse has all the right native libraries.
	* 
	*/
	
	private final static Logger log = LoggerFactory.getLogger(MIPBasedSolver.class);
	
	private final IConstraintManager fConstraintManager;
	
	public MIPBasedSolver(IConstraintManager aConstraintManager)
	{
		fConstraintManager = aConstraintManager;
	}
	
	/**
	 * Adds all the necessary binary variables to the given MIP, and return a map of variables added.
	 * @param aMIP - a MIP to modify.
	 * @param aInstance - a station packing instance to get variables for.
	 * @return an unmodifiable map of station to channel to variable.
	 * @throws IloException
	 */
	private static Pair<Map<Station,Map<Integer,IloIntVar>>,Map<IloIntVar,Pair<Station,Integer>>> addVariables(IloCplex aMIP, StationPackingInstance aInstance) throws IloException
	{
		final Map<Station,Map<Integer,IloIntVar>> variablesMap = new HashMap<Station,Map<Integer,IloIntVar>>();
		final Map<IloIntVar,Pair<Station,Integer>> variablesDecoder = new HashMap<IloIntVar,Pair<Station,Integer>>();
		
		final Map<Station,Set<Integer>> domains = aInstance.getDomains();
		
		for(final Entry<Station,Set<Integer>> domainsEntry : domains.entrySet())
		{
			final Station station = domainsEntry.getKey();
			final Set<Integer> domain = domainsEntry.getValue();
			
			final Map<Integer,IloIntVar> stationVariablesMap = new HashMap<Integer,IloIntVar>();
			for(final Integer channel : domain)
			{
				final IloIntVar variable = aMIP.boolVar(Integer.toString(station.getID())+":"+Integer.toString(channel));
				
				stationVariablesMap.put(channel, variable);
				variablesDecoder.put(variable, new Pair<Station,Integer>(station,channel));
			}
			variablesMap.put(station, Collections.unmodifiableMap(stationVariablesMap));
		}
		
		return new Pair<Map<Station,Map<Integer,IloIntVar>>,Map<IloIntVar,Pair<Station,Integer>>>(Collections.unmodifiableMap(variablesMap),Collections.unmodifiableMap(variablesDecoder));
	}
	
	/**
	 * Adds all the base constraints to the given MIP using the given variables.
	 * @param aMIP - a MIP to modify.
	 * @param aVariables - a map taking station to channel to variable for which base constraints are necessary.
	 * @throws IloException
	 */
	private static void addBaseConstraints(IloCplex aMIP, Map<Station,Map<Integer,IloIntVar>> aVariables) throws IloException
	{
		for(final Station station : aVariables.keySet())
		{
			final Collection<IloIntVar> stationVariables = aVariables.get(station).values();
			final IloIntVar[] stationVariablesArray = stationVariables.toArray(new IloIntVar[stationVariables.size()]);
			
			// x_{s,c} + x_{s,c'} \leq 1
			for(int i=0;i<stationVariablesArray.length;i++)
			{
				for(int j=i+1;j<stationVariablesArray.length;j++)
				{
					aMIP.addLe(aMIP.sum(stationVariablesArray[i],stationVariablesArray[j]), 1);
				}
			}
			
			// \sum_{c \in D(s)} x_{s,c} = 1
			aMIP.addEq(aMIP.sum(stationVariablesArray), 1);
		}
	}
	
	/**
	 * Adds all the interference constraints to the given MIP using the given variables.
	 * @param aMIP - a MIP to modify.
	 * @param aVariables - a map taking station to channel to variable for which base constraints are necessary.
	 * @throws IloException
	 */
	private static void addInterferenceConstraints(IloCplex aMIP, Map<Station,Map<Integer,IloIntVar>> aVariables, IConstraintManager aConstraintManager, Map<Station, Set<Integer>> domains) throws IloException
	{
        for (Constraint constraint : aConstraintManager.getAllRelevantConstraints(domains)) {
            final IloIntVar sourceVariable = aVariables.get(constraint.getSource()).get(constraint.getSourceChannel());
            final IloIntVar targetVariable = aVariables.get(constraint.getTarget()).get(constraint.getTargetChannel());
            // x_{s,c} + x_{s',c'} \leq 1
            aMIP.addLe(aMIP.sum(sourceVariable, targetVariable), 1);
        }
	}


    public static Pair<IloCplex,Map<IloIntVar,Pair<Station,Integer>>> encodeMIP(StationPackingInstance aInstance, IConstraintManager aConstraintManager) throws IloException
    {
    	IloCplex mip = new IloCplex();
    	
    	//Add constraints.
    	Pair<Map<Station,Map<Integer,IloIntVar>>,Map<IloIntVar,Pair<Station,Integer>>> variablesMaps = addVariables(mip, aInstance);
    	Map<Station,Map<Integer,IloIntVar>> variables = variablesMaps.getFirst();
    	Map<IloIntVar,Pair<Station,Integer>> decoder = variablesMaps.getSecond();
    	addBaseConstraints(mip,variables);
    	addInterferenceConstraints(mip, variables, aConstraintManager, aInstance.getDomains());
    	
    	//Add dummy objective function.
    	mip.addMaximize();
    	
    	return new Pair<IloCplex,Map<IloIntVar,Pair<Station,Integer>>>(mip,decoder);
    }
	
	
    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed)
    {
    	Watch watch = new Watch();
    	watch.start();
    	
    	final IloCplex mip;
    	final Map<IloIntVar, Pair<Station,Integer>> decoder;
		try {
			log.debug("Encoding station packing instance to MIP...");
			Pair<IloCplex,Map<IloIntVar,Pair<Station,Integer>>> encoding = encodeMIP(aInstance, fConstraintManager);
			mip = encoding.getFirst();
			decoder = encoding.getSecond();
			
		} catch (IloException e) {
			e.printStackTrace();
			log.error("Could not encode station packing instance to MIP.",e);
			throw new IllegalStateException("Could not encode station packing instance to MIP ("+e.getMessage()+").");
		}
		log.debug("Encoding MIP took {} s.",watch.getElapsedTime());
		log.debug("MIP has {} variables.",mip.getNcols());
		log.debug("MIP has {} constraints.",mip.getNrows());
		
    
    	// This turns off CPLEX logging.
    	mip.setOut(new NullOutputStream());
        
    	//Set CPLEX's parameters.
    	
    	double cutoff = aTerminationCriterion.getRemainingTime();
    	if(cutoff <= 0 )
    	{
    		log.debug("Already have spent all the allocated time.");
    		return SolverResult.createTimeoutResult(watch.getElapsedTime());
    	}
    	
    	try {
			mip.setParam(IloCplex.DoubleParam.TimeLimit, cutoff);
			mip.setParam(IloCplex.LongParam.RandomSeed, (int) aSeed);
		} catch (IloException e) {
			e.printStackTrace();
			log.error("Could not set CPLEX's parameters to the desired values",e);
			throw new IllegalStateException("Could not set CPLEX's parameters to the desired values ("+e.getMessage()+").");
		}
    	
    	//Solve the MIP.
    	final boolean feasible;
    	try {
			feasible = mip.solve();
		} catch (IloException e) {
			e.printStackTrace();
			log.error("CPLEX could not solve the MIP.",e);
			throw new IllegalStateException("CPLEX could not solve the MIP ("+e.getMessage()+").");
		}
    	
    	//Gather output
    	final double runtime = watch.getElapsedTime();
    	final SATResult satisfiability;
    	final Map<Integer,Set<Station>> assignment;
    	
    	final Status status;
		try {
			 status = mip.getStatus();
		} catch (IloException e) {
			e.printStackTrace();
			log.error("Could not get CPLEX status post-execution.",e);
			throw new IllegalStateException ("Could not get CPLEX status post-execution ("+e.getMessage()+").");
		}
    	
		if(status.equals(Status.Optimal))
		{
			if(feasible)
			{
				satisfiability = SATResult.SAT;
				assignment = getAssignment(mip, decoder);
			}
			else
			{
				satisfiability = SATResult.UNSAT;
				assignment = null;
			}
		}
		else if(status.equals(Status.Feasible))
		{
			satisfiability = SATResult.SAT;
			//Parse the assignment.
			assignment = getAssignment(mip, decoder);
			
		}
		else if(status.equals(Status.Infeasible))
		{
			satisfiability = SATResult.UNSAT;
			assignment = null;
		}
		else if(status.equals(Status.Unknown) && aTerminationCriterion.hasToStop())
		{
			satisfiability = SATResult.TIMEOUT;
			assignment = null;
		}
		else
		{
			log.error("CPLEX has a bad post-execution status.");
			log.error(status.toString());
			satisfiability = SATResult.CRASHED;
			assignment = null;
		}
    	
    	//Wrap up.
    	mip.end();
    	
    	if(assignment == null)
    	{
    		return SolverResult.createNonSATResult(satisfiability, runtime, SolvedBy.MIP);
    	}
    	else
    	{
    		return new SolverResult(satisfiability, runtime, assignment, SolvedBy.MIP);
    	}
    }
    
    
    /**
     * @param aMIP - a (solved) station packing MIP.
     * @param aDecoder - a decoder of MIP variables to station channel pairs.
     * @return the station packing assignment contained in the solved MIP. 
     */
	private static Map<Integer, Set<Station>> getAssignment(final IloCplex aMIP, final Map<IloIntVar, Pair<Station, Integer>> aDecoder) {
		
		final Map<Integer, Set<Station>> assignment = new HashMap<Integer,Set<Station>>();
		for(Entry<IloIntVar,Pair<Station,Integer>> entryDecoder : aDecoder.entrySet())
		{
			final IloIntVar variable = entryDecoder.getKey();
				try {
					if(aMIP.getValue(variable) == 1)
					{
						final Pair<Station,Integer> stationChannelPair = entryDecoder.getValue();
						final Station station = stationChannelPair.getFirst();
						final Integer channel = stationChannelPair.getSecond();
						
						if(!assignment.containsKey(channel))
						{
							assignment.put(channel, new HashSet<Station>());
						}
						assignment.get(channel).add(station);
					}
				} catch (IloException e) {
					e.printStackTrace();
					log.error("Could not get MIP value assignment for variable "+variable+".",e);
					throw new IllegalStateException("Could not get MIP value assignment for variable "+ variable +" ("+e.getMessage()+").");
				}
		}
		return assignment;
	}

    @Override
    public void notifyShutdown()
    {
    	
    }

	@Override
	public void interrupt() {
		// TODO Auto-generated method stub
		
	}
    
}
