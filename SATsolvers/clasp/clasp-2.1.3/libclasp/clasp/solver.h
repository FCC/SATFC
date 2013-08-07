// 
// Copyright (c) 2006-2012, Benjamin Kaufmann
// 
// This file is part of Clasp. See http://www.cs.uni-potsdam.de/clasp/ 
// 
// Clasp is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
// 
// Clasp is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with Clasp; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
//
#ifndef CLASP_SOLVER_H_INCLUDED
#define CLASP_SOLVER_H_INCLUDED
#ifdef _MSC_VER
#pragma once
#pragma warning (disable : 4996) // 'std::copy': Function call with parameters that may be unsafe
#endif

#include <clasp/solver_types.h>
#include <clasp/solver_strategies.h>
#include <clasp/shared_context.h>

namespace Clasp { 

//! Parameter-Object for controlling search limits.
struct SearchLimits {
	explicit SearchLimits(uint64 conf = UINT64_MAX) 
		: conflicts(conf)
		, local(UINT64_MAX)
		, learnts(UINT32_MAX)
		, xLbd(0.0f)
		, xCfl(0.0f) {
	}
	bool   reached(const SolveStats& st) const { 
		return conflicts == 0 || local == 0 || dynamicRestart(st);
	}
	bool   dynamicRestart(const SolveStats& st) const {
		return st.queue && st.queue->full() 
			&&   (((st.queue->avgLbd()*xLbd) > st.avgLbd()) || ((st.queue->avgCfl()*xCfl) > st.avgCfl()));
	}
	uint64 conflicts; /*!< Search for at least number of conflicts.             */
	uint64 local;     /*!< Search for at least number of conflicts in branch.   */
	uint32 learnts;   /*!< Limit on number of learnt lemmas.                    */
	float  xLbd;      /*!< Adjustment factor for dynamic restarts based on LBD. */
	float  xCfl;      /*!< Adjustment factor for dynamic restarts based on CFL. */
};

/**
 * \defgroup solver Solver and related classes.
 */
//@{

//! clasp's Solver class.
/*!
 * A Solver-object maintains the state and provides the functions 
 * necessary to implement our CDNL-algorithm. 
 *
 * The interface supports incremental solving (search under assumptions) as well as
 * solution enumeration. To make this possible the solver maintains two special  
 * decision levels: the root-level and the backtrack-level.
 *
 * The root-level is the lowest decision level to which a search
 * can return. Conflicts on the root-level are non-resolvable and end a search - the
 * root-level therefore acts as an artificial top-level during search.
 * Incremental solving is then implemented by first adding a list of unit assumptions
 * and second initializing the root-level to the current decision level.
 * Once search terminates assumptions can be undone by calling clearAssumptions
 * and a new a search can be started using different assumptions.
 *
 * For model enumeration the solver maintains a backtrack-level which restricts
 * backjumping in order to prevent repeating already enumerated solutions.
 * The solver will never backjump above that level and conflicts on the backtrack-level 
 * are resolved by backtracking, i.e. flipping the corresponding decision literal.
 *
 * \see "Conflict-Driven Answer Set Enumeration" for a detailed description of this approach. 
 *
 */
class Solver {
	friend class SharedContext;
	void startInit(SharedContext* ctx, uint32 constraintGuess);
	bool endInit();
	void setEnumerationConstraint(Constraint* c) {
		if (enum_) enum_->destroy(this, true);
		enum_ = c;
	}
public:
	typedef PodVector<Constraint*>::type      ConstraintDB;
	typedef std::auto_ptr<DecisionHeuristic>  Heuristic;
	/*!
	 * \name Construction/Destruction/Setup
	 */
	//@{
	//! Creates an empty solver object with all strategies set to their default value.
	Solver();
	
	//! Destroys the solver object and all contained constraints.
	~Solver();
	
	//! Sets the solver id - default is 0.
	/*!
	 * Setting an id is mainly useful in parallel solving.
	 */
	void   setId(uint32 id) { id_ = id; }
	uint32 id() const       { return id_; }
	//! Used to signal an error in this solver.
	static const   uint32 invalidId = UINT32_MAX;
	
	//! Resets a solver object to the state it had after default-construction.
	void reset();

	//! Shuffle constraints upon next simplification.
	void shuffleOnNextSimplify() { shuffle_ = true; }
	
	//! Sets the decision heuristic to use in this object.
	void setHeuristic(uint32 heuId, DecisionHeuristic* h) {
		strategies().heuId = heuId;
		heuristic_.reset(h);
	}
	
	//! Returns the strategies used by this solver-object. 
	SolverStrategies& strategies() { return strategy_; }
	Heuristic&        heuristic()  { return heuristic_;}

	/*!
	 * \overload SolverStrategies& Solver::strategies()
	 */
	const SolverStrategies& strategies() const { return strategy_; }
	const Heuristic&        heuristic()  const { return heuristic_;}

	//! Returns a pointer to the shared context object of this solver.
	const SharedContext* sharedContext() const { return shared_; }

	//! Returns a pointer to the sat-preprocessor used by this solver.
	SatPreprocessor* satPrepro() const;
	
