package ca.ubc.cs.beta.stationpacking.solvers.decorators;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.util.Pair;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATDecoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

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
		ISATEncoder aSATEncoder = new SATCompressor(fConstraintManager);
		Pair<CNF,ISATDecoder> aEncoding = aSATEncoder.encode(aInstance);
		CNF aCNF = aEncoding.getKey();
		
		//Create comments
		String[] comments = new String[]{
				"FCC Feasibility Checking Instance",
				"Instance Info: "+aInstance.getInfo(),
				"Original instance: "+aInstance.toString()
				};
		
		
		//Save instance to file.
		String aCNFFilename = fCNFDirectory+File.separator+aInstance.getHashString()+".cnf";
		try {
			FileUtils.writeStringToFile(
					new File(aCNFFilename),
					aCNF.toDIMACS(comments)
					);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException("Could not write CNF to file.");
		}
		
		return fDecoratedSolver.solve(aInstance, aTerminationCriterion, aSeed);
	}
	

	
	
}
