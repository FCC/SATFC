// 
// Copyright (c) 2010-2012, Benjamin Kaufmann
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
#ifndef CLASP_SHARED_CONTEXT_H_INCLUDED
#define CLASP_SHARED_CONTEXT_H_INCLUDED
#ifdef _MSC_VER
#pragma warning (disable : 4200) // nonstandard extension used : zero-sized array
#pragma once
#endif

#include <clasp/literal.h>
#include <clasp/constraint.h>
#include <clasp/util/left_right_sequence.h>
#include <clasp/util/misc_types.h>
#include <clasp/util/atomic.h>
/*!
 * \file 
 * Contains some types shared between different solvers
 */
namespace Clasp {
class Solver;
class ClauseInfo;	
class Assignment;
class SharedContext;
class Enumerator;
class SharedLiterals;

/*!
 * \addtogroup solver
 */
//@{
//! Base class for preprocessors working on clauses only
class SatPreprocessor {
public:
	SatPreprocessor() : ctx_(0) {}
	virtual ~SatPreprocessor();
	void setContext(SharedContext& ctx) { ctx_ = &ctx; }
	virtual bool addClause(const LitVec& cl) = 0;
	virtual bool preprocess(bool enumerateModels) = 0;
	virtual void extendModel(Assignment& m, LitVec& open) = 0;
	virtual bool limit(uint32 numCons) const = 0;
	struct Stats {
		Stats() : clRemoved(0), clAdded(0), litsRemoved(0) {}
		uint32 clRemoved;
		uint32 clAdded;
		uint32 litsRemoved;
	} stats;
protected:
	void reportProgress(const PreprocessEvent& ev);
	SharedContext*  ctx_;
private:
	SatPreprocessor(const SatPreprocessor&);
	SatPreprocessor& operator=(const SatPreprocessor&);
};
//@}

/**
 * \defgroup shared classes to be shared between solvers
 */
//@{

///////////////////////////////////////////////////////////////////////////////
// Problem statistics
///////////////////////////////////////////////////////////////////////////////
//! A struct for aggregating basic problem statistics.
/*!
 * Maintained in SharedContext.
 */
struct ProblemStats {
	ProblemStats() { reset(); }
	uint32  vars;
	uint32  vars_eliminated;
	uint32  vars_frozen;
	uint32  constraints;
	uint32  constraints_binary;
	uint32  constraints_ternary;
	uint32  size;
	uint32  complexity;
	void    reset() { std::memset(this, 0, sizeof(*this)); }
	void diff(const ProblemStats& o) {
		vars               = std::max(vars, o.vars)-std::min(vars, o.vars);
		vars_eliminated    = std::max(vars_eliminated, o.vars_eliminated)-std::min(vars_eliminated, o.vars_eliminated);
		vars_frozen        = std::max(vars_frozen, o.vars_frozen)-std::min(vars_frozen, o.vars_frozen);
		constraints        = std::max(constraints, o.constraints) - std::min(constraints, o.constraints);
		constraints_binary = std::max(constraints_binary, o.constraints_binary) - std::min(constraints_binary, o.constraints_binary);
		constraints_ternary= std::max(constraints_ternary, o.constraints_ternary) - std::min(constraints_ternary, o.constraints_ternary);
	} 
};

//! Stores static information about variables.
class VarInfo {
public:
	enum FLAGS {
		RESERVED_1 = 0x1u, // reserved for future
		RESERVED_2 = 0x2u, // use
		NANT   = 0x4u, // if this var is an atom, is it in NAnt(P)
		PROJECT= 0x8u, // do we project on this var?
		BODY   = 0x10u,// is this var representing a body?
		EQ     = 0x20u,// is the var representing both a body and an atom?
		ELIM   = 0x40u,// is the variable eliminated?
		FROZEN = 0x80u // is the variable frozen?
	};
	VarInfo() {}
	void  reserve(uint32 maxSize) { info_.reserve(maxSize); }
	void  add(bool body) {
		uint8 m = (!body?0:flag(BODY));
		info_.push_back( m );
	}
	bool      empty()                const { return info_.empty(); }
	uint32    numVars()              const { return (uint32)info_.size(); }
	bool      isSet(Var v, FLAGS f)  const { return (info_[v] & flag(f)) != 0; }
	void      toggle(Var v, FLAGS f)       { info_[v] ^= flag(f); }
	void      clear() { info_.clear(); }
private:
	// Bit:   7     6   5   4    3    2   1     0
	//      frozen elim eq body proj nant reserved
	typedef PodVector<uint8>::type InfoVec;
	static uint8 flag(FLAGS x) { return uint8(x); }
	