	//! Adds the unary constraint p to the solver.
	/*!
	 * \note Unary constraints are immediately asserted.
	 * \return false if asserting p leads to a conflict.
	 */
	bool addUnary(Literal p, ConstraintType);	
	//! Adds the short clause cl to the solver.
	/*! 
	 * \pre  cl is either binary or ternary (sz > 1 && sz < 4)
	 * \note cl is stored in the short implication graph unless
	 *       updates of this graph are not permitted.
	 * \see  SharedContext::allowImplicit(ConstraintType)
	 */
	void addShort(const Literal* cl, uint32 sz, const ClauseInfo& extra);
	//! Adds the binary clause [p q] by calling addShort.
	void addBinary(Literal p, Literal q, const ClauseInfo& extra) { Literal x[2] = {p,q}; addShort(x, 2, extra); }
	//! Adds the ternary clause [p q r] by calling addShort.
	void addTernary(Literal p, Literal q, Literal r, const ClauseInfo& extra) { Literal x[3] = {p,q,r}; addShort(x, 3, extra); }
	
	//! Adds the problem constraint c to the solver.
	void add(Constraint* c);

	//! Adds p as post propagator to this solver.
	/*!
	 * \pre p was not added previously and is not part of any other solver.
	 * \note Post propagators are stored and called in priority order.
	 * \see PostPropagator::priority()
	 */
	void addPost(PostPropagator* p)    { post_.add(p); }

	//! Removes p from the solver's list of post propagators.
	/*!
	 * \note removePost(p) shall only be called during propagation
	 *       of p or if no propagation is currently active.
	 */
	void removePost(PostPropagator* p) { post_.remove(p); }
	
	/*!
	 * \name CDNL-functions.
	 */
	//@{
	//! Searches for a model as long as the given limit is not reached.
	/*!
	 * The search function implements the CDNL-algorithm.
	 * It searches for a model as long as none of the limits given by limit
	 * is reached. The limits are updated during search.
	 *
	 * \param limit Imposed limit on conflicts and number of learnt constraints.
	 * \param randf Pick next decision variable randomly with a probability of randf.
	 * \return
	 *  - value_true: if a model was found.
	 *  - value_false: if the problem is unsatisfiable (under assumptions, if any).
	 *  - value_free: if search was stopped because limit was reached. 
	 *  .
	 *
	 * \note search treats the root level as top-level, i.e. it will never backtrack below that level.
	 */
	ValueRep search(SearchLimits& limit, double randf = 0.0);
	ValueRep search(uint64 maxC, uint32 maxL, bool local = false, double rp  = 0.0) {
		SearchLimits limit;
		if (!local) { limit.conflicts = maxC; }
		else        { limit.conflicts = UINT64_MAX; limit.local = maxC; }
		limit.learnts = maxL;
		return search(limit, rp);
	}

	//! Moves the root-level i levels down (i.e. away from the top-level).
	/*!
	 * The root-level is similar to the top-level in that it cannot be
	 * undone during search, i.e. the solver will not resolve conflicts that are on or
	 * above the root-level. 
	 */
	void pushRootLevel(uint32 i = 1) { 
		rootLevel_  = std::min(decisionLevel(), rootLevel_+i); 
		btLevel_    = std::max(btLevel_, rootLevel_);
	}
	//! Returns the current root level.
	uint32 rootLevel() const { return rootLevel_; }

	//! Moves the root-level i levels up (i.e. towards the top-level).
	/*!
	 * The function undos all levels between the new root level and the current decision level,
	 * resets the current backtrack-level, and re-assigns any implied literals.
	 * \param i Number of levels to pop.
	 * \post decisionLevel() == rootLevel()
	 * \note The function first calls clearStopConflict() to remove any stop conflicts.
	 * \note The function *does not* propagate any asserted literals. It is
	 *       the caller's responsibility to call propagate() after the function returned.
	 */
	bool popRootLevel(uint32 i = 1);

	//! Removes a previously set stop conflict and restores the root level.
	void clearStopConflict();

	//! Removes any implications made between the top-level and the root-level.
	/*!
	 * The function also resets the current backtrack-level and re-assigns learnt facts.
	 * \note
	 *   Equivalent to popRootLevel(rootLevel()) followed by simplify().
	 */
	bool clearAssumptions();

	//! Sets the backtracking level to dl.
	void setBacktrackLevel(uint32 dl) {
		btLevel_ = std::max(std::min(dl, decisionLevel()), rootLevel_);
	}
	//! Returns the current backtracking level.
	uint32 backtrackLevel() const { return btLevel_; }

	//! Returns whether the solver can split-off work.
	bool splittable() const { return decisionLevel() > rootLevel() && !frozenLevel(rootLevel()+1); }

	//! Copies the solver's currrent guiding path to gp.
	/*!
	 * \note The solver's guiding path consists of:
	 *   - the decisions from levels [1, rootLevel()]
	 *   - any literals that are implied on a level <= rootLevel() because of newly learnt
	 *     information. This particularly includes literals that were flipped during model enumeration.
	 * 
	 * \param[in,out] gp          where to store the guiding path
	 * \param[in,out] startPos    position in the trail from which copying should start
	 * \param[in,out] numImplied  number of newly implied literals in gp
	 */
	void updateGuidingPath(LitVec& gp, LitVec::size_type& startPos, uint32& numImplied);

	//! If called on top-level, removes SAT-clauses + Constraints for which Constraint::simplify returned true.
	/*!
	 * \note If this method is called on a decision-level > 0, it is a noop and will
	 * simply return true.
	 * \return false, if a top-level conflict is detected. Otherwise, true.
	 */
	bool simplify();
	void simplifyDB(ConstraintDB& db);

	//! Removes all conditional knowledge, i.e. all previously tagged learnt clauses.
	/*!
	 * \see SharedContext::requestTagLiteral()
	 */
	void removeConditional();

