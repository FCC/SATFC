package ca.ubc.cs.beta.stationpacking.modelcount.mbound.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

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

import com.google.common.collect.Lists;

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

        // Parse parameters
        int k = aMBoundParameters.getXorClauseSize();
        int s = aMBoundParameters.getNumXorClauses();
        int t = aMBoundParameters.getNumTrials();
        double deviation = aMBoundParameters.getDeviation();
        double slack = aMBoundParameters.getPrecisionSlack();

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
            return new MBoundResult(MBoundResultType.LOWERBOUND, Math.pow(2, s-slack));
        } else if (numSat <= t * (0.5 - deviation)) {
            return new MBoundResult(MBoundResultType.UPPERBOUND, Math.pow(2, s+slack));
        } else {
            return new MBoundResult(MBoundResultType.FAILURE, null);
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

            // Uniformly sample from subsets of size k by first sorting the variables and taking the first k variables.
            Collections.shuffle(variables);

            List<Long> randomlySampledSubset = new ArrayList<Long>();
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
            
        } else if (xorVariables.size() == 1) {
            
            Clause singleClause = new Clause();
            singleClause.add(new Literal(xorVariables.get(1), true));
            
            return Lists.<Clause>newArrayList(singleClause);
            
        } else if (xorVariables.size() == 2) {

            // Create the CNF (a v b) ^ (~a v ~b)
            
            Long var1 = xorVariables.get(1);
            Long var2 = xorVariables.get(2);
            
            List<Clause> baseClause = new ArrayList<Clause>(2);
            
            Clause negClause = new Clause();
            negClause.add(new Literal(var1, false));
            negClause.add(new Literal(var2, false));
            
            Clause posClause = new Clause();
            posClause.add(new Literal(var1, true));
            posClause.add(new Literal(var2, true));
            
            baseClause.add(negClause);
            baseClause.add(posClause);
            
            return baseClause;
            
        } else {
            
            // Store a variable, cut down the problem size and recurse.
            
            List<Long> copyVariables = new ArrayList<Long>(xorVariables);
            Long lastVariable = copyVariables.remove(copyVariables.size()-1);
            
            Collection<Clause> firstClauses = convertXorClauseToCNF(copyVariables);
            
            // For each clause, append either the last variable or the negated last variable.
            // If appending the negated last variable, also negate one variable within the clause.
            
            Collection<Clause> retClauses = new HashSet<Clause>();
            
            Literal posLiteral = new Literal(lastVariable, true);
            Literal negLiteral = new Literal(lastVariable, false);
            
            for (Clause clause : firstClauses) {
                Clause appendedClause = new Clause();
                appendedClause.addAll(clause);
                appendedClause.add(posLiteral);
                
                retClauses.add(appendedClause);
            }
            
            for (Clause clause : firstClauses) {
                
                for (Literal literal : clause) {
                    
                    Clause negatedClause = new Clause();
                    negatedClause.addAll(clause);
                    negatedClause.remove(literal);
                    negatedClause.add(new Literal(literal.getVariable(), !literal.getSign()));
                    
                    negatedClause.add(negLiteral);
                    
                    retClauses.add(negatedClause);
                }
                
            }
            
            return retClauses;
            
        }
    }
}
