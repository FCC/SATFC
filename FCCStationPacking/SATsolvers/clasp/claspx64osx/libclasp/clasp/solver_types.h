// 
// Copyright (c) 2006-2011, Benjamin Kaufmann
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
#ifndef CLASP_SOLVER_TYPES_H_INCLUDED
#define CLASP_SOLVER_TYPES_H_INCLUDED
#ifdef _MSC_VER
#pragma once
#endif

#include <clasp/literal.h>
#include <clasp/constraint.h>
#include <clasp/util/left_right_sequence.h>
#include <clasp/util/misc_types.h>
#include <clasp/util/type_manip.h>

/*!
 * \file 
 * Contains some types used by a Solver
 */
namespace Clasp {
class SharedLiterals;

/*!
 * \addtogroup solver
 */
//@{

///////////////////////////////////////////////////////////////////////////////
// Statistics
///////////////////////////////////////////////////////////////////////////////
//! A struct for holding core solving statistics used by a solver.
struct CoreStats {
	CoreStats() { reset(); }
	void reset(){ std::memset(this, 0, sizeof(*this)); }
	void accu(const CoreStats& o) {
		const uint64* rhs = &o.choices;
		      uint64* lhs = &choices;
		while (rhs != &o.cflLast) { *lhs++ += *rhs++; }
		cflLast = std::max(o.cflLast, cflLast);
		lhs     = learnts; rhs = o.learnts;
		while (rhs <= &o.deleted) { *lhs++ += *rhs++; }
		binary += o.binary;
		ternary+= o.ternary;
	}
	void addLearnt(uint32 size, ConstraintType t) {
		assert(t != Constraint_t::static_constraint && t <= Constraint_t::max_value);
		learnts[t-1]+= 1;
		lits[t-1]   += size;
		binary      += (size == 2);
		ternary     += (size == 3);
	}
	double avgLbd() const { return sumLbd / (double)analyzed; }
	double avgCfl() const { return sumCfl / (double)analyzed; }
	uint64 choices;   /**< Number of choices performed. */
	uint64 conflicts; /**< Number of conflicts found. */
	uint64 analyzed;  /**< Number of backjumps (i.e. number of analyzed conflicts). */
	uint64 restarts;  /**< Number of restarts. */ 
	uint64 models;    /**< Number of models found. */
	uint64 sumCfl;    /**< Sum of conflict levels. */
	uint64 sumLbd;    /**< Sum of lbds.            */
	uint64 cflLast;   /**< Number of conflicts since last restart. */
	// CL2 - Learnt stats
	typedef uint64 Array[Constraint_t::max_value];
	Array  learnts;   /**< learnts[t-1]: Number of learnt nogoods of type t.  */
	Array  lits;      /**< lits[t-1]   : Sum of literals in nogoods of type t.*/
	uint64 deleted;   /**< Sum of learnt nogoods removed. */
	uint32 binary;    /**< Number of learnt binary nogoods. */
	uint32 ternary;   /**< Number of learnt ternary nogoods.*/
};
//! A struct for jump statistics.
struct JumpStats {
	JumpStats() { reset(); }
	void reset(){ std::memset(this, 0, sizeof(*this)); }
	void accu(const JumpStats& o) {
		modLits += o.modLits;
		bJumps  += o.bJumps;
		jumpSum += o.jumpSum;
		boundSum+= o.boundSum;
		maxJump  = std::max(o.maxJump, maxJump);
		maxJumpEx= std::max(o.maxJumpEx, maxJumpEx);
		maxBound = std::max(o.maxBound, maxBound);
	}
	void    update(uint32 dl, uint32 uipLevel, uint32 bLevel) {
		jumpSum += dl - uipLevel; 
		maxJump = std::max(maxJump, dl - uipLevel);
		if (uipLevel < bLevel) {
			++bJumps;
			boundSum += bLevel - uipLevel;
			maxJumpEx = std::max(maxJumpEx, dl - bLevel);
			maxBound  = std::max(maxBound, bLevel - uipLevel);
		}
		else { maxJumpEx = maxJump; }
	}
	uint64  modLits;  /**< Sum of decision literals in models. */
	uint64  bJumps;   /**< Number of backjumps that were bounded. */
	uint64  jumpSum;  /**< Number of levels that could be skipped w.r.t first-uip. */
	uint64  boundSum; /**< Number of levels that could not be skipped because of backtrack-level.*/
	uint32  maxJump;  /**< Longest possible backjump. */
	uint32  maxJumpEx;/**< Longest executed backjump (< maxJump if longest jump was bounded). */
	uint32  maxBound; /**< Max difference between uip- and backtrack-level. */
};
//! A struct for aggregating statistics relevant for parallel solving.
/*!
 * Always associated with one solver (thread).
 */
struct ParallelStats {
	ParallelStats() { reset(); }
	double  cpuTime;    /**< (Estimated) cpu time of the current solver. */
	uint64  distributed;/**< Number of nogoods distributed. */
	uint64  integrated; /**< Number of nogoods integrated   */
	uint64  sumLbd;     /**< Sum of lbds of shared nogoods. */
	uint64  imps;       /**< Number of initial implications from shared. */
	uint64  jumps;      /**< Sum of backjumps needed to integrate new implications. */
	uint64  gpLits;     /**< Sum of literals in received guiding paths. */
	uint32  gps;        /**< Number of guiding paths received. */
	uint32  splits;     /**< Number of split requests handled. */
	void    reset() { 
		std::memset(this, 0, sizeof(*this)); 
		cpuTime = 0.0;
	}
	void    accu(const ParallelStats& o) {
		cpuTime     += o.cpuTime;
		// dist stats
		distributed += o.distributed;
		integrated  += o.integrated;
		sumLbd      += o.sumLbd;
		imps        += o.imps;
		jumps       += o.jumps;
		// gp stats
		gpLits      += o.gpLits;
		splits      += o.splits;
		gps         += o.gps;
	}
	void newGP(LitVec::size_type length) {
		++gps;
		gpLits += length;
	}
};

struct QueueImpl {
	explicit QueueImpl(uint32 size) : maxSize(size), wp(0), rp(0) {}
	bool    full()  const { return size() == maxSize; }
	uint32  size()  const { return ((rp > wp) * cap()) + wp - rp;}
	uint32  cap()   const { return maxSize + 1; }
	void    clear()       { wp = rp = 0; }
	uint32  top()   const { return buffer[rp]; }
	void    push(uint32 x){ buffer[wp] = x; if (++wp == cap()) { wp = 0; } }
	void    pop()         { if (++rp == cap()) { rp = 0; } }
	uint32  maxSize;
	uint32  wp;
	uint32  rp;
	uint32  buffer[1];
};

struct SumQueue {
	static SumQueue* create(uint32 size) {
		void* m = ::operator new(sizeof(SumQueue) + (size*sizeof(uint32)));
		return new (m) SumQueue(size);
	}
	void destroy() { this->~SumQueue(); ::operator delete(this); }
	void reset()   { sumLbd = sumCfl = samples = 0; queue.clear(); }
	void update(uint32 dl, uint32 lbd) {
		if (samples++ >= queue.maxSize) {
			uint32 y = queue.top(); 
			sumLbd  -= (y & 127u);
			sumCfl  -= (y >> 7u);
			queue.pop();
		}
		sumLbd += lbd;
		sumCfl += dl;
		queue.push((dl << 7) + lbd);
	}
	double  avgLbd() const { return sumLbd / (double)queue.maxSize; }
	double  avgCfl() const { return sumCfl / (double)queue.maxSize; }
	uint32  maxSize()const { return queue.maxSize; }
	bool    full()   const { return queue.full();  }
	uint32     sumLbd;
	uint32     sumCfl;
	uint32     samples;
	QueueImpl  queue;
private: SumQueue(uint32 size) : sumLbd(0), sumCfl(0), samples(0), queue(size) {}
private: SumQueue(const SumQueue&);
private: SumQueue& operator=(const SumQueue&);
};
//! A struct for aggregating statistics of one solve operation.
struct SolveStats : public CoreStats {
	SolveStats() : queue(0), jumps(0), parallel(0) {}
	SolveStats(const SolveStats& o) : CoreStats(o), queue(0), jumps(0), parallel(0) {
		if (o.queue)    enableQueue(o.queue->maxSize());
		if (o.jumps)    jumps    = new JumpStats(*o.jumps); 
		if (o.parallel) parallel = new ParallelStats(*o.parallel);
	}
	~SolveStats() { delete parallel; delete jumps; if (queue) queue->destroy(); }
	void enableParallelStats() { if (!parallel)parallel = new ParallelStats(); }
	void enableStats(uint32 level);
	void enableStats(const SolveStats& other);
	void enableQueue(uint32 size);
	void reset();
	void accu(const SolveStats& o);
	void swapStats(SolveStats& o);
	inline void updateJumps(uint32 dl, uint32 uipLevel, uint32 bLevel, uint32 lbd);
	inline void addDistributed(uint32 lbd, ConstraintType t);
	inline void addIntegratedAsserting(uint32 receivedDL, uint32 jumpDL);
	inline void addIntegrated(uint32 num = 1);
	inline void removeIntegrated(uint32 num = 1);
	inline void addDeleted(uint32 num);
	inline void addModel(uint32 decisionLevel);
	SumQueue*      queue;    /**< Optional queue for running averages. */
	JumpStats*     jumps;    /**< Optional jump statistics. */
	ParallelStats* parallel; /**< Optional parallel statistics. */
private: SolveStats& operator=(const SolveStats&);
};
inline void SolveStats::addDeleted(uint32 num)                    { deleted += num; }
inline void SolveStats::addDistributed(uint32 lbd, ConstraintType){ if (parallel) { ++parallel->distributed; parallel->sumLbd += lbd; } }
inline void SolveStats::updateJumps(uint32 dl, uint32 uipLevel, uint32 bLevel, uint32 lbd) {
	++analyzed;
	sumCfl += dl;
	sumLbd += lbd;
	if (queue) { queue->update(dl, lbd); }
	if (jumps) { jumps->update(dl, uipLevel, bLevel); }
}
inline void SolveStats::addIntegratedAsserting(uint32 rDL, uint32 jDL) {
	if (parallel) { ++parallel->imps; parallel->jumps += (rDL - jDL); }
}
inline void SolveStats::addIntegrated(uint32 n)   { if (parallel) { parallel->integrated += n;} }
inline void SolveStats::removeIntegrated(uint32 n){ if (parallel) { parallel->integrated -= n;} }
inline void SolveStats::addModel(uint32 DL) {
	++models;
	if (jumps)    { jumps->modLits += DL;}
}
///////////////////////////////////////////////////////////////////////////////


///////////////////////////////////////////////////////////////////////////////
// Clauses
///////////////////////////////////////////////////////////////////////////////
//! Type storing initial information on a (learnt) clause.
class ClauseInfo {
public:
	typedef ClauseInfo self_type;
	enum { MAX_LBD = Activity::MAX_LBD, MAX_ACTIVITY = (1<<22)-1 }; 
	ClauseInfo(ConstraintType t = Constraint_t::static_constraint) : act_(0), lbd_(MAX_LBD), type_(t), tag_(0) {
		static_assert(sizeof(self_type) == sizeof(uint32), "Unsupported padding");
	}
	bool           learnt()   const { return type() != Constraint_t::static_constraint; }
	ConstraintType type()     const { return static_cast<ConstraintType>(type_); }
	uint32         activity() const { return static_cast<uint32>(act_); }
	uint32         lbd()      const { return static_cast<uint32>(lbd_); }
	bool           tagged()   const { return tag_ != 0; }
	self_type&     setType(ConstraintType t) { type_  = static_cast<uint32>(t); return *this; }
	self_type&     setActivity(uint32 act)   { act_   = std::min(act, (uint32)MAX_ACTIVITY); return *this; }
	self_type&     setTagged(bool b)         { tag_   = static_cast<uint32>(b); return *this; }
	self_type&     setLbd(uint32 a_lbd)      { lbd_   = std::min(a_lbd, (uint32)MAX_LBD); return *this; }
private:
	uint32 act_ : 22; // Activity of clause
	uint32 lbd_ :  7; // Literal block distance in the range [0, MAX_LBD]
	uint32 type_:  2; // One of ConstraintType
	uint32 tag_ :  1; // Conditional constraint?
};
//! (Abstract) base class for clause types.
/*!
 * ClauseHead is used to enforce a common memory-layout for all clauses.
 * It contains the two watched literals and a cache literal to improve
 * propagation performance. A virtual call to Constraint::propagate()
 * is only needed if the other watch is not true and the cache literal
 * is false.
 */
class ClauseHead : public LearntConstraint {
public:
	enum { HEAD_LITS = 3, MAX_SHORT_LEN = 5, MAX_LBD = (1<<5)-1, TAGGED_CLAUSE = 1023, MAX_ACTIVITY = (1<<15)-1 };
	explicit ClauseHead(const ClauseInfo& init);
	// base interface
	//! Propagates the head and calls updateWatch() if necessary.
	PropResult propagate(Solver& s, Literal, uint32& data);
	//! Type of clause.
	ConstraintType type() const    { return ConstraintType(info_.data.type); }
	//! True if this clause currently is the antecedent of an assignment.
	bool     locked(const Solver& s) const;
	//! Returns the activity of this clause.
	Activity activity() const       { return Activity(info_.data.act, info_.data.lbd); }
	//! Halves the activity of this clause.
	void     decreaseActivity()     { info_.data.act >>= 1; }
	//! Downcast from LearntConstraint.
	ClauseHead* clause()           { return this; }
	
