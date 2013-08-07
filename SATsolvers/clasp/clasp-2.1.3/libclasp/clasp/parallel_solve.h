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
#ifndef CLASP_PARALLEL_SOLVE_H_INCLUDED
#define CLASP_PARALLEL_SOLVE_H_INCLUDED

#ifdef _MSC_VER
#pragma once
#endif

#if WITH_THREADS

#include <clasp/solve_algorithms.h>
#include <clasp/constraint.h>
#include <clasp/shared_context.h>
#include <clasp/util/thread.h>
#include <clasp/util/multi_queue.h>
#include <clasp/solver_types.h>

/*!
 * \file 
 * Defines classes controlling multi-threaded parallel solving.
 * 
 */
namespace Clasp { namespace mt {

class ParallelHandler;
class ParallelSolve;

struct ParallelSolveOptions {
	enum SearchMode      { mode_split = 0, mode_compete  = 1 };
	ParallelSolveOptions() : mode(mode_compete) {}
	struct Integration { /**< Nogood integration options. */
		Integration() :  grace(1024), filter(filter_gp), topo(topo_all) {}
		enum Filter   { filter_no = 0, filter_gp = 1, filter_sat = 2, filter_heuristic = 3 };
		enum Topology { topo_all  = 0, topo_ring = 1, topo_cube  = 2, topo_cubex = 3       };
		uint32 grace : 28; /**< Lower bound on number of shared nogoods to keep. */
		uint32 filter: 2;  /**< Filter for integrating shared nogoods (one of Filter). */   
		uint32 topo  : 2;  /**< Integration topology */
	};          
	struct GRestarts {   /**< Options for configuring global restarts. */
		GRestarts():maxR(0) {}
		uint32           maxR;
		ScheduleStrategy sched;
	};
	SolveLimits  limit;     /**< Solve limit (disabled by default). */
	Integration  integrate; /**< Nogood integration parameters. */
	GRestarts    restarts;  /**< Global restart strategy. */
	SearchMode   mode;
	//! Returns a newly allocated solve object in out.
	/*!
	 * \pre sc points to an array of size >= ctx.numSolvers().
	 */
	void createSolveObject(SolveAlgorithm*& out, SharedContext& ctx, SolverConfig** sc) const;
	//! Returns the number of threads that can run concurrently on the current hardware.
	static uint32   recommendedSolvers() { return std::thread::hardware_concurrency(); }
	//! Returns number of maximal number of supported threads.
	static uint32   supportedSolvers()   { return 64; }
};

//! A parallel algorithm for multi-threaded solving with and without search-space splitting.
/*!
 * The class adapts clasp's basic solve algorithm
 * to a parallel solve algorithm that solves
 * a problem using a given number of threads.
 * It supports guiding path based solving, portfolio based solving, as well
 * as a combination of these two approaches.
 */
class ParallelSolve : public SolveAlgorithm {
public:
	//! Initializes this object for solving with at most ctx.numSolvers() threads.
	/*!
	 * \pre ctx.numSolvers() > 1
	 */
	ParallelSolve(SharedContext& ctx, const ParallelSolveOptions& opts);
	~ParallelSolve();
	// own interface

	//! Attaches a new thread to s and runs it in this algorithm.
	/*!
	 * Shall be called once for each solver except the master.
	 * \pre s.id() != Solver::invalidId
	 */
	void   addSolver(Solver& s, const SolveParams& p);
	
