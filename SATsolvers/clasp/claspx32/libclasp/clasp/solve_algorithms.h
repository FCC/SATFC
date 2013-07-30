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
#ifndef CLASP_SOLVE_ALGORITHMS_H_INCLUDED
#define CLASP_SOLVE_ALGORITHMS_H_INCLUDED

#ifdef _MSC_VER
#pragma warning (disable : 4200) // nonstandard extension used : zero-sized array
#pragma once
#endif

#include <clasp/solver_strategies.h>

/*!
 * \file 
 * Defines top-level functions for solving problems.
 */
namespace Clasp { 

template <class T>
struct Range {
	Range(T x, T y) : lo(x), hi(y) { if (x > y)  { hi = x;  lo = y; } }
	T clamp(T val) const { 
		if (val < lo) return lo;
		if (val > hi) return hi;
		return val;
	}
	T lo;
	T hi;
};
typedef Range<uint32> Range32;
	
//! Aggregates restart-parameters to configure restarts during search.
/*!
 * \see ScheduleStrategy
 */
struct RestartParams {
	RestartParams() : sched(), counterRestart(0), counterBump(9973), shuffle(0), shuffleNext(0), cntLocal(0), dynRestart(0), bndRestart(0), rstRestart(0) {}
	void disable();
	bool dynamic()     const { return dynRestart != 0; }
	bool local()       const { return cntLocal   != 0; }
	bool bounded()     const { return bndRestart != 0; }
	bool resetOnModel()const { return rstRestart != 0; }
	ScheduleStrategy sched; /**< Restart schedule to use. */
	uint16 counterRestart;  /**< Apply counter implication bump every counterRestart restarts (0: disable). */
	uint16 counterBump;     /**< Bump factor for counter implication restarts. */
	uint32 shuffle    :14;  /**< Shuffle program after shuffle restarts (0: disable). */
	uint32 shuffleNext:14;  /**< Re-Shuffle program every shuffleNext restarts (0: disable). */
	uint32 cntLocal   : 1;  /**< Count conflicts globally or relative to current branch? */
	uint32 dynRestart : 1;  /**< Dynamic restarts enabled? */
	uint32 bndRestart : 1;  /**< Allow (bounded) restarts after first solution was found. */
	uint32 rstRestart : 1;  /**< Repeat restart seq. after each solution. */
};

//! Aggregates parameters for the nogood deletion heuristic used during search.
struct ReduceParams {
	ReduceParams() : cflSched(ScheduleStrategy::none()), growSched(ScheduleStrategy::def())
		, fInit(1.0f/3.0f)
		, fMax(3.0f)
		, fGrow(1.1f)
		, initRange(10, UINT32_MAX)
		, maxRange(UINT32_MAX) {}
	void   disable();
	uint32 getBase(const SharedContext& ctx)   const;
	uint32 initLimit(const SharedContext& ctx) const { return getLimit(getBase(ctx), fInit, initRange); }
	uint32 maxLimit(const SharedContext& ctx)  const { 
		return getLimit(getBase(ctx), fMax, Range32(initLimit(ctx), maxRange)); 
	}
	bool   estimate() const { return strategy.estimate != 0; }
	float  fReduce()  const { return strategy.fReduce / 100.0f; }
	float  fRestart() const { return strategy.fRestart/ 100.0f; }
	static uint32 getLimit(uint32 base, double f, const Range<uint32>& r);
	ScheduleStrategy cflSched;   /**< Conflict-based deletion schedule.               */
	ScheduleStrategy growSched;  /**< Growth-based deletion schedule.                 */
	ReduceStrategy   strategy;   /**< Strategy to apply during nogood deletion.       */
	float            fInit;      /**< Initial limit. X = P*fInit clamped to initRange.*/
	float            fMax;       /**< Maximal limit. X = P*fMax  clamped to maxRange. */
	float            fGrow;      /**< Growth factor for db.                           */
	Range32          initRange;  /**< Allowed range for initial limit.                */
	uint32           maxRange;   /**< Allowed range for maximal limit: [initRange.lo,maxRange]*/
};

//! Type for holding global solve limits.
struct SolveLimits {
	explicit SolveLimits(uint64 conf = UINT64_MAX, uint64 r = UINT64_MAX) 
		: conflicts(conf)
		, restarts(r) {
	}
	bool   reached() const { return conflicts == 0 || restarts == 0; }
	uint64 conflicts; /*!< Number of conflicts. */
	uint64 restarts;  /*!< Number of restarts.  */
};

//! Type for holding pre-solve options.
struct InitParams {
	InitParams() : randRuns(0), randConf(0), lookType(0), lookOps(0) {}
	uint16 randRuns; /*!< Number of initial randomized-runs. */
	uint16 randConf; /*!< Number of conflicts comprising one randomized-run. */
	uint16 lookType; /*!< Type of lookahead operations. */
	uint16 lookOps;  /*!< Max. number of lookahead operations (0: no limit). */
};
///////////////////////////////////////////////////////////////////////////////
// Parameter-object for the solve function
// 
///////////////////////////////////////////////////////////////////////////////
//! Parameter-Object for configuring search-parameters.
/*!
 * \ingroup solver
 */
struct SolveParams {
	//! Creates a default-initialized object.
	/*!
	 * The following parameters are used:
	 * restart      : quadratic: 100*1.5^k / no restarts after first solution
	 * deletion     : initial size: vars()/3, grow factor: 1.1, max factor: 3.0, do not reduce on restart
	 * randomization: disabled
	 * randomProp   : 0.0 (disabled)
	 */
	SolveParams();
	