	VarInfo(const VarInfo&);
	VarInfo& operator=(const VarInfo&);
	InfoVec info_;
};

//! A class for efficiently storing and propagating binary and ternary clauses.
class ShortImplicationsGraph {
public:
	ShortImplicationsGraph();
	~ShortImplicationsGraph();
	//! Makes room for nodes number of nodes.
	void resize(uint32 nodes);
	//! Mark the instance as shared/unshared.
	/*!
	 * A shared instance adds learnt binary/ternary clauses
	 * to specialized shared blocks of memory.
	 */
	void markShared(bool b) { shared_ = b; }
	//! Adds the binary constraint (p, q) to the implication graph.
	/*!
	 * \return true iff a new implication was added.
	 */
	bool addBinary(Literal p, Literal q, bool learnt);
	//! Adds the ternary constraint (p, q, r) to the implication graph.
	/*!
	 * \return true iff a new implication was added.
	 */
	bool addTernary(Literal p, Literal q, Literal r, bool learnt);
	
	//! Removes p and its implications.
	/*!
	 * \pre s.isTrue(p)
	 */
	void removeTrue(Solver& s, Literal p);
	
	//! Propagates consequences of p following from binary and ternary clauses.
	/*!
	 * \pre s.isTrue(p)
	 */
	bool   propagate(Solver& s, Literal p) const;
	//! Propagates immediate consequences of p following from binary clauses only.
	bool   propagateBin(Assignment& out, Literal p, uint32 dl) const;
	//! Checks whether there is a reverse arc implying p and if so returns it in out.
	bool   reverseArc(const Solver& s, Literal p, uint32 maxLev, Antecedent& out) const;
	