	// clause interface
	typedef std::pair<bool, bool> BoolPair;
	//! Increases activity.
	void bumpActivity()     { info_.data.act += (info_.data.act != MAX_ACTIVITY); }
	//! Adds watches for first two literals in head to solver.
	void attach(Solver& s);
	//! Returns true if head is satisfied w.r.t current assignment in s.
	bool satisfied(const Solver& s);
	//! Conditional clause?
	bool tagged() const     { return info_.data.key == uint32(TAGGED_CLAUSE); }
	bool learnt() const     { return info_.data.type!= 0; }
	uint32 lbd()  const     { return info_.data.lbd; }
	void lbd(uint32 x)      { info_.data.lbd = std::min(x, uint32(MAX_LBD)); }
	//! Removes watches from s.
	virtual void     detach(Solver& s);
	//! Returns the size of this clause.
	virtual uint32   size()              const = 0;
	//! Returns the literals of this clause in out.
	virtual void     toLits(LitVec& out) const = 0;
	//! Returns true if this clause is a valid "reverse antecedent" for p.
	virtual bool     isReverseReason(const Solver& s, Literal p, uint32 maxL, uint32 maxN) = 0;
	//! Removes p from clause if possible.
	/*!
	 * \return
	 *   The first component of the returned pair specifies whether or not
	 *   p was removed from the clause.
	 *   The second component of the returned pair specifies whether
	 *   the clause should be kept (false) or removed (true). 
	 */
	virtual BoolPair strengthen(Solver& s, Literal p, bool allowToShort = true) = 0;
protected:
	friend struct ClauseWatch;
	bool         toImplication(Solver& s);
	void         clearTagged()   { info_.data.key = 0; }
	void         setLbd(uint32 x){ info_.data.lbd = x; }
	bool         hasLbd() const  { return info_.data.type != Constraint_t::learnt_other || lbd() != MAX_LBD; }
	//! Shall replace the watched literal at position pos with a non-false literal.
	/*!
	 * \pre pos in [0,1] 
	 * \pre s.isFalse(head_[pos]) && s.isFalse(head_[2])
	 * \pre head_[pos^1] is the other watched literal
	 */
	virtual bool updateWatch(Solver& s, uint32 pos) = 0;
	union Data {
		SharedLiterals* shared;
		struct LocalClause {
			uint32 sizeExt;
			uint32 idx;
			void   init(uint32 size) {
				if (size <= ClauseHead::MAX_SHORT_LEN){ sizeExt = idx = negLit(0).asUint(); }
				else                                  { sizeExt = (size << 3) + 1; idx = 0; }
			}
			bool   isSmall()     const    { return (sizeExt & 1u) == 0u; }
			bool   contracted()  const    { return (sizeExt & 3u) == 3u; }
			bool   strengthened()const    { return (sizeExt & 5u) == 5u; }
			uint32 size()        const    { return sizeExt >> 3; }
			void   setSize(uint32 size)   { sizeExt = (size << 3) | (sizeExt & 7u); }
			void   markContracted()       { sizeExt |= 2u;  }
			void   markStrengthened()     { sizeExt |= 4u;  }
			void   clearContracted()      { sizeExt &= ~2u; }
		}               local;
		uint32          lits[2];
	}       data_;   // additional data
	union Info { 
		Info() : rep(0) {}
		explicit Info(const ClauseInfo& i);
		struct {
			uint32 act : 15; // activity of clause
			uint32 key : 10; // lru key of clause
			uint32 lbd :  5; // lbd of clause
			uint32 type:  2; // type of clause
		}      data;
		uint32 rep;
	}       info_;
	Literal head_[HEAD_LITS]; // two watched literals and one cache literal
};
//! Allocator for small (at most 32-byte) clauses.
class SmallClauseAlloc {
public:
	SmallClauseAlloc();
	~SmallClauseAlloc();
	void* allocate() {
		if(freeList_ == 0) {
			allocBlock();
		}
		Chunk* r   = freeList_;
		freeList_  = r->next;
		return r;
	}
	void   free(void* mem) {
		Chunk* b = reinterpret_cast<Chunk*>(mem);
		b->next  = freeList_;
		freeList_= b;
	}
private:
	SmallClauseAlloc(const SmallClauseAlloc&);
	SmallClauseAlloc& operator=(const SmallClauseAlloc&);
	struct Chunk {
		Chunk*        next; // enforce ptr alignment
		unsigned char mem[32 - sizeof(Chunk*)];
	};
	struct Block {
		enum { num_chunks = 1023 };
		Block* next;
		unsigned char pad[32-sizeof(Block*)];
		Chunk  chunk[num_chunks];
	};
	void allocBlock();
	Block*  blocks_;
	Chunk*  freeList_;
};
///////////////////////////////////////////////////////////////////////////////
// Watches
///////////////////////////////////////////////////////////////////////////////
//! Represents a clause watch in a Solver.
struct ClauseWatch {
	//! Clause watch: clause head
	explicit ClauseWatch(ClauseHead* a_head) : head(a_head) { }
	ClauseHead* head;
	struct EqHead {
		explicit EqHead(ClauseHead* h) : head(h) {}
		bool operator()(const ClauseWatch& w) const { return head == w.head; }
		ClauseHead* head;
	};
};

//! Represents a generic watch in a Solver.
struct GenericWatch {
	//! A constraint and some associated data.
	explicit GenericWatch(Constraint* a_con, uint32 a_data = 0) : con(a_con), data(a_data) {}
	//! Calls propagate on the stored constraint and passes the stored data to that constraint.
	Constraint::PropResult propagate(Solver& s, Literal p) { return con->propagate(s, p, data); }
	