	//! Resolves all tagged clauses with the tag literal and thereby strengthens the learnt db.
	/*!
	 * \see SharedContext::requestTagLiteral()
	 */
	void strengthenConditional();

	//! Sets the literal p to true and schedules p for propagation.
	/*!
	 * Setting a literal p to true means assigning the appropriate value to
	 * p's variable. That is: value_false if p is a negative literal and value_true
	 * if p is a positive literal.
	 * \param p The literal that should become true.
	 * \param a The reason for the literal to become true or 0 if no reason exists.
	 * 
	 * \return
	 *  - false if p is already false
	 *  - otherwise true.
	 *
	 * \pre hasConflict() == false
	 * \pre a.isNull() == false || decisionLevel() <= rootLevel() || SearchStrategy == no_learning
	 * \post
	 *  p.var() == trueValue(p) || p.var() == falseValue(p) && hasConflict() == true
	 *
	 * \note if setting p to true leads to a conflict, the nogood that caused the
	 * conflict can be requested using the conflict() function.
	 */
	bool force(const Literal& p, const Antecedent& a) {
		assert((!hasConflict() || isTrue(p)) && !shared_->eliminated(p.var()));
		if (assign_.assign(p, decisionLevel(), a)) return true;
		setConflict(p, a, UINT32_MAX);
		return false;
	}
	/*!
	 * \overload bool Solver::force(const Literal&, const Antecedent&)
	 * \pre data == UINT32_MAX || SharedContext::requestData(p.var()) was called during setup
	 */
	bool force(const Literal& p, const Antecedent& a, uint32 data) {
		return data != UINT32_MAX 
			? assign_.assign(p, decisionLevel(), a.constraint(), data) || (setConflict(p, a, data), false)
			: force(p, a);
	}

	//! Assigns p at dl because of r.
	/*!
	 * \pre dl <= decisionLevel()
	 * \note 
	 *   If dl < ul = max(rootLevel(), backtrackLevel()), p is actually assigned
	 *   at ul but the solver stores enough information to reassign 
	 *   p on backtracking.
	 */
	bool force(Literal p, uint32 dl, const Antecedent& r, uint32 d = UINT32_MAX) {
		// Correct level?
		if (dl == decisionLevel())      { return force(p, r, d); } 
		ImpliedLiteral* x = 0;
		// Already implied?
		if (isTrue(p) && (level(p.var()) <= dl || (x = impliedLits_.find(p)) != 0)) {
			if (level(p.var()) <= dl)     { return true; }
			if (x->level       <= dl)     { return true; }
			*x = ImpliedLiteral(p, dl, r, d);
			return setReason(p, r, d); // r is stronger
		}
		// Can we return to correct level?
		if (undoUntil(dl, false) == dl) { return force(p, r, d); }
		// Logically the implication is on level dl.
		// Store enough information so that p can be re-assigned once we backtrack.
		assert(!x);
		impliedLits_.add(decisionLevel(), ImpliedLiteral(p, dl, r, d));
		return (isTrue(p) && setReason(p, r, d)) || force(p, r, d);
	}

	//! Assumes the literal p if possible.
	/*!
	 * If p is currently unassigned, sets p to true and starts a new decision level.
	 * \pre validVar(p.var()) == true
	 * \param p The literal to assume.
	 * \return !isFalse(p)
	 */
	bool assume(const Literal& p);

	//! Selects and assumes the next branching literal by calling the installed decision heuristic.
	/*!
	 * \pre queueSize() == 0
	 * \note The next decision literal will be selected randomly with probability f.
	 * \return 
	 *  - true if the assignment is not total and a literal was assumed (or forced).
	 *  - false otherwise
	 *  .
	 * \see DecisionHeuristic
	 */
	bool decideNextBranch(double f = 0.0);

	//! Sets a conflict that forces the solver to terminate its search.
	/*!
	 * \pre  !hasConflict()
	 * \post hasConflict()
	 *
	 * \note 
	 *   To prevent the solver from resolving the stop conflict, the
	 *   function sets the root level to the current decision level. 
	 *   Call clearStopConflict() to remove the conflict and to restore
	 *   the previous root-level.
	 */
	void setStopConflict();

	/*!
	 * Propagates all enqueued literals. If a conflict arises during propagation
	 * propagate returns false and the current conflict (as a set of literals)
	 * is stored in the solver's conflict variable.
	 * \pre !hasConflict()
	 * \see Solver::force
	 * \see Solver::assume
	 * \note Shall not be called recursively.
	 */
	bool propagate();

	/*!
	 * Does unit propagation and calls x->propagateFixpoint(*this)
	 * for all post propagators up to but not including p.
	 * \note The function is meant to be called only in the context of p.
	 */
	bool propagateUntil(PostPropagator* p) { return unitPropagate() && (p == post_.head || post_.propagate(*this, p)); }

	//! Executes a one-step lookahead on p.
	/*!
	 * Assumes p and propagates this assumption. If propagations leads to
	 * a conflict, false is returned. Otherwise the assumption is undone and 
	 * the function returns true.
	 * \param p The literal to test.
	 * \param c The constraint that wants to test p (can be 0).
	 * \pre p is free
	 * \note If c is not null and testing p does not lead to a conflict, 
	 *       c->undoLevel() is called *before* p is undone. Hence, the
	 *       range [s.levelStart(s.decisionLevel()), s.assignment().size())
	 *       contains p followed by all literals that were forced because of p.
	 * \note During propagation of p, only post propagators with priority
	 *       higher than that of c are called.
	 */
	bool test(Literal p, PostPropagator* c);

