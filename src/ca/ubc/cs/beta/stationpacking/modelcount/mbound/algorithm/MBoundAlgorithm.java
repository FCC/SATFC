package ca.ubc.cs.beta.stationpacking.modelcount.mbound.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import ca.ubc.cs.beta.stationpacking.modelcount.mbound.base.MBoundResult;
import ca.ubc.cs.beta.stationpacking.modelcount.mbound.base.MBoundResult.MBoundResultType;
import ca.ubc.cs.beta.stationpacking.modelcount.mbound.parameters.MBoundParameters;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Clause;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ISATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * Implements the MBound algorithm as specified in the paper "Model Counting: A New Strategy for Obtaining Good Bounds"
 * by Carla P. Gomes and Ashish Sabharwal and Bart Selman. 
 * 
 * @author tqichen
 *
 */
public class MBoundAlgorithm {

    /**
     * Solve the CNF with additional randomly generated parity constraints. This is done in trials. 
     * Return an upper bound or lower bound depending on the number of SAT trials.  
     * 
     * @param aMBoundParameters
     * @param aCNF
     * @param aSATSolver a SAT solver used for solving CNFs.
     * @param aTerminationCriterion termination criteria for the SAT solver
     * @param aSeed seed for the SAT solver
     * @return
     */
    public static MBoundResult solve(MBoundParameters aMBoundParameters, CNF aCNF, ISATSolver aSATSolver, ITerminationCriterion aTerminationCriterion, Long aSeed) {

        int k = aMBoundParameters.getXorClauseSize();
        int s = aMBoundParameters.getNumXorClauses();
        int t = aMBoundParameters.getNumTrials();
        double deviation = aMBoundParameters.getDeviation();
        double slack = aMBoundParameters.getPrecisionSlack();

        if (k > aCNF.getVariables().size()) {
            throw new IllegalArgumentException("Size of generated XOR clauses cannot be larger than the number of variables ("+aCNF.getVariables().size()+").");
        }
        
        int numSat = 0;

        // Perform t trails. TODO: do in parallel
        for (int i=0; i<t; i++) {
            CNF cnfWithParityConstraints = addRandomParityConstraints(aCNF, s, k);
            SATSolverResult result = aSATSolver.solve(cnfWithParityConstraints, aTerminationCriterion, aSeed);

            if (result.getResult().equals(SATResult.SAT)) {
                numSat++;
            }
        }

        // Return lower/upper bound or failure.
        if (numSat >= t * (0.5+deviation)) {
            return new MBoundResult(MBoundResultType.LOWERBOUND, Math.pow(2, s-slack), calculateMaximumErrorProbability(aMBoundParameters));
        } else if (numSat <= t * (0.5-deviation)) {
            return new MBoundResult(MBoundResultType.UPPERBOUND, Math.pow(2, s+slack), calculateMaximumErrorProbability(aMBoundParameters));
        } else {
            return new MBoundResult(MBoundResultType.FAILURE, null, null);
        }
    }
    
    /**
     * Calculates the probability that, if a bound be returned by MBound, the bound is incorrect.
     * @param aMBoundParameters
     * @return
     */
    public static Double calculateMaximumErrorProbability(MBoundParameters aMBoundParameters) {

        int t = aMBoundParameters.getNumTrials();
        double deviation = aMBoundParameters.getDeviation();
        double slack = aMBoundParameters.getPrecisionSlack();
        
        if (deviation == 0.5) {
            
            return Math.pow(2, - slack * t);
            
        } else {

            Double b = (Math.pow(2,slack) * (0.5 + deviation)) - 1; 
            return Math.pow(Math.exp(b) / Math.pow(1+b, 1+b), t / Math.pow(2,b)); 
            
        }
    }