	//! Returns the number of active threads.
	uint32 numThreads()            const;
	bool   integrateUseHeuristic() const { return test_bit(intFlags_, 31); }
	uint32 integrateGrace()        const { return intGrace_; }
	uint32 integrateFlags()        const { return intFlags_; }
	bool   hasErrors()             const { return error_ != 0; }
	//! Terminates current solving process and all client threads.
	bool   terminate();
	//! Requests a global restart.
	void   requestRestart();
	bool   handleMessages(Solver& s);
	void   pushWork(LitVec& gp);
	enum GpType { gp_none = 0, gp_split = 1, gp_fixed = 2 };
private:
	ParallelSolve(const ParallelSolve&);
	ParallelSolve& operator=(const ParallelSolve&);
	typedef SingleOwnerPtr<const LitVec> PathPtr;
	enum ErrorCode { error_none = 0, error_oom = 1, error_runtime = 2, error_other = 4 };
	enum           { masterId = 0 };
	// -------------------------------------------------------------------------------------------
	// Thread setup 
	struct EntryPoint;
	void   destroyThread(uint32 id);
	void   allocThread(uint32 id, Solver& s, const SolveParams& p); 
	void   joinThreads();
	// -------------------------------------------------------------------------------------------
	// Algorithm steps
	void   setIntegrate(uint32 grace, uint8 filter);
	void   setRestarts(uint32 maxR, const ScheduleStrategy& rs);
	bool   doSolve(Solver& s, const SolveParams& p);
	void   solveParallel(uint32 id);
	bool   initOpt(Solver& s, ValueRep last);
	void   initQueue();
	bool   requestWork(Solver& s, PathPtr& out);
	bool   backtrackFromModel(Solver& s);
	void   terminate(Solver& s, bool complete);
	bool   waitOnSync(Solver& s);
	void   exception(uint32 id, PathPtr& path, ErrorCode e, const char* what);
	void   reportProgress(const SolveEvent& ev) const;
	// -------------------------------------------------------------------------------------------
	struct SharedData;
	// SHARED DATA
	SharedData*       shared_;       // Shared control data
	ParallelHandler** thread_;       // Thread-locl control data
	// READ ONLY
	uint32            maxRestarts_;  // disable global restarts once reached 
	uint32            intGrace_;     // grace period for clauses to integrate
	uint32            intFlags_;     // bitset controlling clause integration
	int               error_;
	GpType            initialGp_;
};

//! A per-solver (i.e. thread) class that implements message handling and knowledge integration.
/*!
 * The class adds itself as a post propagator to the given solver. Each time
 * propagateFixpoint() is called (i.e. on each new decision level), it checks
 * for new lemmas to integrate and synchronizes the search with any new models.
 * Furthermore, it adds a second (high-priority) post propagator for message handling.
 */
class ParallelHandler : public PostPropagator {
public:
	typedef ParallelSolve::GpType GpType;

	//! Creates a new parallel handler to be used in the given solve group.
	/*!
	 * \param ctrl The object controlling the parallel solve operation.
	 * \param s    The solver that is to be controlled by this object.
	 * \param p    The solver-specific solve options.
	 */
	explicit ParallelHandler(ParallelSolve& ctrl, Solver& s, const SolveParams& p);
	~ParallelHandler();
	//! Attaches the object's solver to ctx and adds this object as a post propagator.
	bool attach(SharedContext& ctx);
	//! Removes this object from the list of post propagators of its solver and detaches the solver from ctx.
	void detach(SharedContext& ctx, bool fastExit);

	void setError(int e) { error_ = e; }
	int  error() const   { return (int)error_; }
	void setWinner()     { win_ = 1; }
	bool winner() const  { return win_ != 0; }
	void setThread(std::thread& x) { assert(!joinable()); x.swap(thread_); assert(joinable()); }
	
	//! True if *this has an associated thread of execution, false otherwise.
	bool joinable() const { return thread_.joinable(); }
	//! Waits for the thread of execution associated with *this to finish.
	int join() { if (joinable()) { thread_.join(); } return error(); }
	
	// overridden methods
	
	//! Returns a priority suited for a post propagators that is non-deterministic.
	uint32 priority() const { return priority_general + 100; }

	//! Integrates new information.
	bool propagateFixpoint(Solver& s);
	bool propagate(Solver& s) { return ParallelHandler::propagateFixpoint(s); }
	void reset()              { up_ = 1; }
	//! Checks whether new information has invalidated current model.
	bool isModel(Solver& s);

	// own interface
	
	// TODO: make functions virtual once necessary 
	
	//! Returns true if handler's guiding path is disjoint from all others.
	bool disjointPath() const { return gp_.type == ParallelSolve::gp_split; }
	//! Returns true if handler has a guiding path.
	bool hasPath()      const { return gp_.type != ParallelSolve::gp_none; }
	void setGpType(GpType t)  { gp_.type = t; }
	