	uint32 numBinary() const { return bin_[0]; }
	uint32 numTernary()const { return tern_[0]; }
	uint32 numLearnt() const { return bin_[1] + tern_[1]; }
	uint32 numEdges(Literal p) const;
	//! Applies op on all unary- and binary implications following from p.
	/*!
	 * OP must provide two functions:
	 *  - bool unary(Literal, Literal)
	 *  - bool binary(Literal, Literal, Literal)
	 * The first argument will be p, the second (resp. third) the unary
	 * (resp. binary) clause implied by p.
	 * \note For learnt imps, at least one literal has its watch-flag set.
	 */
	template <class OP>
	bool forEach(Literal p, const OP& op) const {
		const ImplicationList& x = graph_[p.index()];
		if (x.empty()) return true;
		ImplicationList::const_right_iterator rEnd = x.right_end(); // prefetch
		for (ImplicationList::const_left_iterator it = x.left_begin(), end = x.left_end(); it != end; ++it) {
			if (!op.unary(p, *it)) { return false; }
		}
		for (ImplicationList::const_right_iterator it = x.right_begin(); it != rEnd; ++it) {
			if (!op.binary(p, it->first, it->second)) { return false; }
		}
#if WITH_THREADS
		for (Block* b = (x).learnt; b ; b = b->next) {
			p.watch(); bool r = true;
			for (Block::const_iterator imp = b->begin(), endOf = b->end(); imp != endOf; ) {
				if (!imp->watched()) { r = op.binary(p, imp[0], imp[1]); imp += 2; }
				else                 { r = op.unary(p, imp[0]);          imp += 1; }
				if (!r)              { return false; }
			}
		}
#endif
		return true;
	}
private:
	ShortImplicationsGraph(const ShortImplicationsGraph&);
	ShortImplicationsGraph& operator=(ShortImplicationsGraph&);
	struct Propagate;
	struct ReverseArc;
#if WITH_THREADS
	struct Block {
		typedef std::atomic<uint32> atomic_size;
		typedef std::atomic<Block*> atomic_ptr;
		typedef const Literal*      const_iterator;
		typedef       Literal*      iterator;
		enum { block_cap = (64 - (sizeof(atomic_size)+sizeof(atomic_ptr)))/sizeof(Literal) };
		explicit Block();
		const_iterator  begin() const { return data; }
		const_iterator  end()   const { return data+size(); }
		iterator        end()         { return data+size(); }
		uint32          size()  const { return size_lock >> 1; }
		bool tryLock(uint32& lockedSize);
		void addUnlock(uint32 lockedSize, const Literal* x, uint32 xs);
		atomic_ptr  next;
		atomic_size size_lock;
		Literal     data[block_cap];
	};
	typedef Block::atomic_ptr SharedBlockPtr;
	typedef bk_lib::left_right_sequence<Literal, std::pair<Literal,Literal>, 64-sizeof(SharedBlockPtr)> ImpListBase;
	struct ImplicationList : public ImpListBase {
		ImplicationList() : ImpListBase() { learnt = 0; }
		ImplicationList(const ImplicationList& other) : ImpListBase(other), learnt(other.learnt) {}
		~ImplicationList();
		bool hasLearnt(Literal q, Literal r = negLit(0)) const;
		void addLearnt(Literal q, Literal r = negLit(0));
		bool empty() const { return ImpListBase::empty() && learnt == 0; }
		void move(ImplicationList& other);
		void clear(bool b);
		SharedBlockPtr learnt; 
	};
#else
	typedef bk_lib::left_right_sequence<Literal, std::pair<Literal,Literal>, 64> ImplicationList;
#endif
	ImplicationList& getList(Literal p) { return graph_[p.index()]; }
	void remove_bin(ImplicationList& w, Literal p);
	void remove_tern(ImplicationList& w, Literal p);
	typedef PodVector<ImplicationList>::type ImpLists;
	ImpLists   graph_;     // one implication list for each literal
	uint32     bin_[2];    // number of binary constraints (0: problem / 1: learnt)
	uint32     tern_[2];   // number of ternary constraints(0: problem / 1: learnt)
	bool       shared_;
};

//! Base class for distributing learnt knowledge between solvers.
class Distributor {
public:
	static  uint64  mask(uint32 i)             { return uint64(1) << i; }
	static  uint32  initSet(uint32 sz)         { return (uint64(1) << sz) - 1; }
	static  bool    inSet(uint64 s, uint32 id) { return (s & mask(id)) != 0; }
	Distributor();
	virtual ~Distributor();
	virtual void    publish(const Solver& source, SharedLiterals* lits) = 0;
	virtual uint32  receive(const Solver& in, SharedLiterals** out, uint32 maxOut) = 0;
private:
	Distributor(const Distributor&);
	Distributor& operator=(const Distributor&);
};

//! Aggregates information to be shared between solver objects.
/*!
 * Among other things, SharedContext objects store 
 * static information on variables, the (possibly unused) 
 * symbol table, as well as the binary and ternary 
 * implication graph of the input problem.
 * 
 * Furthermore, a SharedContext object stores a distinguished
 * master solver that is used to store and simplify problem constraints.
 *
 * Once initialization is completed, other solvers s can 
 * attach to this object by calling ctx->attach(s).
 */
class SharedContext {
public:
	typedef std::auto_ptr<SatPreprocessor> SatPrepro;
	typedef ProblemStats                   Stats;
	typedef LitVec::size_type              size_type;
	typedef ShortImplicationsGraph         BTIG;
	typedef Distributor*                   DistrPtr;
	
