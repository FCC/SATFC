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
package ca.ubc.cs.beta.stationpacking.solvers.base;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.python.google.common.base.Preconditions;

import ca.ubc.cs.beta.stationpacking.base.Station;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;


/**
 * Container object for the result of a solver executed on a problem instance.
 * @author afrechet
 *
 */
@JsonDeserialize(using = SolverResultDeserializer.class)
@EqualsAndHashCode
public class SolverResult implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private SATResult fResult;
	private double fRuntime;
	private ImmutableMap<Integer,Set<Station>> fAssignment;
    @JsonIgnore
    @Getter
    private final SolvedBy solvedBy;

    public enum SolvedBy {
        CLASP,
        MIP,
        SAT_PRESOLVER,
        UNSAT_PRESOLVER,
        CONNECTED_COMPONENTS,
        SAT_CACHE,
        UNSAT_CACHE,
        ARC_CONSISTENCY,
        CHANNEL_KILLER,
        UNKNOWN,
        UNSOLVED, 
        UNDERCONSTRAINED
    }
	
	/**
	 * @param aResult - solver result satisfiability.
	 * @param aRuntime - solver result runtime.
	 * @param aAssignment - solver result witness assignment.
	 */
	public SolverResult(SATResult aResult, double aRuntime, Map<Integer,Set<Station>> aAssignment, SolvedBy aSolvedBy)
	{
		if(aRuntime<0 && Math.abs(aRuntime)!=0.0)
		{
			throw new IllegalArgumentException("Cannot create a solver result with negative runtime (runtime = "+aRuntime+").");
		}
		
		fResult = aResult;
		fRuntime = aRuntime;
		fAssignment = ImmutableMap.copyOf(aAssignment);
        solvedBy = aResult.isConclusive() ? aSolvedBy : SolvedBy.UNSOLVED;
	}
	
	/**
	 * @param aResult - solver result satisfiability.
	 * @param aRuntime - solver result runtime.
	 */
	public static SolverResult createNonSATResult(SATResult aResult, double aRuntime, SolvedBy aSolvedBy)
	{
		Preconditions.checkArgument(!aResult.equals(SATResult.SAT), "Must provide a station assignment when creating a SAT solver result.");
        return new SolverResult(aResult, aRuntime, ImmutableMap.of(), aSolvedBy);
	}
	
	/**
	 * Create a TIMEOUT result with the given runtime.
	 * @param aRuntime - runtime
	 * @return a TIMEOUT SolverResult with the given runtime.
	 */
	public static SolverResult createTimeoutResult(double aRuntime)
	{
		return SolverResult.createNonSATResult(SATResult.TIMEOUT,aRuntime, SolvedBy.UNSOLVED);
	}
	
	public static SolverResult relabelTime(SolverResult aResult, double aTime) {
		return new SolverResult(aResult.getResult(), aTime, aResult.getAssignment(), aResult.getSolvedBy());
	}

    public static SolverResult relabelTimeAndSolvedBy(SolverResult aResult, double aTime, SolvedBy aSolvedBy) {
        return new SolverResult(aResult.getResult(), aTime, aResult.getAssignment(), aSolvedBy);
    }
    
	
	/**
	 * @return the satisfiabiltity result.
	 */
	public SATResult getResult(){
		return fResult;
	}
	
	/**
	 * @return the runtime (s). This is walltime.
	 */
	public double getRuntime()
	{
		return fRuntime;
	}
	

	/**
	 * @return the witness assignment.
	 */
	public ImmutableMap<Integer,Set<Station>> getAssignment()
	{
		return fAssignment;
	}
	
	
	@Override
	public String toString()
	{
		return fResult+","+fRuntime+","+fAssignment;//toStringAssignment(fAssignment);
	}
	
	/**
	 * @return a parseable string version of the result.
	 */
	public String toParsableString()
	{
        final StringBuilder aOutput = new StringBuilder();
		aOutput.append(fResult.toString()).append(",").append(fRuntime).append(",");
		
		Iterator<Integer> aChannelIterator = fAssignment.keySet().iterator();
		while(aChannelIterator.hasNext())
		{
			Integer aChannel = aChannelIterator.next();

            aOutput.append(aChannel).append("-");
			
			Iterator<Station> aAssignedStationIterator = fAssignment.get(aChannel).iterator();
			
			while(aAssignedStationIterator.hasNext())
			{
				Station aAssignedStation = aAssignedStationIterator.next();

                aOutput.append(aAssignedStation.getID());
				
				if(aAssignedStationIterator.hasNext())
				{
                    aOutput.append("_");
				}
				
			}
			
			if(aChannelIterator.hasNext())
			{
                aOutput.append(";");
			}
		}

		return aOutput.toString();
	}

    @JsonIgnore
    public boolean isConclusive() {
        return fResult.isConclusive();
    }

}