	//! Estimates the number of assignments following from setting p to true.
	/*!
	 * \note For the estimate only binary clauses are considered.
	 */ 
	uint32 estimateBCP(const Literal& p, int maxRecursionDepth = 5) const;

	//! Computes the number of in-edges for each assigned literal.
	/*!
	 * \pre  !hasConflict()
	 * \note For a literal p assigned on level level(p), only in-edges from
	 *       levels < level(p) are counted.
	 * \return The maximum number of in-edges.
	 */
	uint32 inDegree(WeightLitVec& out);
	
	//! Adds the learnt constraint c to the solver.
	/*!
	 * \pre endInit() was called.
	 */
	void addLearnt(LearntConstraint* c, uint32 size, ConstraintType type) {
		learnts_.push_back(c);
		stats.addLearnt(size, type); 
	}
	void addLearnt(LearntConstraint* c, uint32 size) { addLearnt(c, size, c->type()); }
	
	//! Removes upto remMax percent of the learnt nogoods.
	/*!
	 * \param remMax  Fraction of nogoods to remove ([0.0,1.0]).
	 * \param rs      Strategy to apply during nogood deletion.
	 * \return        The number of locked and active/glue clauses currently exempt from deletion.
	 * \note 
	 *   Nogoods that are the reason for a literal to be in the assignment
	 *   are said to be locked and won't be removed.
	 */
	struct DBInfo { uint32 size; uint32 locked; uint32 pinned; };
	DBInfo reduceLearnts(float remMax, const ReduceStrategy& rs = ReduceStrategy());

	//! Resolves the active conflict using the selected strategy.
	/*!
	 * If the SearchStrategy is set to learning, resolveConflict implements
	 * First-UIP learning and backjumping. Otherwise, it simply applies
	 * chronological backtracking.
	 * \pre hasConflict()
	 * \return
	 *  - true if the conflict was successfully resolved
	 *  - false otherwise
	 * \note
	 *  If decisionLevel() == rootLevel() false is returned.
	 */
	bool resolveConflict();

	//! Backtracks the last decision and sets the backtrack-level to the resulting decision level.
	/*!
	 * \return
	 *  - true if backtracking was possible
	 *  - false if decisionLevel() == rootLevel()
	 */
	bool backtrack();

	//! Undoes all assignments up to (but not including) decision level dl.
	/*!
	 * \pre dl > 0 (assignments made on decision level 0 cannot be undone)
	 * \post decisionLevel == max(min(decisionLevel(), dl), max(rootLevel(), btLevel))
	 */
	void undoUntil(uint32 dl);

	/*!
	 * Similar to undoUntil but optionally also pops the backtrack-level
	 * to dl if possible.
	 * \return The current decision level.
	 */
	uint32 undoUntil(uint32 dl, bool popBtLevel);

	//! Checks whether there is a model that is symmetric to the current model.
	/*!
	 * The function checks for symmetric models, i.e. models that differ only in the 
	 * assignment of variables outside of the solver's assignment. 
	 * Typical example: vars eliminated by the SAT-preprocessor
	 * \pre the current assignment is a model
	 */ 
	bool nextSymModel();
	//@}  
	
	/*!
	 * \name state inspection
	 * Functions for inspecting the state of the solver & search.
	 * \note validVar(v) is a precondition for all functions that take a variable as 
	 * parameter.
	 */
	//@{
	