	enum InitMode        { init_share_symbols };
	enum PhysicalSharing { share_no   = 0, share_problem = 1, share_learnt = 2, share_all = 3 };
	enum UpdateMode      { update_propagate = 0, update_conflict = 1 };

	/*!
	 * \name configuration
	 */
	//@{
	//! Creates a new object for sharing variables and the binary and ternary implication graph.
	SharedContext(PhysicalSharing x = share_no, bool learnImp = true);
	//! Creates a new object that shares its symbol table with rhs.
	SharedContext(const SharedContext& rhs,  InitMode m);
	~SharedContext();
	//! Enables progress reporting via the given report callback.
	void       enableProgressReport(ProgressReport* r) { progress_ = r; }
	//! Resets this object to the state after default construction.
	void       reset();
	//! Sets maximal number of solvers sharing this object.
	void       setSolvers(uint32 numSolver);
	//! Configures physical sharing of (explicit) constraints.
	/*!
	 * If x is share_no, explicit constraints are not shared but copied.
	 * If x is share_problem, only problem constraints are shared while learnt constraints are copied.
	 * If x is share_learnt, learnt constraints are shared while problem constraints are copied.
	 * If x is share_all, all constraints are shared.
	 */
	void       physicalSharing(PhysicalSharing x)   { share_.physical = x; }
	//! Enables/disables learning of small clauses via ShortImplicationsGraph.
	void       learnImplicit(bool b)                { share_.impl = (uint32)b; }
	//! Set mt update mode.
	void       updateMode(UpdateMode x)             { share_.update = x;       }
	//! Sets the global distribution strategey to use.
	/*!
	 * \note The caller also has to set a distributor in order to enable
	 *       distribution of learnt constraints.
	 */
	void       setDistribution(uint32 maxSize, uint32 maxLbd, uint32 types) {
		share_.distSize = maxSize;
		share_.distLbd  = maxLbd;
		share_.distMask = types;
	}
	//! Sets the distributor object to use for distribution of learnt constraints.
	/*!
	 * If this function is not called or d is 0, (explicit) learnt constraints are
	 * never distributed to other solvers. Otherwise,
	 * distribution is controlled by the global distribution strategy.
	 * \see setDistribution(uint32 maxSize, uint32 maxLbd, uint32 types)
	 */
	void       setDistributor(DistrPtr d) { distributor_.reset(d); }
	DistrPtr   releaseDistributor()       { return distributor_.release(); }
	
	
	uint32     numSolvers() const { return share_.count; }
	bool       frozen()     const { return share_.frozen; }
	bool       isShared()   const { return frozen() && numSolvers() > 1; }
	UpdateMode updateMode() const { return UpdateMode(share_.update); }
	bool       physicalShare(ConstraintType t) const{ return (share_.physical & (1 + (t != Constraint_t::static_constraint))) != 0; }
	bool       physicalShareProblem()          const{ return (share_.physical & share_problem) != 0; }
	bool       distribution()                  const{ return share_.distMask != 0; }
	bool       allowImplicit(ConstraintType t) const{ return t != Constraint_t::static_constraint ? share_.impl != 0 : !isShared(); }
	//@}
	
	/*!
	 * \name problem specification
	 * Functions for adding a problem to the master solver.
	 * Problem specification is a four-stage process:
	 * -# Add variables to the SharedContext object.
	 * -# Call startAddConstraints().
	 * -# Add problem constraints to the master solver.
	 * -# Call endInit() to finish the initialization process.
	 * .
	 * \note After endInit() was called, other solvers can be attached to this object.
	 * \note In incremental setting, the process must be repeated for each incremental step.
	 * 
	 * \note Problem specification is *not* thread-safe, i.e. during initialization no other thread shall
	 * access the context.
	 *
	 * \note !frozen() is a precondition for all functions in this group!
	 */
	//@{
	
