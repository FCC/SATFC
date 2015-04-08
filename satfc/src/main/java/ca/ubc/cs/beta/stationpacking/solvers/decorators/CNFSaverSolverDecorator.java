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
package ca.ubc.cs.beta.stationpacking.solvers.decorators;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

import com.google.common.base.Joiner;

/**
 * Solver decorator that saves CNFs on solve query. 
 * @author afrechet
 */
public class CNFSaverSolverDecorator extends ASolverDecorator
{	
	private final IConstraintManager fConstraintManager;
	private final String fCNFDirectory;

	public CNFSaverSolverDecorator(ISolver aSolver,IConstraintManager aConstraintManager, String aCNFDirectory) {
		super(aSolver);
		
		if(aConstraintManager == null)
		{
			throw new IllegalArgumentException("Constraint manager must not be null.");
		}
		
		fConstraintManager = aConstraintManager;
		
		File cnfdir = new File(aCNFDirectory);
		if(!cnfdir.exists())
		{
			throw new IllegalArgumentException("CNF directory "+aCNFDirectory+" does not exist.");
		}
		if(!cnfdir.isDirectory())
		{
			throw new IllegalArgumentException("CNF directory "+aCNFDirectory+" is not a directory.");
		}
		
		fCNFDirectory = aCNFDirectory;
	}
	
	@Override
	public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed)
	{
		//Encode instance.
        SATCompressor aSATEncoder = new SATCompressor(fConstraintManager);
		SATEncoder.CNFEncodedProblem aEncoding = aSATEncoder.encodeWithAssignment(aInstance);
		CNF aCNF = aEncoding.getCnf();
		
		//Create comments
		String[] comments = new String[]{
				"FCC Feasibility Checking Instance",
				"Instance Info: "+aInstance.getInfo(),
				"Original instance: "+aInstance.toString()
				};
		
		
		//Save instance to file.
		final String aCNFFilenameBase = fCNFDirectory+File.separator+aInstance.getHashString();
		try {
            // write cnf file
			FileUtils.writeStringToFile(
					new File(aCNFFilenameBase + ".cnf"),
					aCNF.toDIMACS(comments)
					);
            // write previous assignment file
            FileUtils.writeStringToFile(
                    new File(aCNFFilenameBase + "_assignment.txt"),
                    Joiner.on(System.lineSeparator()).join(aEncoding.getInitialAssignment().entrySet().stream()
                            .map(entry -> entry.getKey() + " " + (entry.getValue() ? 1 : 0)).collect(Collectors.toList()))
            );
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException("Could not write CNF to file.");
		}
		
		return fDecoratedSolver.solve(aInstance, aTerminationCriterion, aSeed);
	}
	

	
	
}