	//! Returns the number of problem variables.
	/*!
	 * \see SharedContext::numVars()
	 */
	uint32   numVars()              const { return assign_.numVars() - 1; }
	//! Returns true if var represents a valid variable in this solver.
	/*!
	 * \see SharedContext::validVar(Var v)
	 */
	bool     validVar(Var var)      const { return var <= numVars(); }
	//! Returns the number of assigned problem variables.
	uint32   numAssignedVars()      const { return assign_.assigned(); }
	//! Returns the number of free variables.
	/*!
	 * The number of free variables is the number of vars that are neither
	 * assigned nor eliminated.
	 */
	uint32   numFreeVars()          const { return assign_.free()-1; }
	//! Returns the value of v w.r.t the current assignment.
	ValueRep value(Var v)           const { assert(validVar(v)); return assign_.value(v); }
	//! Returns the previous value of v.
	ValueRep savedValue(Var v)      const { assert(validVar(v)); return assign_.saved(v); }
	//! Returns the preferred value of v.
	ValueRep prefValue(Var v)       const { assert(validVar(v)); return assign_.pref(v); }
	//! Returns true if p is true w.r.t the current assignment.
	bool     isTrue(Literal p)      const { assert(validVar(p.var())); return assign_.value(p.var()) == trueValue(p); }
	//! Returns true if p is false w.r.t the current assignment.
	bool     isFalse(Literal p)     const { assert(validVar(p.var())); return assign_.value(p.var()) == falseValue(p); }
	//! Returns the literal of v being true in the current assignment.
	/*!
	 * \pre v is assigned a value in the current assignment
	 */
	Literal  trueLit(Var v)         const { assert(value(v) != value_free); return Literal(v, valSign(value(v))); }
	//! Returns the decision level on which v was assigned.
	/*!
	 * \note The returned value is only meaningful if value(v) != value_free.
	 */
	uint32   level(Var v)           const { assert(validVar(v)); return assign_.level(v); }
	//! Returns true if v is currently marked as seen.
	/*!
	 * Note: variables assigned on level 0 are always marked.
	 */
	bool     seen(Var v)            const { assert(validVar(v)); return assign_.seen(v, 3u); }
	//! Returns true if the literal p is currently marked as seen.
	bool     seen(Literal p)        const { assert(validVar(p.var())); return assign_.seen(p.var(), uint8(1+p.sign())); }
	//! Returns the current decision level.
	uint32   decisionLevel()        const { return (uint32)levels_.size(); }
	bool     validLevel(uint32 dl)  const { return dl != 0 && dl <= decisionLevel(); }
	//! Returns the starting position of decision level dl in the trail.
	/*!
	 * \pre validLevel(dl)
	 */
	uint32   levelStart(uint32 dl)  const { assert(validLevel(dl)); return levels_[dl-1].trailPos; }
	//! Returns the decision literal of the decision level dl.
	/*!
	 * \pre validLevel(dl)
	 */
	Literal  decision(uint32 dl)    const { assert(validLevel(dl)); return assign_.trail[ levels_[dl-1].trailPos ]; }
	//! Returns true, if the current assignment is conflicting.
	bool     hasConflict()          const { return !conflict_.empty(); }
	//! Returns the number of (unprocessed) literals in the propagation queue.
	uint32   queueSize()            const { return (uint32) assign_.qSize(); }
	//! Number of problem constraints in this solver.
	uint32   numConstraints()       const;
	//! Returns the number of constraints that are currently in the solver's learnt database.
	uint32   numLearntConstraints() const { return (uint32)learnts_.size(); }
	//! Returns the reason for p being true.
	/*!
	 * \pre p is true w.r.t the current assignment
	 */
	const Antecedent& reason(Literal p)              const { assert(isTrue(p)); return assign_.reason(p.var()); }
	//! Returns the additional reason data associated with p.
	uint32            reasonData(Literal p)          const { return assign_.data(p.var()); }
	//! Returns the current (partial) assignment as a set of true literals.
	/*!
	 * \note Although the special var 0 always has a value it is not considered to be
	 * part of the assignment.
	 */
	const LitVec&     trail()                        const { return assign_.trail; }
	const Assignment& assignment()                   const { return assign_; }
	//! Returns the current conflict as a set of literals.
	const LitVec&     conflict()                     const { return conflict_; }
	//! Returns the most recently derived conflict clause.
	const LitVec&     conflictClause()               const { return cc_; }
	//! Returns the enumeration constraint set by the enumerator used.
	Constraint*       getEnumerationConstraint()     const { return enum_; }
	//! Returns the idx'th learnt constraint.
	/*!
	 * \pre idx < numLearntConstraints()
	 */
	LearntConstraint& getLearnt(uint32 idx)          const {
		assert(idx < numLearntConstraints());
		return *static_cast<LearntConstraint*>(learnts_[ idx ]);
	}
	
	SolveStats  stats;  /**< stores some statistics about the solving process */
	//@}

	/*!
	 * \name Watch management
	 * Functions for setting/removing watches.
	 * \pre validVar(v)
	 */
	//@{
	//! Returns the number of constraints watching the literal p.
	uint32        numWatches(Literal p)              const;
	//! Returns true if the constraint c watches the literal p.
	bool          hasWatch(Literal p, Constraint* c) const;
	bool          hasWatch(Literal p, ClauseHead* c) const;
	//! Returns c's watch-structure associated with p.
	/*!
	 * \note returns 0, if hasWatch(p, c) == false
	 */
	GenericWatch* getWatch(Literal p, Constraint* c) const;
	//! Adds c to the watch-list of p.
	/*!
	 * When p becomes true, c->propagate(p, data, *this) is called.
	 * \post hasWatch(p, c) == true
	 */
	void addWatch(Literal p, Constraint* c, uint32 data = 0) {
		assert(validWatch(p));
		watches_[p.index()].push_right(GenericWatch(c, data));
	}
	//! Adds w to the clause watch-list of p.
	void addWatch(Literal p, const ClauseWatch& w) {
		assert(validWatch(p));
		watches_[p.index()].push_left(w);
	}
	//! Removes c from p's watch-list.
	/*!
	 * \post hasWatch(p, c) == false
	 */
	void removeWatch(const Literal& p, Constraint* c);
	void removeWatch(const Literal& p, ClauseHead* c);
	//! Adds c to the watch-list of decision-level dl.
	/*!
	 * Constraints in the watch-list of a decision level are
	 * notified when that decision level is about to be backtracked.
	 * \pre validLevel(dl)
	 */
	void addUndoWatch(uint32 dl, Constraint* c) {
		assert(validLevel(dl));
		if (levels_[dl-1].undo != 0) {
			levels_[dl-1].undo->push_back(c);
		}
		else {
			levels_[dl-1].undo = allocUndo(c);
		}
	}
	//! Removes c from the watch-list of the decision level dl.
	bool removeUndoWatch(uint32 dl, Constraint* c);
	//@}