	//! Reserves space for at least varGuess variables.
	/*!
	 * \param varGuess Number of vars to reserve space for.
	 * \note If the number of variables is known upfront, passing the correct value
	 * for varGuess avoids repeated regrowing of the state data structures.
	 */
	void    reserveVars(uint32 varGuess);

	//! Adds a new variable of type t.
	/*!
	 * \param t  Type of the new variable (either Var_t::atom_var or Var_t::body_var).
	 * \param eq True if var represents both an atom and a body. In that case
	 *           t is the variable's primary type and determines the preferred literal.
	 * \return The index of the new variable.
	 * \note Problem variables are numbered from 1 onwards!
	 */
	Var     addVar(VarType t, bool eq = false);
	//! Request additional reason data slot for variable v.
	void    requestData(Var v);
	//! Freezes/defreezes a variable (a frozen var is exempt from SatELite preprocessing).
	void    setFrozen(Var v, bool b);
	//! Adds v to resp. removes v from the set of projection variables.
	void    setProject(Var v, bool b)    { assert(validVar(v)); if (b != varInfo_.isSet(v, VarInfo::PROJECT)) varInfo_.toggle(v, VarInfo::PROJECT); }
	//! Marks/unmarks v as contained in a negative loop or head of a choice rule.
	void    setNant(Var v, bool b)       { assert(validVar(v)); if (b != varInfo_.isSet(v, VarInfo::NANT))    varInfo_.toggle(v, VarInfo::NANT);    }
	//! Eliminates the variable v.
	/*!
	 * \pre v must not occur in any constraint and frozen(v) == false and value(v) == value_free
	 */
	void    eliminate(Var v);
	
	//! Requests a special tag literal for tagging conditional knowledge.
	/*!
	 * Once a tag literal p is set, learnt clauses containing ~p are
	 * tagged as "conditional". Conditional clauses can be removed from a solver
	 * by calling Solver::removeConditional(). Furthermore, calling 
	 * Solver::strengthenConditional() removes ~p from conditional clauses and
	 * transforms them to unconditional knowledge.
	 *
	 * \note Typically, the tag literal is an initial assumption and hence true during 
	 *       the whole search. 
	 */
	void    requestTagLiteral();
	void    removeTagLiteral();
	
	//! Returns the number of problem variables.
	/*!
	 * \note The special sentinel-var 0 is not counted, i.e. numVars() returns
	 * the number of problem-variables.
	 * To iterate over all problem variables use a loop like:
	 * \code
	 * for (Var i = 1; i <= numVars(); ++i) {...}
	 * \endcode
	 */
	uint32  numVars()          const { return varInfo_.numVars() - 1; }
	//! Returns the number of eliminated vars.
	uint32  numEliminatedVars()const { return problem_.vars_eliminated; }
	
	//! Returns true if var represents a valid variable in this object.
	/*!
	 * \note The range of valid variables is [1..numVars()]. The variable 0
	 * is a special sentinel variable. 
	 */
	bool    validVar(Var var)  const { return var <= numVars(); }
	//! Returns the type of variable v.
	/*!
	 * If v was added with parameter eq=true, the return value
	 * is Var_t::atom_body_var.
	 */
	VarType type(Var v)        const {
		assert(validVar(v));
		return varInfo_.isSet(v, VarInfo::EQ)
			? Var_t::atom_body_var
			: VarType(Var_t::atom_var + varInfo_.isSet(v, VarInfo::BODY));
	}
	//! Returns true if v is currently eliminated, i.e. no longer part of the problem.
	bool    eliminated(Var v)  const { assert(validVar(v)); return varInfo_.isSet(v, VarInfo::ELIM); }
	//! Returns true if v is excluded from variable elimination.
	bool    frozen(Var v)      const { assert(validVar(v)); return varInfo_.isSet(v, VarInfo::FROZEN); }
	//! Returns true if v is a projection variable.
	bool    project(Var v)     const { assert(validVar(v)); return varInfo_.isSet(v, VarInfo::PROJECT);}
	//! Returns true if v is contained in a negative loop or head of a choice rule.
	bool    nant(Var v)        const { assert(validVar(v)); return varInfo_.isSet(v, VarInfo::NANT);}
	Literal tagLiteral()       const { return tag_; }
	//! Returns the preferred decision literal of variable v w.r.t its type.
	/*!
	 * \return 
	 *  - posLit(v) if type(v) == body_var
	 *  - negLit(v) if type(v) == atom_var
	 * \note If type(v) is atom_body_var, the preferred literal is determined
	 *       by v's primary type, i.e. the one that was initially passed to addVar().
	 */
	Literal preferredLiteralByType(Var v) const {
		assert(validVar(v));
		return Literal(v, !varInfo_.isSet(v, VarInfo::BODY));
	}
	