	Constraint* con;    /**< The constraint watching a certain literal. */
	uint32      data;   /**< Additional data associated with this watch - passed to constraint on update. */

	struct EqConstraint {
		explicit EqConstraint(Constraint* c) : con(c) {}
		bool operator()(const GenericWatch& w) const { return con == w.con; }
		Constraint* con;
	};	
};

//! Watch list type.
typedef bk_lib::left_right_sequence<ClauseWatch, GenericWatch, 0> WatchList;
inline void releaseVec(WatchList& w) { w.clear(true); }

///////////////////////////////////////////////////////////////////////////////
// Assignment
///////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////////
//! Type for storing reasons for variable assignments together with additional data.
/*!
 * \note On 32-bit systems additional data is stored in the high-word of antecedents.
 */
struct ReasonStore32 : PodVector<Antecedent>::type {
	uint32  dataSize() const     { return (uint32)size(); }
	void    dataResize(uint32)   {}
	uint32  data(uint32 v) const { return decode((*this)[v]);}
	void    setData(uint32 v, uint32 data) { encode((*this)[v], data); }
	static  void   encode(Antecedent& a, uint32 data) {
		a.asUint() = (uint64(data)<<32) | static_cast<uint32>(a.asUint());
	}
	static  uint32 decode(const Antecedent& a) {
		return static_cast<uint32>(a.asUint()>>32);
	}
	struct value_type {
		value_type(const Antecedent& a, uint32 d) : ante_(a) {
			if (d != UINT32_MAX) { encode(ante_, d); assert(data() == d && ante_.type() == Antecedent::generic_constraint); }
		}
		const Antecedent& ante() const { return ante_;      }
		      uint32      data() const { return ante_.type() == Antecedent::generic_constraint ? decode(ante_) : UINT32_MAX; }
		Antecedent ante_;
	};
};

//! Type for storing reasons for variable assignments together with additional data.
/*
 * \note On 64-bit systems additional data is stored in a separate container.
 */
struct ReasonStore64 : PodVector<Antecedent>::type {
	uint32  dataSize() const               { return (uint32)data_.size(); }
	void    dataResize(uint32 nv)          { if (nv > dataSize()) data_.resize(nv, UINT32_MAX); }
	uint32  data(uint32 v) const           { return data_[v]; }
	void    setData(uint32 v, uint32 data) { dataResize(v+1); data_[v] = data; }
	VarVec  data_;
	struct  value_type : std::pair<Antecedent, uint32> {
		value_type(const Antecedent& a, uint32 d) : std::pair<Antecedent, uint32>(a, d) {}
		const Antecedent& ante() const { return first;  }
		      uint32      data() const { return second; }
	};
};

//! Stores assignment related information.
/*!
 * For each variable v, the class stores 
 *  - v's current value (value_free if unassigned)
 *  - the decision level on which v was assign (only valid if value(v) != value_free)
 *  - the reason why v is in the assignment (only valid if value(v) != value_free)
 *  - (optionally) some additional data associated with the reason
 *  .
 * Furthermore, the class stores the sequences of assignments as a set of
 * true literals in its trail-member.
 */
class Assignment  {
public:
	typedef PodVector<uint32>::type     AssignVec;
	typedef PodVector<uint8>::type      PhaseVec;
	typedef bk_lib::detail::if_then_else<
		sizeof(Constraint*)==sizeof(uint64)
		, ReasonStore64
		, ReasonStore32>::type            ReasonVec;
	typedef ReasonVec::value_type       ReasonWithData;
	Assignment() : front(0), eliminated_(0) { }
	LitVec            trail;   // assignment sequence
	LitVec::size_type front;   // "propagation queue"
	bool              qEmpty() const { return front == trail.size(); }
	uint32            qSize()  const { return (uint32)trail.size() - front; }
	Literal           qPop()         { return trail[front++]; }
	void              qReset()       { front  = trail.size(); }