    /**
     * Appends randomly generated parity (XOR clauses) constraints onto an existing CNF. 
     * Parity constraints are generated from a uniform random sample of the subsets of 
     * variables in the CNF with the given size. 
     * 
     * @param cnf a CNF
     * @param numConstraints number of parity constraints to add to the CNF
     * @param xorClauseSize size of the parity constraints. 
     * @return a new CNF with additional parity constraints
     */
    private static CNF addRandomParityConstraints(CNF cnf, Integer numConstraints, Integer xorClauseSize) {

        List<Long> variables = new ArrayList<Long>(cnf.getVariables());

        // Create an always-true variable.
        Long alwaysTrueVariable = Collections.max(variables) + 1;
        variables.add(alwaysTrueVariable);

        Clause alwaysTrueVariableClause = new Clause();
        alwaysTrueVariableClause.add(new Literal(alwaysTrueVariable, true));

        if (xorClauseSize > variables.size()) {
            throw new IllegalArgumentException("XOR clause size cannot be more than the number of variables.");
        }

        Collection<Clause> additionalNewClauses = new HashSet<Clause>();

        // Add the always-true variable as a clause.
        additionalNewClauses.add(alwaysTrueVariableClause);

        for (int i=0; i<numConstraints; i++) {

            List<Long> randomlySampledSubset = new ArrayList<Long>();
            
            // Uniformly sample from subsets of size k by first sorting the variables and taking the first k variables.
            Collections.shuffle(variables);
            for (int j=0; j<xorClauseSize; j++) {
                randomlySampledSubset.add(variables.get(j));
            }

            Collection<Clause> newDisjunctionClauses = convertXorClauseToCNF(randomlySampledSubset);
            additionalNewClauses.addAll(newDisjunctionClauses);

        }

        CNF newCNF = new CNF();
        newCNF.addAll(cnf);
        newCNF.addAll(additionalNewClauses);

        return newCNF;

    }

    /**
     * Converts a clause of the form (a xor b xor ...) to CNF.
     * @param xorVariables variables within the xor clause.
     * @return set of disjunctive clauses in the CNF
     */
    private static Collection<Clause> convertXorClauseToCNF(List<Long> xorVariables) {
        if (xorVariables.isEmpty()) {
            
            return Collections.<Clause>emptyList();
            
        } else {
            
            Map<Long, Literal> posVars = new HashMap<Long, Literal>(xorVariables.size());
            Map<Long, Literal> negVars = new HashMap<Long, Literal>(xorVariables.size());
            
            for (Long var : xorVariables) {
                posVars.put(var, new Literal(var, true));
                negVars.put(var, new Literal(var, false));
            }
            
            // Generate CNF clauses. Each clause should have an even number of negations.
            // Here, we generate binary strings with even number of 1s. A 1 will indicate a negation.
            
            String[] binaryStrings = generateBinaryStringsWithEvenNumberOfOnes(xorVariables.size());
            
            Collection<Clause> disjunctionClauses = new HashSet<Clause>();

            for (String binary : binaryStrings) {
                
                Clause clause = new Clause();
                
                for(int i=0, n=binary.length() ; i<n ; i++) { 
                    
                    char inclusion = binary.charAt(i); 
                    Long var = xorVariables.get(i);
                    
                    if (inclusion == '1') {
                        
                        // negate
                        clause.add(negVars.get(var));
                        
                    } else {
                        
                        clause.add(posVars.get(var));
                        
                    }
                }
                
                disjunctionClauses.add(clause);
                
            }
            
            return disjunctionClauses;
        }
    }
    
    /**
     * Generates binary strings with even number of ones, up the given length.
     */
    private static String[] generateBinaryStringsWithEvenNumberOfOnes(int length) {
        
        int n = (int) Math.pow(2,length-1);
        
        String[] ret = new String[n];
        
        for (int i=0; i<n ; i++) {
            
            char digit = Integer.bitCount(i) % 2 == 0 ? '0' : '1';
            
            String binaryString = String.format("%0" +length+ "d", Integer.parseInt(Integer.toBinaryString(i) + digit));
            
            ret[i] = binaryString;
        }
        
        return ret;
    }

}