	/*!
	 * \name Misc functions
	 * Low-level implementation functions. Use with care and only if you
	 * know what you are doing!
	 */
	//@{
	//! Sets the initial saved value of v to val if not yet assigned.
	void initSavedValue(Var v, ValueRep val) {
		assert(validVar(v));
		if (assign_.saved(v) == value_free) { assign_.setSavedValue(v, val); }
	}
	//! Sets the preferred value of v to val. 
	/*!
	 * The preferred value overwrites any sign heuristic.
	 */
	void initPrefValue(Var v, ValueRep val) {
		assert(validVar(v));
		if (assign_.pref(v) == value_free) { assign_.setPrefValue(v, val); }
	}
	//! Returns the default decision literal of v.
	Literal  defaultLiteral(Var v);
	//! Updates the reason for p being tue.
	/*!
	 * \pre p is true and x is a valid reason for p
	 */
	bool setReason(Literal p, const Antecedent& x, uint32 data = UINT32_MAX) {
		assert(isTrue(p));
		assign_.setReason(p.var(), x);
		if (data != UINT32_MAX) { assign_.setData(p.var(), data); }
		return true;
	}
	//! Returns the reason for p being true as a set of literals.
	void reason(Literal p, LitVec& out) { assert(isTrue(p)); out.clear(); return assign_.reason(p.var()).reason(*this, p, out); }
	//! Computes a new lbd for the antecedent of p given as the range [first, last).
	/*!
	 * \param p A literal implied by [first, last)
	 * \param [first, last) The literals of a learnt nogood implying p.
	 * \param cLbd The current lbd of the learnt nogood.
	 * \return The new lbd of the learnt nogood.
	 *
	 * \note If SolverStrategies::bumpVarAct is active, p's activity
	 *       is increased if the new lbd is smaller than the lbd of the
	 *       conflict clause that is currently being derived.
	 */
	uint32 updateLearnt(Literal p, const Literal* first, const Literal* last, uint32 cLbd, bool forceUp = false) {
		uint32 up = strategies().updateLbd;
		if ((up || forceUp) && cLbd > 1) {
			uint32 strict = (up == 2u);
			uint32 p1     = (up == 3u);
			uint32 nLbd   = (strict|p1) + countLevels(first, last, cLbd - strict);
			if (nLbd < cLbd) { cLbd = nLbd - p1; }
		}
		if (strategies().bumpVarAct && isTrue(p)) { bumpAct_.push_back(WeightLiteral(p, cLbd)); }
		return cLbd;
	}
	//! Visitor function for antecedents used during conflict clause minimization.
	bool ccMinimize(Literal p, CCMinRecursive* rec) const {
		return seen(p.var()) || 
			(rec && hasLevel(level(p.var())) && rec->checkRecursive(p));
	}
	
	//! Sets a soft memory limit on learnt nogoods.
	/*!
	 * The soft limit is checked in search() and when hit
	 * search() will return value_free.
	 */
	void  setMemLimit(uint32 learntLimitInMB);
	//! Returns true if number of learnts exceeds x.learnts or the soft memory limit is exceeded.
	bool  learntLimit(const SearchLimits& x) const { return numLearntConstraints() > x.learnts || memLimit_ < 0; }
	
	//! Allocates a small block (32-bytes) from the solver's small block pool.
	void* allocSmall()       { return smallAlloc_->allocate(); }
	//! Frees a small block previously allocated from the solver's small block pool.
	void  freeSmall(void* m) { smallAlloc_->free(m);    }
	
	void  addLearntBytes(uint32 bytes)  { memLimit_ -= bytes; }
	void  freeLearntBytes(uint32 bytes) { memLimit_ += bytes; }
	