	//! Number of variables in the three-valued assignment.
	uint32            numVars()    const { return (uint32)assign_.size(); }
	//! Number of assigned variables.
	uint32            assigned()   const { return (uint32)trail.size();   }
	//! Number of free variables.
	uint32            free()       const { return numVars() - (assigned()+eliminated_);   }
	//! Returns the largest possible decision level.
	uint32            maxLevel()   const { return (1u<<28)-1; }
	//! Returns v's value in the three-valued assignment.
	ValueRep          value(Var v) const { return ValueRep(assign_[v] & 3u); }
	//! Returns v's previously saved value in the three-valued assignment.
	ValueRep          saved(Var v) const { return v < phase_.size() ? ValueRep(phase_[v] & 3u)  : value_free; }
	//! Returns v's preferred value or value_free if not set.
	ValueRep          pref(Var v)  const { return v < phase_.size() ? ValueRep(phase_[v] >> 2) : value_free; }
	//! Returns the decision level on which v was assigned if value(v) != value_free.
	uint32            level(Var v) const { return assign_[v] >> 4u; }
	//! Returns the reason for v being assigned if value(v) != value_free.
	const Antecedent& reason(Var v)const { return reason_[v]; }
	//! Returns the number of allocated data slots.
	uint32            numData()    const { return reason_.dataSize(); }
	//! Returns the reason data associated with v.
	uint32            data(Var v)  const { assert(v < reason_.dataSize()); return reason_.data(v); }