	//! Called before solver starts to solve given guiding path.
	/*!
	 * \param gp      The new guiding path.
	 * \param type    The guiding path's type.
	 * \param restart Request restart after restart number of conflicts.
	 */
	void prepareForGP(const LitVec& gp, GpType type, uint64 restart);

	/*!
	 * \name Message handlers
	 * \note 
	 *   Message handlers are intended as callbacks for ParallelSolve::handleMessages().
	 *   They shall not change the assignment of the solver object.
	 */
	//@{
	
	//! Algorithm is about to terminate.
	/*!
	 * Removes this object from the solver's list of post propagators.
	 */
	void handleTerminateMessage();

	//! Request for split.
	/*!
	 * Splits off a new guiding path and adds it to the control object.
	 * \pre The guiding path of this object is "splittable"
	 */
	void handleSplitMessage();

	//! Request for (global) restart.
	/*!
	 * \return true if restart is valid, else false.
	 */
	bool handleRestartMessage();

	SolveStats aggStats;  // aggregated statistics over all gps
	Solver&            solver()      { return *solver_; }
	const SolveParams& params() const{ return *params_; }
	//@}  
private:
	bool simplify(Solver& s, bool re);
	bool integrateClauses(Solver& s);
	void add(ClauseHead* h);
	void clearDB(Solver* s);
	ParallelSolve* ctrl() const { return messageHandler_.ctrl; }
	typedef LitVec::size_type size_type;
	typedef PodVector<Constraint*>::type ClauseDB;
	std::thread        thread_;     // active thread or empty for master
	ClauseDB           integrated_; // my integrated clauses
	Solver*            solver_;     // my solver
	const SolveParams* params_;     // my solving params
	size_type          intTail_;    // where to put next clause
	uint32             error_:30;   // error code or 0 if ok
	uint32             win_  : 1;
	uint32             up_   : 1;
	struct GP {
		LitVec      path;     // current guiding path
		uint64      restart;  // don't give up before restart number of conflicts
		size_type   pos;      // pos in trail
		uint32      impl;     // number of additional implied literals
		GpType      type ;    // type of gp
		void reset(uint64 r = UINT64_MAX, GpType t = ParallelSolve::gp_none) {
			path.clear();
			restart = r;
			pos     = 0;
			impl    = 0;
			type    = t;
		}
	} gp_;
	struct MessageHandler : PostPropagator {
		explicit MessageHandler(ParallelSolve* c) : ctrl(c) {}
		uint32 priority() const { return PostPropagator::priority_highest; }
		bool   propagateFixpoint(Solver& s) { return ctrl->handleMessages(s); }
		bool   propagate(Solver& s)         { return MessageHandler::propagateFixpoint(s); }
		ParallelSolve* ctrl; // get messages from here
	}    messageHandler_;
};

class GlobalQueue : public Distributor {
public:
	explicit GlobalQueue(uint32 maxShare, uint32 topo);
	~GlobalQueue();
	uint32  receive(const Solver& in, SharedLiterals** out, uint32 maxOut);
	void    publish(const Solver& source, SharedLiterals* n);
private:
	void release();
	struct DistPair {
		DistPair(uint32 sId = UINT32_MAX, SharedLiterals* x = 0) : sender(sId), lits(x) {}
		uint32          sender;
		SharedLiterals* lits;
	};
	class Queue : public MultiQueue<DistPair> {
	public:
		typedef MultiQueue<DistPair> base_type;
		using base_type::publish;
		Queue(uint32 m) : base_type(m) {}
	};
	struct ThreadInfo {
		Queue::ThreadId id;
		uint64          peerMask;
		char pad[64 - sizeof(Queue::ThreadId)];
	};
	Queue::ThreadId& getThreadId(uint32 sId) const { return threadId_[sId].id; }
	uint64           getPeerMask(uint32 sId) const { return threadId_[sId].peerMask; }
	uint64           populatePeerMask(uint32 topo, uint32 maxT, uint32 id) const;
	uint64           populateFromCube(uint32 maxT, uint32 id, bool ext) const;
	Queue*           queue_;
	ThreadInfo*      threadId_;
};

} }
#endif

#endif