	//! Prepares master solver so that constraints can be added.
	/*!
	 * Must be called to publish previously added variables to master solver
	 * and before constraints over these variables can be added.
	 * \post !frozen()
	 * \return The master solver associated with this object.
	 */
	Solver& startAddConstraints(uint32 constraintGuess = 100);
	//! Attaches the given enumerator to this object.
	/*!
	 * \note ownership is transferred
	 * \note In incremental setting, the enumerator must be reattached in
	 *       each incremental step by calling addEnumerator(enumerator());
	 */
	void    addEnumerator(Enumerator* en);
	//! Same as master()->addUnary(p, Constraint_t::static_constraint)
	bool    addUnary(Literal p);
	//! Same as master()->add(c)
	void    add(Constraint* c);
	//! Same as shortImplications->addBinary(p, q, learnt).
	bool    addBinary(Literal p, Literal q, bool learnt = false)             { return btig_.addBinary(p, q, learnt); }
	//! Same as shortImplications->addTernary(p, q, learnt).
	bool    addTernary(Literal p, Literal q, Literal r, bool learnt = false) { return btig_.addTernary(p, q, r, learnt); }
	
	//! Finishes initialization of the master solver.
	/*!
	 * The function must be called once before search is started. After endInit()
	 * was called, numSolvers()-1 other solvers can be attached to the 
	 * shared context and learnt constraints may be added to solver.
	 * \return If the constraints are initially conflicting, false. Otherwise, true.
	 * \note
	 * The master solver can't recover from top-level conflicts, i.e. if endInit()
	 * returned false, the solver is in an unusable state.
	 * \post frozen()
	 */
	bool    endInit();
	
	//! Attaches s to this object.
	/*!
	 * \pre other is not already attached to some other shared context.
	 * \note It is safe to attach multiple solvers concurrently
	 * but the master solver shall not change during the whole
	 * operation.
	 */
	bool    attach(Solver& other);
	
	//! Detaches s from this object.
	/*!
	 * The function removes any tentative constraints from s.
	 * Shall be called once after search has stopped.
	 * \note The function is concurrency-safe w.r.t to different solver objects, 
	 *       i.e. in a parallel search different solvers may call detach()
	 *       concurrently.
	 */
	void    detach(Solver& s);

	//! Returns the number of problem constraints.
	uint32      numConstraints()    const;
	//! Estimates the problem complexity.
	/*!
	 * \return sum of c->estimateComplexity(*master()) for each problem 
	 *         constraint c.
	 */
	uint32      problemComplexity() const;
	//! Returns the size of top-level after last call to endInit()
	size_type   topLevelSize()      const { return lastTopLevel_; }
	Enumerator* enumerator()        const { return enumerator_.get(); }
	//@}