	RestartParams restart;
	ReduceParams  reduce;
	InitParams    init;

	//! Sets the randomization-parameters to use during path initialization.
	/*!
	 * \param runs Number of initial randomized-runs.
	 * \param cfl  Number of conflicts comprising one randomized-run.
	 */
	bool setRandomizeParams(uint32 runs, uint32 cfls) {
		if (!runs || !cfls) { runs = cfls = 0; }
		init.randRuns = (uint16)std::min(runs, (1u<<16)-1);
		init.randConf = (uint16)std::min(cfls, (1u<<16)-1);
		return true;
	}

	//! Sets the probability with which choices are made randomly instead of with the installed heuristic.
	bool setRandomProbability(double p) {
		if (p >= 0.0 && p <= 1.0) {
			randFreq_ = p;
		}
		return randFreq_ == p;
	}
	// accessors
	uint32  randRuns()          const { return init.randRuns; }
	uint32  randConflicts()     const { return init.randConf; }
	double  randomProbability() const { return randFreq_;     }
private:
	double randFreq_;
};
//! Object holding options for one solver instance.
struct SolverConfig {
	explicit    SolverConfig(Solver& s) : solver(&s) {}
	void        initFrom(const SolverConfig& other);
	Solver*     solver;
	SolveParams params;
};
///////////////////////////////////////////////////////////////////////////////
// Basic solve functions
///////////////////////////////////////////////////////////////////////////////

//! Basic sequential search.
/*!
 * \ingroup solver
 * \relates Solver
 * \param ctx The context containing the problem.
 * \param p   The solve parameters to use.
 * \param lim Optional solve limit. 
 *
 * \return
 *  - true:  if the search stopped before the search-space was exceeded.
 *  - false: if the search-space was completely examined.
 * 
 */
bool solve(SharedContext& ctx, const SolveParams& p, const SolveLimits& lim = SolveLimits());

//! Basic sequential search under assumptions.
/*!
 * \ingroup solver
 * \relates Solver
 * The use of assumptions allows for incremental solving. Literals contained
 * in assumptions are assumed to be true during search but are undone before solve returns.
 *
 * \param ctx The context containing the problem.
 * \param p   The solve parameters to use.
 * \param assumptions The list of initial unit-assumptions.
 * \param lim Optional solve limit.
 *
 * \return
 *  - true:  if the search stopped before the search-space was exceeded.
 *  - false: if the search-space was completely examined.
 * 
 */
bool solve(SharedContext& ctx, const SolveParams& p, const LitVec& assumptions, const SolveLimits& lim = SolveLimits());

///////////////////////////////////////////////////////////////////////////////
// General solve
///////////////////////////////////////////////////////////////////////////////
struct SolveEvent_t { enum Type {progress_msg = 1, progress_state = 2, progress_path = 3}; };
struct SolveMsgEvent : SolveEvent {
	SolveMsgEvent(const Solver& s, const char* m) : SolveEvent(s, SolveEvent_t::progress_msg), msg(m) {}
	const char* msg;
};
struct SolveStateEvent : SolveEvent {
	SolveStateEvent(const Solver& s, const char* m, double exitTime = -1.0) : SolveEvent(s, SolveEvent_t::progress_state), state(m), time(exitTime) {}
	const char* state; // state that is entered or left
	double      time;  // < 0: enter, else exit
};
struct SolvePathEvent : SolveEvent {
	enum EventType { event_none = 0, event_deletion = 'D', event_grow = 'G', event_model = 'M', event_restart = 'R' };
	SolvePathEvent(const Solver& s, EventType t, uint64 cLim, uint32 lLim) : SolveEvent(s, SolveEvent_t::progress_path), cLimit(cLim), lLimit(lLim), evType(t) {}
	uint64    cLimit; // next conflict limit
	uint32    lLimit; // next learnt limit
	EventType evType; // type of path event
};
//! Interface for solve algorithms.
/*!
 * \ingroup solver
 * \relates Solver
 * SolveAlgorithm objects wrap an enumerator and
 * implement concrete solve algorithms.
 */
class SolveAlgorithm {
public:
	/*!
	 * \param lim    An optional solve limit applied in solve().
	 */
	explicit SolveAlgorithm(const SolveLimits& limit = SolveLimits());
	virtual ~SolveAlgorithm();