	//! Resize to nv variables.
	void resize(uint32 nv) {
		assign_.resize(nv);
		reason_.resize(nv);
	}
	//! Adds a var to assignment - initially the new var is unassigned.
	Var addVar() {
		assign_.push_back(0);
		reason_.push_back(0);
		return numVars()-1;
	}
	//! Allocates data slots for nv variables to be used for storing additional reason data.
	void requestData(uint32 nv) {
		reason_.dataResize(nv);
	}
	//! Eliminates var from assignment.
	void eliminate(Var v) {
		assert(value(v) == value_free && "Can not eliminate assigned var!\n");
		setValue(v, value_true);
		++eliminated_;
	}

	//! Assigns p.var() on level lev to the value that makes p true and stores x as reason for the assignment.
	/*!
	 * \return true if the assignment is consistent. False, otherwise.
	 * \post If true is returned, p is in trail. Otherwise, ~p is.
	 */
	bool assign(Literal p, uint32 lev, const Antecedent& x) {
		const Var      v   = p.var();
		const ValueRep val = value(v);
		if (val == value_free) {
			assign_[v] = (lev<<4) + trueValue(p);
			reason_[v] = x;
			trail.push_back(p);
			return true;
		}
		return val == trueValue(p);
	}
	bool assign(Literal p, uint32 lev, Constraint* c, uint32 data) {
		const Var      v   = p.var();
		const ValueRep val = value(v);
		if (val == value_free) {
			assign_[v] = (lev<<4) + trueValue(p);
			reason_[v] = c;
			reason_.setData(v, data);
			trail.push_back(p);
			return true;
		}
		return val == trueValue(p);
	}
	//! Undos all assignments in the range trail[first, last).
	/*!
	 * \param first First assignment to be undone.
	 * \param save  If true, previous assignment of a var is saved before it is undone.
	 */
	void undoTrail(LitVec::size_type first, bool save) {
		if (!save) { popUntil<&Assignment::clearValue>(trail[first]); }
		else       { phase_.resize(numVars(), 0); popUntil<&Assignment::saveAndClear>(trail[first]); }
		front  = trail.size();
	}
	//! Undos the last assignment.
	void undoLast() { clearValue(trail.back().var()); trail.pop_back(); }
	//! Returns the last assignment as a true literal.
	Literal last() const { return trail.back(); }
	Literal&last()       { return trail.back(); }
	//! Sets val as "previous value" of v.
	void setSavedValue(Var v, ValueRep val) {
		if (phase_.size() <= v) phase_.resize(numVars(), 0);
		save(v, val);
	}
	void setPrefValue(Var v, ValueRep val) {
		if (phase_.size() <= v) phase_.resize(numVars(), 0);
		phase_[v] = (phase_[v] & 3u) | (val << 2);
	}
	/*!
	 * \name Implementation functions
	 * Low-level implementation functions. Use with care and only if you
	 * know what you are doing!
	 */
	//@{
	bool seen(Var v, uint8 m) const { return (assign_[v] & (m<<2)) != 0; }
	void setSeen(Var v, uint8 m)    { assign_[v] |= (m<<2); }
	void clearSeen(Var v)           { assign_[v] &= ~uint32(12); }
	void clearValue(Var v)          { assign_[v] = 0; }
	void setValue(Var v, ValueRep val) {
		assert(value(v) == val || value(v) == value_free);
		assign_[v] = val;
	}
	void setReason(Var v, const Antecedent& a) { reason_[v] = a;  }
	void setData(Var v, uint32 data) { reason_.setData(v, data); }
	void copyAssignment(Assignment& o) const { o.assign_ = assign_; }
	//@}
private:
	Assignment(const Assignment&);
	Assignment& operator=(const Assignment&);
	void    save(Var v, ValueRep val) { phase_[v] = (phase_[v] & uint8(0xFC)) | val; }
	void    saveAndClear(Var v)       { save(v, value(v)); clearValue(v); }
	template <void (Assignment::*op)(Var v)>
	void popUntil(Literal stop) {
		Literal p;
		do {
			p = trail.back(); trail.pop_back();
			(this->*op)(p.var());
		} while (p != stop);
	}
	AssignVec assign_; // for each var: three-valued assignment
	ReasonVec reason_; // for each var: reason for being assigned (+ optional data)
	PhaseVec  phase_;  // for each var: previous assignment and fixed sign (if any)
	uint32    eliminated_;
};

//! Stores information about a literal that is implied on an earlier level than the current decision level.
struct ImpliedLiteral {
	typedef Assignment::ReasonWithData AnteInfo;
	ImpliedLiteral(Literal a_lit, uint32 a_level, const Antecedent& a_ante, uint32 a_data = UINT32_MAX) 
		: lit(a_lit)
		, level(a_level)
		, ante(a_ante, a_data) {
	}
	Literal     lit;    /**< The implied literal */
	uint32      level;  /**< The earliest decision level on which lit is implied */
	AnteInfo    ante;   /**< The reason why lit is implied on decision-level level */
};
//! A type for storing ImpliedLiteral objects.
struct ImpliedList {
	typedef PodVector<ImpliedLiteral>::type VecType;
	typedef VecType::const_iterator iterator;
	ImpliedList() : level(0), front(0) {}
	//! Searches for an entry <p> in list. Returns 0 if none is found.
	ImpliedLiteral* find(Literal p) {
		for (uint32 i = 0, end = lits.size(); i != end; ++i) {
			if (lits[i].lit == p)  { return &lits[i]; }
		}
		return 0;
	}
	//! Adds a new object to the list.
	void add(uint32 dl, const ImpliedLiteral& n) {
		if (dl > level) { level = dl; }
		lits.push_back(n);
	}
	//! Returns true if list contains entries that must be reassigned on current dl.
	bool active(uint32 dl) const { return dl < level && front != lits.size(); }
	//! Reassigns all literals that are still implied.
	bool assign(Solver& s);
	iterator begin() const { return lits.begin(); }
	iterator end()   const { return lits.end();   }
	VecType lits;  // current set of (out-of-order) implied literals
	uint32  level; // highest dl on which lits must be reassigned
	uint32  front; // current starting position in lits
};

struct CCMinRecursive {
	enum State { state_open = 0, state_poison = 1, state_removable = 2 };
	void  init(uint32 numV) { extra.resize(numV,0); }
	State state(Literal p) const { return State(extra[p.var()]); }
	bool  checkRecursive(Literal p) {
		if (state(p) == state_open) { p.clearWatch(); dfsStack.push_back(p); }
		return state(p) != state_poison;
	}
	void  markVisited(Literal p, State st) {
		if (state(p) == state_open) {
			visited.push_back(p.var());
		}
		extra[p.var()] = static_cast<uint8>(st);
	}
	void clear() {
		for (; !visited.empty(); visited.pop_back()) {
			extra[visited.back()] = 0;
		}
	}
	typedef PodVector<uint8>::type DfsState;
	LitVec   dfsStack;
	VarVec   visited;
	DfsState extra;
};
//@}
}
#endif