	//! simplifies cc and returns finalizeConflictClause(cc, info);
	uint32  simplifyConflictClause(LitVec& cc, ClauseInfo& info, ClauseHead* rhs);
	uint32  finalizeConflictClause(LitVec& cc, ClauseInfo& info);
	uint32  countLevels(const Literal* first, const Literal* last, uint32 maxLevels);
	bool hasLevel(uint32 dl)    const { assert(validLevel(dl)); return levels_[dl-1].marked != 0; }
	bool frozenLevel(uint32 dl) const { assert(validLevel(dl)); return levels_[dl-1].freeze != 0; }
	void markLevel(uint32 dl)    { assert(validLevel(dl)); levels_[dl-1].marked = 1; }
	void freezeLevel(uint32 dl)  { assert(validLevel(dl)); levels_[dl-1].freeze = 1; }
	void unmarkLevel(uint32 dl)  { assert(validLevel(dl)); levels_[dl-1].marked = 0; }
	void unfreezeLevel(uint32 dl){ assert(validLevel(dl)); levels_[dl-1].freeze = 0; }
	void markSeen(Var v)         { assert(validVar(v)); assign_.setSeen(v, 3u); }
	void markSeen(Literal p)     { assert(validVar(p.var())); assign_.setSeen(p.var(), uint8(1+p.sign())); }
	void clearSeen(Var v)        { assert(validVar(v)); assign_.clearSeen(v);  }
	//@}
private:
	struct DLevel {
		explicit DLevel(uint32 pos = 0, ConstraintDB* u = 0)
			: trailPos(pos)
			, marked(0)
			, freeze(0)
			, undo(u) {}
		uint32        trailPos : 30;
		uint32        marked   :  1;
		uint32        freeze   :  1;
		ConstraintDB* undo;
	};
	typedef PodVector<DLevel>::type     DecisionLevels;  
	typedef PodVector<Antecedent>::type ReasonVec;
	typedef PodVector<WatchList>::type  Watches;
	struct PPList {
		PPList();
		~PPList();
		void add(PostPropagator* p);
		void remove(PostPropagator* p);
		bool propagate(Solver& s, PostPropagator* p) const;
		void simplify(Solver& s, bool shuf);
		void reset() const;
		bool isModel(Solver& s) const;
		bool init(Solver& s, PostPropagator* list) const;
		bool init(Solver& s) const { return init(s, head); }
		PostPropagator* enableFunky();
		PostPropagator* disableFunky();
		PostPropagator* nil() const { return (PostPropagator*)1; }
		PostPropagator* head;
		PostPropagator* look;
	};
	struct CmpScore {
		typedef std::pair<uint32, Activity> ViewPair;
		CmpScore(const ConstraintDB& learnts, ReduceStrategy::Score sc, uint32 g) : db(learnts), rs(sc), glue(g) {}
		uint32 score(const Activity& act)                             const { return ReduceStrategy::asScore(rs, act); }
		bool operator()(uint32 lhsId, uint32 rhsId)                   const { return (*this)(db[lhsId], db[rhsId]); }
		bool operator()(const ViewPair& lhs, const ViewPair& rhs)     const { return this->operator()(lhs.second, rhs.second); }
		bool operator()(Activity lhs, Activity rhs)                   const { return ReduceStrategy::compare(rs, lhs, rhs) < 0;}
		bool operator()(const Constraint* lhs, const Constraint* rhs) const {
			return this->operator()(
				static_cast<const LearntConstraint*>(lhs)->activity(), 
				static_cast<const LearntConstraint*>(rhs)->activity());
		}
		const ConstraintDB&   db;
		ReduceStrategy::Score rs;
		uint32                glue;
	private: CmpScore& operator=(const CmpScore&);
	};
	inline  bool hasStopConflict() const;
	bool    validWatch(Literal p) const { return p.index() < (uint32)watches_.size(); }
	void    freeMem();
	bool    simplifySAT();
	void    simplifyShort(Literal p);
	bool    unitPropagate();
	bool    propagateUnits();
	void    undoLevel(bool sp);
	uint32  analyzeConflict();
	void    otfs(Antecedent& lhs, const Antecedent& rhs, Literal p, bool final);
	ClauseHead* otfsRemove(ClauseHead* c, const LitVec* newC);
	uint32  ccMinimize(LitVec& cc, LitVec& removed, uint32 antes, CCMinRecursive* ccMin);
	bool    ccRemovable(Literal p, uint32 antes, CCMinRecursive* ccMin);
	Antecedent ccHasReverseArc(Literal p, uint32 maxLevel, uint32 maxNew);
	void    ccResolve(LitVec& cc, uint32 pos, const LitVec& reason);
	void    undoFree(ConstraintDB* x);
	void    setConflict(Literal p, const Antecedent& a, uint32 data);
	uint64  updateBranch(uint32 cfl);
	DBInfo  reduceLinear(uint32 maxR, const CmpScore& cmp);
	DBInfo  reduceSort(uint32 maxR, const CmpScore& cmp);
	DBInfo  reduceSortInPlace(uint32 maxR, const CmpScore& cmp, bool onlyPartialSort);
	ConstraintDB* allocUndo(Constraint* c);
	SolverStrategies  strategy_;    // Strategies used by this solver-object
	Heuristic         heuristic_;   // active decision heuristic
	Assignment        assign_;      // three-valued assignment
	ConstraintDB      constraints_; // problem constraints
	ConstraintDB      learnts_;     // learnt constraints
	ImpliedList       impliedLits_; // Lits that were asserted on current dl but are logically implied earlier
	LitVec            conflict_;    // stores conflict-literals for later analysis
	LitVec            cc_;          // temporary: stores conflict clause within analyzeConflict
	LitVec            temp_;        // temporary: eliminated vars that are unconstraint w.r.t the current model
	WeightLitVec      bumpAct_;     // temporary: lits from current dl whose activity might get an extra bump
	DecisionLevels    levels_;      // Stores information (e.g. position in trail) on each decision level
	VarVec            lbdStamp_;    // temporary vector for computing LBD
	VarVec            cflStamp_;    // temporary vector for computing number of conflicts in branch
	Watches           watches_;     // for each literal p: list of constraints watching p
	PPList            post_;        // (possibly empty) list of post propagators
	ClauseInfo        ccInfo_;      // temporary: stores information about conflict clause cc_
	CCMinRecursive*   ccMin_;       // additional data for supporting recursive strengthen
	SmallClauseAlloc* smallAlloc_;  // allocator object for small clauses
	SharedContext*    shared_;      // initialized by master thread - otherwise read-only!
	ConstraintDB*     undoHead_;    // Free list of undo DBs
	Constraint*       enum_;        // enumeration constraint - set by enumerator
	int64             memLimit_;    // Soft memory limit on learnt constraints
	uint32            id_;          // solver id - only needed with multi-threading
	uint32            units_;       // number of top-level assignments: always marked as seen
	LitVec::size_type lastSimplify_;// number of top-level assignments on last call to simplify
	uint32            rootLevel_;   // dl on which search started.
	uint32            btLevel_;     // When enumerating models: DL of the last unflipped decision from the current model. Can't backjump below this level.
	uint32            lbdTime_;     // temporary counter for computing lbd
	bool              shuffle_;     // shuffle program on next simplify?
};

inline bool isRevLit(const Solver& s, Literal p, uint32 maxL) {
	return s.isFalse(p) && (s.seen(p) || s.level(p.var()) < maxL);
}
inline Literal Solver::defaultLiteral(Var v) {
	uint32 xs = strategies().signDef;
	if (xs == SolverStrategies::sign_type) { return sharedContext()->preferredLiteralByType(v); }
	if (xs == SolverStrategies::sign_rnd)  { return Literal(v, strategies().rng.drand() < 0.5); }
	return Literal(v, xs == SolverStrategies::sign_yes);
}

//@}

/**
 * \defgroup heuristic Decision Heuristics
 */
//@{

//! Base class for decision heuristics to be used in a Solver.
/*! 
 * During search the decision heuristic is used whenever the DPLL-procedure must pick 
 * a new variable to branch on. Each concrete decision heuristic can implement a
 * different algorithm for selecting the next decision variable.
 */
class DecisionHeuristic {
public:
	DecisionHeuristic() {}
	virtual ~DecisionHeuristic(); 
	/*!
	 * Called once after all problem variables are known to the solver.
	 * The default-implementation is a noop.
	 * \param s The solver in which this heuristic is used.
	 */
	virtual void startInit(const Solver& /* s */) {}  