	//! Force termination of current solve process.
	/*!
	 * Shall return true if termination is supported, otherwise false.
	 */
	virtual bool   terminate() = 0;
	virtual bool   hasErrors() const { return false; }
	//! Runs the solve algorithm.
	/*!
	 * \param ctx    A fully initialized context object containing the problem.
	 * \param p      The solve parameters for the master solver ctx.master()
	 * \param assume A list of initial unit-assumptions.
	 *
	 * \return
	 *  - true:  if the search stopped before the search-space was exceeded.
	 *  - false: if the search-space was completely examined.
	 *
	 * \note 
	 * The use of assumptions allows for incremental solving. Literals contained
	 * in assumptions are assumed to be true during search but are undone before solve returns.
	 */
	bool solve(SharedContext& ctx, const SolveParams& p, const LitVec& assume);
protected:
	//! The default implementation simply forwards the call to the enumerator.
	virtual bool  backtrackFromModel(Solver& s);
	virtual bool  doSolve(Solver& s, const SolveParams& p) = 0;
	bool          initPath(Solver& s,  const LitVec& gp, InitParams& params);
	ValueRep      solvePath(Solver& s, const SolveParams& p, SolveLimits& inOut);
	const LitVec& getInitialPath()     const { return assumptions_; }
	const SolveLimits& getSolveLimits()const { return limits_; }
	void               setSolveLimits(const SolveLimits& x) { limits_ = x; }
private:
	SolveAlgorithm(const SolveAlgorithm&);
	SolveAlgorithm& operator=(const SolveAlgorithm&);
	SolveLimits    limits_;
	LitVec         assumptions_;
};
//! A basic algorithm for single-threaded sequential solving.
class SimpleSolve : public SolveAlgorithm {
public:
	SimpleSolve(const SolveLimits& lim) : SolveAlgorithm(lim) {}
	bool   terminate();
private:
	bool   doSolve(Solver& s, const SolveParams& p);
};
}
#endif
