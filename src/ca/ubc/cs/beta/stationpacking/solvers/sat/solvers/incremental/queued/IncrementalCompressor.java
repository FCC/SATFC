package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental.queued;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Clause;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class IncrementalCompressor 
{
	private final HashBiMap<Long,Long> fCompressionMap = HashBiMap.create();
	private long fCompressionMapMax = 1;
	private long fNextControlLiteral = -1;
	
	private final HashMap<Clause, Long> fControlVariables = new HashMap<Clause, Long>();
	private final TreeSet<Literal> fActivatedCompressedControls = new TreeSet<Literal>(new Comparator<Literal>() {
		@Override
		public int compare(Literal o1, Literal o2) {
			return Long.compare(o1.getVariable(), o2.getVariable());
		}
	});
	
	public Pointer compress(CNF cnf)
	{
		fActivatedCompressedControls.clear();

		HashSet<Clause> newClauses = new HashSet<Clause>();
		ArrayDeque<Long> newControlVariables = new ArrayDeque<Long>();
		for (Clause clause : cnf)
		{
			Long control = fControlVariables.get(clause);
			if (control == null)
			{
				// add the clause
				fControlVariables.put(clause, fNextControlLiteral);
				control = fNextControlLiteral;
				fNextControlLiteral--;
				// add the clause to new clauses
				Clause compressedClause = compressClause(clause, control);
				newClauses.add(compressedClause);
				newControlVariables.add(compressVar(control));
			}
			fActivatedCompressedControls.add(new Literal(compressVar(control), false));
			
		}
		return createEncoding(fCompressionMap.size(), fActivatedCompressedControls, newClauses, newControlVariables);
	}
	
	protected Clause compressClause(Clause clause, long control)
	{
		Clause newClause = new Clause();
		// add the compressed lits to the clause
		for (Literal lit : clause)
		{
			Long compressedVar = compressVar(lit.getVariable());
			newClause.add(new Literal(compressedVar, lit.getSign()));
		}
		// add the control variable 
		Long compressedVar = compressVar(control);
		Literal controlLit = new Literal(compressedVar, true);
		newClause.add(controlLit);
		return newClause;
	}
	
	/**
	 * Compresses the var to its given value or creates a new compression and returns it.
	 * @param var variable to compress.
	 * @return the compressed value of the variable.
	 */
	protected long compressVar(long var)
	{
		Long val = fCompressionMap.get(var);
		if (val == null)
		{
			val = fCompressionMapMax;
			fCompressionMap.put(var, val);
			fCompressionMapMax++;
		}
		return val;
	}
	
	/**
	 * Decompresses the given compressed var to its original value.
	 * @param cvar compressed var for which to obtain the decompressed value.
	 * @return the decompressed value of the compressed var.
	 */
	public long decompressVar(long cvar)
	{
		BiMap<Long,Long> aInverseCompressionMap = fCompressionMap.inverse();
		if(aInverseCompressionMap.containsKey(cvar))
		{
			return aInverseCompressionMap.get(cvar);
		}
		else
		{
			throw new IllegalArgumentException("Cannot uncompress variable "+cvar+", not in the compression map.");
		}
	}
	
	/**
	 * Format
	 * [0]: size of array
		 * [1]: total number of variables
	 * [2]: number of new clauses
	 * [3]: number of control literals of clauses that need to be solved // MUST BE INCREASING
	 * [4..(4+[2]-1)]: new control variables (1 per new clause)
	 * [(4+[2])..(4+[2]+[3]-1)]: control literals that are true // MUST BE SORTED, MUST ALL BE FALSE i.e. negated
	 * [(4+[2]+[3])..end]: new clauses separated by 0s.
	 * 
	 * or
	 * 
	 * [0]: -1 = terminate the solver.
	 * @return
	 */
	protected Pointer createEncoding(int numVars, TreeSet<Literal> assumptions, HashSet<Clause> clauses, ArrayDeque<Long> newControlVariables)
	{
		int size = getIntSize(clauses, assumptions);
		Pointer message= new Memory(size * Native.getNativeSize(Integer.TYPE));
		message.setInt(0 * Native.getNativeSize(Integer.TYPE), size);
		message.setInt(1 * Native.getNativeSize(Integer.TYPE), numVars);
		message.setInt(2 * Native.getNativeSize(Integer.TYPE), clauses.size());
		message.setInt(3 * Native.getNativeSize(Integer.TYPE), assumptions.size());

		int i = 4;
		for (long newControl : newControlVariables)
		{
			message.setInt(i * Native.getNativeSize(Integer.TYPE), (int) newControl);
			i++;
		}
		for (Literal trueControl : assumptions)
		{
			message.setInt(i * Native.getNativeSize(Integer.TYPE), (trueControl.getSign()?1:-1) * (int) trueControl.getVariable());
			i++;
		}
		for (Clause clause : clauses)
		{
			for (Literal lit : clause)
			{
				message.setInt(i * Native.getNativeSize(Integer.TYPE), (lit.getSign()?1:-1) * (int) lit.getVariable());
				i++;
			}
			message.setInt(i * Native.getNativeSize(Integer.TYPE), 0);
			i++;
		}
		return message;
	}
	
	protected int getIntSize(HashSet<Clause> clauses, TreeSet<Literal> assumptions)	
	{
		int size = 4;//size of array
		size += clauses.size();
		size += assumptions.size();
		for (Clause clause : clauses)
		{
			size += clause.size()+1; // +1 because the clause needs to be terminated by a 0.
		}
		return size;
	}
}