	/*!
	 * Called once after all problem constraints are known to the solver
	 * and the problem was simplified. 
	 * The default-implementation is a noop.
	 * \param s The solver in which this heuristic is used.
	 */
	virtual void endInit(Solver& /* s */) { }  
	
	/*!
	 * Called for each var that changes its state from eliminated back to normal.
	 * The default-implementation is a noop.
	 * \param s Solver in which v is resurrected.
	 * \param v The variable that is resurrected.
	 */
	virtual void resurrect(const Solver& /* s */, Var /* v */) {}
	
	/*!
	 * Called on decision level 0. Variables that are assigned on this level
	 * may be removed from any decision heuristic.
	 * \note Whenever the solver returns to dl 0, simplify is only called again
	 * if the solver learnt new facts since the last call to simplify.
	 *
	 * \param s The solver that reached decision level 0.
	 * \param st The position in the trail of the first new learnt fact.
	 */
	virtual void simplify(const Solver& /* s */, LitVec::size_type /* st */) { }
	
	/*!
	 * Called whenever the solver backracks.
	 * Literals in the range [s.trail()[st], s.trail().size()) are subject to backtracking.
	 * The default-implementation is a noop.
	 * \param s The solver that is about to backtrack.
	 * \param st Position in the trail of the first literal that will be backtracked.
	 */
	virtual void undoUntil(const Solver& /* s */, LitVec::size_type /* st */) {}
	
	/*!
	 * Called whenever a new constraint is added to the solver s.
	 * The default-implementation is a noop.
	 * \param s The solver to which the constraint is added.
	 * \param first First literal of the new constraint.
	 * \param size Size of the new constraint.
	 * \param t Type of the new constraint.
	 * \note first points to an array of size size.
	 */
	virtual void newConstraint(const Solver&, const Literal* /* first */, LitVec::size_type /* size */, ConstraintType /* t */) {}
	
	/*!
	 * Called for each new reason-set that is traversed during conflict analysis.
	 * The default-implementation is a noop.
	 * \param s The solver in which the conflict is analyzed.
	 * \param lits The current reason-set under inspection.
	 * \param resolveLit The literal that is currently resolved.
	 * \note When a conflict is detected, the solver passes the conflicting literals
	 * in lits during the first call to updateReason. On that first call resolveLit
	 * is the sentinel-literal.
	 */
	virtual void updateReason(const Solver& /* s */, const LitVec& /* lits */, Literal /* resolveLit */) {}

	//! Shall bump the activity of literals in lits by lits.second * adj.
	/*!
	 * The default-implementation is a noop and always returns false.
	 * \return true if heuristic supports activities, false otherwise.
	 */
	virtual bool bump(const Solver& /* s */, const WeightLitVec& /* lits */, double /* adj */) { return false; }
	
	/*! 
	 * Called whenever the solver must pick a new variable to branch on. 
	 * \param s The solver that needs a new decision variable.
	 * \return
	 *  - true  : if the decision heuristic assumed a literal 
	 *  - false : if no decision could be made because assignment is total or there is a conflict
	 *  .
	 * \post
	 * If true is returned, the heuristic has asserted a literal.
	 */
	bool select(Solver& s) { return s.numFreeVars() != 0 && s.assume(doSelect(s)); }

	//! Implements the actual selection process.
	/*!
	 * \pre s.numFreeVars() > 0, i.e. there is at least one variable to branch on.
	 * \return 
	 *  - a literal that is currently free or
	 *  - a sentinel literal. In that case, the heuristic shall have asserted a literal!
	 */ 
	virtual Literal doSelect(Solver& /* s */) = 0;

	/*! 
	 * Shall select one of the literals in the range [first, last).
	 * \param s     The solver that needs a new decision variable.
	 * \param first Pointer to first literal in range.
	 * \param last  Pointer to the end of the range.
	 * \pre [first, last) is not empty and all literals in the range are currently unassigned.
	 * \note The default implementation returns *first.
	 */
	virtual Literal selectRange(Solver& /* s */, const Literal* first, const Literal* /* last */) {
		return *first;
	}
	static Literal selectLiteral(Solver& s, Var v, int sc) {
		ValueRep xv;
		if ((xv = s.prefValue(v)) != value_free) { return Literal(v, valSign(xv)); }
		if ((xv = s.savedValue(v))!= value_free) { return Literal(v, valSign(xv)); }
		if (sc != 0)                             { return Literal(v, sc < 0); }
		return s.defaultLiteral(v);
	}
private:
	DecisionHeuristic(const DecisionHeuristic&);
	DecisionHeuristic& operator=(const DecisionHeuristic&);
};

//! Selects the first free literal w.r.t to the initial variable order.
class SelectFirst : public DecisionHeuristic {
private:
	Literal doSelect(Solver& s);
};

//@}
}
#endif