	/*!
	 * \name learning
	 * Functions for distributing knowledge.
	 * 
	 * \note The functions in this group can be safely called 
	 * from multiple threads.
	 */
	//@{
	//! Distributes the clause in lits via the distributor.
	/*!
	 * The function first calls the distribution strategy 
	 * to decides whether the clause is a good candidate for distribution.
	 * If so, it distributes the clause and returns a handle to the
	 * now shared literals of the clause. Otherwise, it returns 0.
	 *
	 * \param owner The solver that created the clause.
	 * \param lits  The literals of the clause.
	 * \param size  The number of literals in the clause
	 * \param extra Additional information about the clause
	 * \note 
	 *   If the return value is not null, it is the caller's 
	 *   responsibility to release the returned handle (i.e. by calling release()).
	 */
	SharedLiterals* distribute(const Solver& owner, const Literal* lits, uint32 size, const ClauseInfo& extra) const;
	//! Tries to receive at most maxOut clauses.
	/*!
	 * The function queries the distribution object for new clauses to be delivered to
	 * the solver target. Clauses are stored in out.
	 * \return The number of clauses received.
	 */
	uint32          receive(const Solver& target, SharedLiterals** out, uint32 maxOut) const;
	//! Returns the number of learnt binary and ternary clauses
	uint32          numLearntShort() const { return btig_.numLearnt(); }

	//@}
	//! Returns the master solver associated with this object.
	Solver*      master()    const   { return master_; }	
	const Stats& stats()     const   { return problem_; }
	uint32       winner()    const   { return share_.winner; }
	void         setWinner(uint32 x) { share_.winner = std::min(x, numSolvers()); }
	uint32       numBinary() const   { return btig_.numBinary();  }
	uint32       numTernary()const   { return btig_.numTernary(); }
	SymbolTable& symTab()    const   { return symTabPtr_->symTab; }
	BTIG&        shortImplications() { return btig_; }
	const BTIG&  shortImplications() const { return btig_; }
	uint32       numShortImplications(Literal p) const { return btig_.numEdges(p); }	
	void         reportProgress(const PreprocessEvent& ev) const { if (progress_) progress_->reportProgress(ev);  }
	void         reportProgress(const SolveEvent& ev)      const { if (progress_) progress_->reportProgress(ev);  }
	void         setProblemSize(uint32 sz, uint32 estimate) {
		problem_.size      = sz;
		problem_.complexity= estimate;
	}
	SatPrepro    satPrepro;  // preprocessor
private:
	SharedContext(const SharedContext&);
	SharedContext& operator=(const SharedContext&);
	typedef std::auto_ptr<Distributor> DistPtr;
	typedef std::auto_ptr<Enumerator>  EnumPtr;
	typedef ProgressReport*            LogPtr;
	struct SharedSymTab {
		SharedSymTab() : refs(1) {}
		SymbolTable symTab;
		uint32      refs;
	}*           symTabPtr_;   // pointer to shared symbol table
	ProblemStats problem_;     // problem statistics
	VarInfo      varInfo_;     // varInfo[v] stores info about variable v
	BTIG         btig_;        // binary-/ternary implication graph
	Solver*      master_;      // master solver, responsible for init
	LogPtr       progress_;    // report interface or 0 if not used
	EnumPtr      enumerator_;  // enumerator object
	DistPtr      distributor_; // object for distributing learnt knowledge
	Literal      tag_;         // literal for tagging learnt constraints
	size_type    lastInit_;    // size of master's db after last init
	size_type    lastTopLevel_;// size of master's top-level after last init
	struct Share {             // Share-Flags
		uint32 count   :14;      //   max number of objects sharing this object
		uint32 winner  :14;      //   id of solver that terminated the search
		uint32 physical: 2;      //   mode of physical sharing 
		uint32 impl    : 1;      //   allow update of BTIG after endInit()?
		uint32 frozen  : 1;      //   is adding of problem constraints allowed?
		uint32 distSize: 20;     //   distribute constraints up to this size only
		uint32 distLbd : 8;      //   distribute constraints up to this lbd only
		uint32 distMask: 3;      //   distribute constraints of this type only
		uint32 update  : 1;      //   process update messages on propagation or after conflict
		bool   distribute(uint32 size, uint32 lbd, uint32 type) const {
			return size <= distSize && lbd <= distLbd && ((type & distMask) != 0);
		}
	}            share_;
};

//@}
}
#endif
