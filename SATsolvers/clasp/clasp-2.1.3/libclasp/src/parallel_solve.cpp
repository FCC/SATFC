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
#if WITH_THREADS
#include <clasp/parallel_solve.h>
#include <clasp/solver.h>
#include <clasp/clause.h>
#include <clasp/clasp_facade.h>
#include <clasp/util/timer.h>
#include <clasp/minimize_constraint.h>
#include <clasp/util/mutex.h>
#include <tbb/concurrent_queue.h>

namespace Clasp { namespace mt {
/////////////////////////////////////////////////////////////////////////////////////////
// BarrierSemaphore
/////////////////////////////////////////////////////////////////////////////////////////
// A combination of a barrier and a semaphore
class BarrierSemaphore {
public:
	explicit BarrierSemaphore(int counter = 0, int maxParties = 1) : counter_(counter), active_(maxParties) {}
	// Initializes this object
	// PRE: no thread is blocked on the semaphore
	//      (i.e. internal counter is >= 0)
	// NOTE: not thread-safe
	void unsafe_init(int counter = 0, int maxParties = 1) {
		counter_ = counter;
		active_  = maxParties;
	}
	// Returns the current semaphore counter.
	int  counter()   { std::lock_guard<std::mutex> lock(semMutex_); return counter_; }
	// Returns the number of parties required to trip this barrier.
	int  parties()   { std::lock_guard<std::mutex> lock(semMutex_); return active_;  } 
	// Returns true if all parties are waiting at the barrier
	bool active()    { std::lock_guard<std::mutex> lock(semMutex_); return unsafe_active(); }
	
	// barrier interface
	
	// Increases the barrier count, i.e. the number of 
	// parties required to trip this barrier.
	void addParty() {
		std::lock_guard<std::mutex> lock(semMutex_);
		++active_;
	}
	// Decreases the barrier count and resets the barrier
	// if reset is true. 
	// PRE: the thread does not itself wait on the barrier
	void removeParty(bool reset) {
		std::unique_lock<std::mutex> lock(semMutex_);
		assert(active_ > 0);
		--active_;
		if      (reset)           { unsafe_reset(0); }
		else if (unsafe_active()) { counter_ = -active_; lock.unlock(); semCond_.notify_one(); }
	}
	// Waits until all parties have arrived, i.e. called wait.
	// Exactly one of the parties will receive a return value of true,
	// the others will receive a value of false.
	// Applications shall use this value to designate one thread as the
	// leader that will eventually reset the barrier thereby unblocking the other threads.
	bool wait() {
		std::unique_lock<std::mutex> lock(semMutex_);
		if (--counter_ >= 0) { counter_ = -1; }
		return unsafe_wait(lock);
	}
	// Resets the barrier and unblocks any waiting threads.
	void reset(uint32 semCount = 0) {
		std::lock_guard<std::mutex> lock(semMutex_);
		unsafe_reset(semCount);
	}
	// semaphore interface
	
	// Decrement the semaphore's counter.
	// If the counter is zero or less prior to the call
	// the calling thread is suspended.
	// Returns false to signal that all but the calling thread
	// are currently blocked.
	bool down() {
		std::unique_lock<std::mutex> lock(semMutex_);
		if (--counter_ >= 0) { return true; }
		return !unsafe_wait(lock);
	}
	// Increments the semaphore's counter and resumes 
	// one thread which has called down() if the counter 
	// was less than zero prior to the call.
	void up() {
		bool notify;
		{
			std::lock_guard<std::mutex> lock(semMutex_);
			notify    = (++counter_ < 1);
		}
		if (notify) semCond_.notify_one();
	}
private:
	BarrierSemaphore(const BarrierSemaphore&);
	BarrierSemaphore& operator=(const BarrierSemaphore&);
	typedef std::condition_variable cv;
	bool    unsafe_active() const { return -counter_ >= active_; }
	void    unsafe_reset(uint32 semCount) {
		int prev = counter_;
		counter_ = semCount;
		if (prev < 0) { semCond_.notify_all(); }
	}
	// Returns true for the leader, else false
	bool    unsafe_wait(std::unique_lock<std::mutex>& lock) {
		assert(counter_ < 0);
		// don't put the last thread to sleep!
		if (!unsafe_active()) {
			semCond_.wait(lock);
		}
		return unsafe_active();
	}	
	cv         semCond_;  // waiting threads
	std::mutex semMutex_; // mutex for updating counter
	int        counter_;  // semaphore's counter
	int        active_;   // number of active threads
};
/////////////////////////////////////////////////////////////////////////////////////////
// ParallelSolve::Impl
/////////////////////////////////////////////////////////////////////////////////////////
struct ParallelSolve::SharedData {
	typedef tbb::concurrent_queue<const LitVec*> queue;
	enum MsgFlag {
		terminate_flag        = 1u, sync_flag  = 2u,  split_flag    = 4u, 
		restart_flag          = 8u, unsat_flag = 16u, complete_flag = 32u,
		interrupt_flag        = 64u,  // set on terminate from outside
		allow_gp_flag         = 128u, // set if splitting mode is active
		forbid_restart_flag   = 256u, // set if restarts are no longer allowed
		cancel_restart_flag   = 512u, // set if current restart request was cancelled by some thread
		restart_abandoned_flag=1024u  // set to signal that threads must not give up their gp
	};
	enum Message {
		msg_terminate      = (terminate_flag),
		msg_interrupt      = (terminate_flag | interrupt_flag),
		msg_sync_restart   = (sync_flag | restart_flag),
		msg_sync_unsat     = (sync_flag | unsat_flag | restart_flag),
		msg_split          = split_flag
	};
	SharedData() : path(0) { reset(0); }
	void reset(SharedContext* a_ctx) {
		clearQueue();
		syncT.reset();
		workSem.unsafe_init(0, a_ctx ? a_ctx->numSolvers() : 0);
		globalR     = ScheduleStrategy::none();
		maxConflict = globalR.current();
		ctx         = a_ctx;
		path        = 0;
		nextId      = 1;
		workReq     = 0;
		restartReq  = 0;
		control     = 0;
	}
	void clearQueue() {
		for (const LitVec* a = 0; workQ.try_pop(a); ) {
			if (a != path) { delete a; }
		}
	}
	Enumerator* enumerator()  const { return ctx->enumerator(); }
	// MESSAGES
	bool        hasMessage()  const { return (control & uint32(7)) != 0; }
	bool        synchronize() const { return (control & uint32(sync_flag))      != 0; }
	bool        terminate()   const { return (control & uint32(terminate_flag)) != 0; }
	bool        split()       const { return (control & uint32(split_flag))     != 0; }
	bool        postMessage(Message m, bool notify);
	void        aboutToSplit()      { if (--workReq == 0) updateSplitFlag();  }
	void        updateSplitFlag();
	// CONTROL FLAGS
	bool        hasControl(uint32 f) const { return (control & f) != 0;        }
	bool        interrupt()          const { return hasControl(interrupt_flag);}
	bool        complete()           const { return hasControl(complete_flag); }
	bool        restart()            const { return hasControl(restart_flag);  }
	bool        allowSplit()         const { return hasControl(allow_gp_flag); }
	bool        allowRestart()       const { return !hasControl(forbid_restart_flag); }
	bool        setControl(uint32 flags)   { return (fetch_and_or(control, flags) & flags) != flags; }
	bool        clearControl(uint32 flags) { return (fetch_and_and(control, ~flags) & flags) == flags; }
	ScheduleStrategy    globalR;     // global restart strategy
	uint64              maxConflict; // current restart limit
	SharedContext*      ctx;         // shared context object
	const LitVec*       path;        // initial guiding path - typically empty
	Timer<RealTime>     syncT;       // thread sync time
	std::mutex          modelM;      // model-mutex 
	BarrierSemaphore    workSem;     // work-semaphore
	queue               workQ;       // work-queue
	uint32              nextId;      // next solver id to use
	std::atomic<int>    workReq;     // > 0: someone needs work
	std::atomic<uint32> restartReq;  // == numThreads(): restart
	std::atomic<uint32> control;     // set of active message flags
};

// post message to all threads
bool ParallelSolve::SharedData::postMessage(Message m, bool notifyWaiting) {
	if (m == msg_split) {
		if (++workReq == 1) { updateSplitFlag(); }
		return true;
	}
	else if (setControl(m)) {
		// control message - notify all if requested
		if (notifyWaiting) workSem.reset();
		if ((uint32(m) & uint32(sync_flag)) != 0) {
			syncT.reset();
			syncT.start();
		}
		return true;
	}
	return false;
}

void ParallelSolve::SharedData::updateSplitFlag() {
	for (bool splitF;;) {
		splitF = (workReq > 0);	
		if (split() == splitF) return;
		if (splitF) fetch_and_or(control,   uint32(split_flag)); 
		else        fetch_and_and(control, ~uint32(split_flag));
	}
}
/////////////////////////////////////////////////////////////////////////////////////////
// ParallelSolve
/////////////////////////////////////////////////////////////////////////////////////////
ParallelSolve::ParallelSolve(SharedContext& ctx, const ParallelSolveOptions& opts)
	: SolveAlgorithm(opts.limit)
	, shared_(new SharedData)
	, thread_(0)
	, maxRestarts_(0)
	, intGrace_(1024)
	, intFlags_(ClauseCreator::clause_not_root_sat | ClauseCreator::clause_no_add)
	, error_(0)
	, initialGp_(opts.mode == ParallelSolveOptions::mode_split ? gp_split : gp_fixed) {
	assert(ctx.numSolvers() && "Illegal number of threads");
	shared_->reset(&ctx);
	shared_->setControl(opts.mode == ParallelSolveOptions::mode_split ? SharedData::allow_gp_flag : SharedData::forbid_restart_flag);
	shared_->setControl(SharedData::sync_flag); // force initial sync with all threads
	setRestarts(opts.restarts.maxR, opts.restarts.sched);
	setIntegrate(opts.integrate.grace, opts.integrate.filter);
	if (ctx.distribution()) {
		ctx.setDistributor(new mt::GlobalQueue(ctx.numSolvers(), opts.integrate.topo));
	}
	ctx.master()->stats.enableParallelStats();
}

ParallelSolve::~ParallelSolve() {
	if (shared_->nextId > 1) {
		// algorithm was not started but there may be active threads -
		// force orderly shutdown
		ParallelSolve::terminate();
		shared_->workSem.removeParty(true);
		joinThreads();
	}
	destroyThread(masterId);
	delete shared_;
}

void ParallelSolve::setIntegrate(uint32 grace, uint8 filter) {
	typedef ParallelSolveOptions::Integration Dist;
	intGrace_     = grace;
	intFlags_     = ClauseCreator::clause_no_add;
	if (filter == Dist::filter_heuristic) { store_set_bit(intFlags_, 31); }
	if (filter != Dist::filter_no)        { intFlags_ |= ClauseCreator::clause_not_root_sat; }
	if (filter == Dist::filter_sat)       { intFlags_ |= ClauseCreator::clause_not_sat; }
}

void ParallelSolve::setRestarts(uint32 maxR, const ScheduleStrategy& rs) {
	maxRestarts_         = maxR;
	shared_->globalR     = maxR ? rs : ScheduleStrategy::none();
	shared_->maxConflict = shared_->globalR.current();
}

void ParallelSolve::addSolver(Solver& s, const SolveParams& p) {
	if (&s != shared_->ctx->master()) {
		uint32 id = shared_->nextId++;
		assert(id < shared_->ctx->numSolvers() && id != masterId);
		s.setId(id);
		s.stats.enableStats(shared_->ctx->master()->stats);
		allocThread(id, s, p);
		// start in new thread
		std::thread x(std::mem_fun(&ParallelSolve::solveParallel), this, id);
		thread_[id]->setThread(x);
	}
}

uint32 ParallelSolve::numThreads() const { return shared_->workSem.parties(); }

void ParallelSolve::allocThread(uint32 id, Solver& s, const SolveParams& p) {
	if (!thread_) {
		uint32 n = numThreads();
		thread_  = new ParallelHandler*[n];
		std::fill(thread_, thread_+n, static_cast<ParallelHandler*>(0));
	}
	#pragma message TODO("replace with CACHE_LINE_ALIGNED alloc")
	uint32 b   = ((sizeof(ParallelHandler)+63) / 64) * 64;
	thread_[id]= new (::operator new( b )) ParallelHandler(*this, s, p);
}

void ParallelSolve::destroyThread(uint32 id) {
	if (thread_ && thread_[id]) {
		assert(!thread_[id]->joinable() && "Shutdown not completed!");
		thread_[id]->~ParallelHandler();
		::operator delete(thread_[id]);
		thread_[id] = 0;
		if (id == masterId) {
			delete [] thread_;
			thread_ = 0;
		}
	}
}

inline void ParallelSolve::reportProgress(const SolveEvent& ev) const {
	return shared_->ctx->reportProgress(ev);
}

// joins with and destroys all active threads
void ParallelSolve::joinThreads() {
	shared_->syncT.reset();
	shared_->syncT.start();
	reportProgress(SolveStateEvent(thread_[masterId]->solver(), "shutdown"));
	error_        = thread_[masterId]->error();
	uint32 winner = thread_[masterId]->winner() ? uint32(masterId) : UINT32_MAX;
	for (uint32 i = 1, end = shared_->nextId; i != end; ++i) {
		if (thread_[i]->join() > error_) {
			error_ = thread_[i]->error();
		}
		if (thread_[i]->winner() && i < winner) {
			winner = i;
		}
		destroyThread(i);
	}
	thread_[masterId]->setError(!shared_->interrupt() ? thread_[masterId]->error() : error_);
	shared_->ctx->setWinner(winner);
	shared_->nextId = 1;
	shared_->syncT.stop();
	reportProgress(SolveStateEvent(thread_[masterId]->solver(), "shutdown", shared_->syncT.total()));
}

bool ParallelSolve::initOpt(Solver& s, ValueRep last) {
	return last != value_true
	  ||   shared_->enumerator()->continueFromModel(s, false);
}

// Entry point for master solvers
bool ParallelSolve::doSolve(Solver& s, const SolveParams& p) {
	assert(shared_->ctx->master() == &s);
	// explicity init parallel handler because Solver::endInit() was
	// already called.
	allocThread(masterId, s, p);
	thread_[masterId]->init(s);
	shared_->path = &getInitialPath();
	shared_->syncT.start();
	solveParallel(masterId);
	joinThreads(); 
	s.stats.parallel->cpuTime = ThreadTime::getTime();
	switch(thread_[masterId]->error()) {
		case error_none   : break;
		case error_oom    : throw std::bad_alloc();
		case error_runtime: throw std::runtime_error("RUNTIME ERROR!");
		default:            throw std::runtime_error("UNKNOWN ERROR!");
	}
	return !shared_->complete();
}

// main solve loop executed by all threads
void ParallelSolve::solveParallel(uint32 id) {
	Solver& s           = thread_[id]->solver();
	const SolveParams& p= thread_[id]->params();
	InitParams init     = p.init;
	SolveLimits lim     = getSolveLimits();
	PathPtr a(0);
	Timer<RealTime> tt; tt.start();
	try {
		// establish solver<->handler connection and attach to shared context
		// should this fail because of an initial conflict, we'll terminate
		// in requestWork.
		thread_[id]->attach(*shared_->ctx);
		reportProgress(SolveStateEvent(s, "algorithm"));
		for (ValueRep last = value_free; requestWork(s, a);) {
			thread_[s.id()]->prepareForGP(*a, a.is_owner() ? gp_split : initialGp_, shared_->maxConflict);
			s.stats.reset();
			if (initOpt(s, last) && initPath(s, *a, init)) {
				if ((last = solvePath(s, p, lim)) == value_free) { terminate(s, false); }
				s.clearStopConflict();
			}
		}
	}
	catch (const std::bad_alloc&)      { exception(id,a,error_oom, "ERROR: std::bad_alloc" ); }
	catch (const std::runtime_error& e){ exception(id,a,error_runtime, e.what()); }
	catch (...)                        { exception(id,a,error_other, "ERROR: unknown");  }
	assert(shared_->terminate() || thread_[id]->error() != error_none);
	tt.stop();
	reportProgress(SolveStateEvent(s, "algorithm", tt.total()));
	// remove solver<->handler connection and detach from shared context
	thread_[id]->detach(*shared_->ctx, shared_->interrupt());
	// this thread is leaving
	shared_->workSem.removeParty(shared_->terminate());
}

void ParallelSolve::exception(uint32 id, PathPtr& path, ErrorCode e, const char* what) {
	try {
		reportProgress(SolveMsgEvent(thread_[id]->solver(), what));
		thread_[id]->setError(e);
		if (id == masterId || shared_->workSem.active()) { 
			ParallelSolve::terminate();
			return;
		}
		else if (path.get() && thread_[id]->disjointPath()) {
			shared_->workQ.push(path.release());
			shared_->workSem.up();
		}
	}
	catch(...) { terminate(); }
}

// forced termination from outside
bool ParallelSolve::terminate() {
	// do not notify blocked threads to avoid possible
	// deadlock in semaphore!
	shared_->postMessage(SharedData::msg_interrupt, false);
	// notify any thread currently in Enumerator::backtrackFromModel()
	shared_->enumerator()->terminate();
	return true;
}

// tries to get new work for the given solver
bool ParallelSolve::requestWork(Solver& s, PathPtr& out) { 
	const LitVec* a = 0;
	while (!shared_->terminate()) {
		// only clear path and stop conflict - we don't propagate() here
		// because we would then have to handle any eventual conflicts
		if (!s.popRootLevel(s.rootLevel())) {
			// s has a real top-level conflict - problem is unsat
			terminate(s, true);
		}	
		else if (a || shared_->workQ.try_pop(a)) {
			assert(s.decisionLevel() == 0);
			// got new work from work-queue
			out = a;
			// do not take over ownership of initial gp!
			if (a == shared_->path) { out.release(); } 
			// propagate any new facts before starting new work
			if (s.simplify())       { return true; }
			// s now has a conflict - either an artifical stop conflict
			// or a real conflict - we'll handle it in the next iteration
			// via the call to popRootLevel()
		}
		else if (shared_->synchronize()) {
			// a synchronize request is active - we are fine with
			// this but did not yet had a chance to react on it
			waitOnSync(s);
		}
		else if (shared_->allowSplit()) {
			// gp mode is active - request a split	
			// and wait until someone has work for us
			shared_->postMessage(SharedData::msg_split, false);
			if (!shared_->workSem.down() && !shared_->synchronize()) {
				// we are the last man standing, there is no
				// work left - quitting time?
				terminate(s, true);
			}
		}
		else {
			// portfolio mode is active - no splitting, no work left
			// quitting time? 
			terminate(s, true);
		}
	}
	return false;
}

// terminated from inside of algorithm
// check if there is more to do
void ParallelSolve::terminate(Solver& s, bool complete) {
	if (!shared_->terminate()) {
		if (complete && shared_->enumerator()->optimizeHierarchical() && s.popRootLevel(s.rootLevel())) {
			// Problem is unsat and hierarchical optimization is active.
			// The active level is at its optimum, but lower-priority levels might
			// still be non-optimal.
			// Notify other threads to prepare for solving next priority level.
			assert(s.decisionLevel() == 0);
			complete = false;
			shared_->postMessage(SharedData::msg_sync_unsat, true);
		}
		else {
			shared_->postMessage(SharedData::msg_terminate, true);
			thread_[s.id()]->setWinner();
			if (complete) { shared_->setControl(SharedData::complete_flag); }
		}
	}
}

// handles an active synchronization request
// returns true to signal that s should restart otherwise false
bool ParallelSolve::waitOnSync(Solver& s) {
	if (!thread_[s.id()]->handleRestartMessage()) {
		shared_->setControl(SharedData::cancel_restart_flag);
	}
	bool hasPath = thread_[s.id()]->hasPath();
	reportProgress(SolveStateEvent(s, "synchronization"));
	if (shared_->workSem.wait()) {
		// last man standing - complete synchronization request
		shared_->workReq     = 0;
		shared_->restartReq  = 0;
		bool unsat           = shared_->hasControl(SharedData::unsat_flag);
		bool restart         = !unsat  && shared_->hasControl(SharedData::restart_flag);
		bool init            = true;
		if (restart) {
			init = shared_->allowRestart() && !shared_->hasControl(SharedData::cancel_restart_flag);
			if (init) { shared_->globalR.next(); }
			shared_->maxConflict = shared_->allowRestart() && shared_->globalR.idx < maxRestarts_
				? shared_->globalR.current()
				: UINT64_MAX;
		}
		else if (unsat && !shared_->enumerator()->optimizeNext()) {
			// nothing more to do
			init = false;
			shared_->setControl(SharedData::terminate_flag | SharedData::complete_flag);
		}
		else if (shared_->maxConflict != UINT64_MAX && !shared_->allowRestart()) {
			shared_->maxConflict = UINT64_MAX;
		}
		// add initial path to work queue
		if (init) { initQueue();  }
		else      { shared_->setControl(SharedData::restart_abandoned_flag); }
		shared_->clearControl(SharedData::msg_split | SharedData::msg_sync_unsat | SharedData::msg_sync_restart | SharedData::restart_abandoned_flag | SharedData::cancel_restart_flag);
		shared_->syncT.stop();
		reportProgress(SolveStateEvent(s, (unsat ? "unsat-sync" : "restart-sync"), shared_->syncT.total()));
		// wake up all blocked threads
		shared_->workSem.reset();
	}
	return shared_->terminate() || (hasPath && !shared_->hasControl(SharedData::restart_abandoned_flag));
}

// If guiding path scheme is active only one
// thread can start with gp (typically empty) and this
// thread must then split up search-space dynamically.
// Otherwise, all threads can start with initial gp
// TODO:
//  heuristic for initial splits?
void ParallelSolve::initQueue() {
	shared_->clearQueue();
	int end = shared_->hasControl(SharedData::allow_gp_flag) ? 1 : numThreads();
	assert(end == 1 || shared_->hasControl(SharedData::forbid_restart_flag));
	for (int i = 0; i != end; ++i) {
		shared_->workQ.push(shared_->path);
	}
}

// adds work to the work-queue
void ParallelSolve::pushWork(LitVec& work) { 
	LitVec* v = new LitVec;
	v->swap(work);
	shared_->workQ.push(v);
	shared_->workSem.up();
}

// called whenever some solver has found a model
bool ParallelSolve::backtrackFromModel(Solver& s) { 
	Enumerator::Result r;
	{
		// grab lock - models must be processed sequentially
		// in order to simplify printing and to avoid duplicates
		// in all non-trivial enumeration modes
		std::lock_guard<std::mutex> lock(shared_->modelM);
		// first check if the model is still valid once all
		// information is integrated into the solver
		if (shared_->terminate() || !thread_[s.id()]->isModel(s)) {
			// model no longer a (unique) model or enough models enumerated
			return !shared_->terminate(); // continue search?
		}
		r = shared_->enumerator()->backtrackFromModel(s, false);
		if (r == Enumerator::enumerate_stop_enough || (r == Enumerator::enumerate_stop_complete && s.decisionLevel() == 0)) {
			// must be called while holding the lock - otherwise
			// we have a race condition with solvers that
			// are currently blocking on the mutex and we could enumerate 
			// more models than requested by the user
			terminate(s, s.decisionLevel() == 0);
		}
		if (shared_->enumerator()->enumerated == 1 && !shared_->enumerator()->supportsRestarts()) {
			// switch to backtracking based splitting algorithm
			shared_->setControl(SharedData::forbid_restart_flag | SharedData::allow_gp_flag);
			// declare the solver's gp as a valid candidate for splitting
			thread_[s.id()]->setGpType(gp_split); 
		}
	}
	// continue only after we released the mutex to avoid deadlock -
	// continueFromModel(s) might call s.propagate() which could
	// block on a sync message.
	if (!shared_->enumerator()->continueFromModel(s)) {
		r = Enumerator::enumerate_stop_complete;
	}
	return r == Enumerator::enumerate_continue && !shared_->terminate();
}

// updates s with new messages and uses s to create a new guiding path
// if necessary and possible
bool ParallelSolve::handleMessages(Solver& s) {
	// check if there are new messages for s
	if (!shared_->hasMessage()) {
		// nothing to do
		return true; 
	}
	uint32 hId         = s.id();
	ParallelHandler* h = thread_[hId];
	if (shared_->terminate()) {
		reportProgress(SolveMsgEvent(s, "TERMINATE message received"));
		h->handleTerminateMessage();
		s.setStopConflict();
		return false;
	}
	if (shared_->synchronize()) {
		if (waitOnSync(s)) {
			s.setStopConflict();
			return false;
		}
		return true;
	}
	if (h->disjointPath() && s.splittable() && shared_->workReq > 0) {
		// First declare split request as handled
		// and only then do the actual split.
		// This way, we minimize the chance for 
		// "over"-splitting, i.e. one split request handled
		// by more than one thread.
		shared_->aboutToSplit();
		h->handleSplitMessage();
	}
	return true;
}

void ParallelSolve::requestRestart() {
	if (shared_->allowRestart() && ++shared_->restartReq == numThreads()) {
		shared_->postMessage(SharedData::msg_sync_restart, true);
	}
}

void ParallelSolveOptions::createSolveObject(SolveAlgorithm*& out, SharedContext& ctx, SolverConfig** sc) const {
	if (ctx.numSolvers() > 1) {
		ParallelSolve* x = new ParallelSolve(ctx, *this);
		out = x;
		for (uint32 i = 1; i != ctx.numSolvers(); ++i) {
			x->addSolver(*sc[i]->solver, sc[i]->params);
		}
	}
	else { out = new SimpleSolve(limit); }
}
////////////////////////////////////////////////////////////////////////////////////
// ParallelHandler
/////////////////////////////////////////////////////////////////////////////////////////
ParallelHandler::ParallelHandler(ParallelSolve& ctrl, Solver& s, const SolveParams& p) 
	: solver_(&s)
	, params_(&p)
	, intTail_(0)
	, error_(0)
	, win_(0)
	, messageHandler_(&ctrl) {
	this->next = this;
}

ParallelHandler::~ParallelHandler() { 
	clearDB(0); 
}

// adds this as post propagator to its solver and attaches the solver to ctx.
bool ParallelHandler::attach(SharedContext& ctx) {
	assert(solver_ && params_);
	aggStats.reset();
	aggStats.enableStats(solver_->stats);
	gp_.reset();
	gp_.impl= UINT32_MAX;
	intTail_= 0;
	error_  = 0;
	win_    = 0;
	up_     = 1;
	next    = 0;
	solver_->addPost(&messageHandler_);
	solver_->addPost(this);
	return ctx.attach(*solver_);
}

// removes this from the list of post propagators of its solver and detaches the solver from ctx.
void ParallelHandler::detach(SharedContext& ctx, bool fastExit) {
	handleTerminateMessage();
	if (solver_->sharedContext() == &ctx) {
		if (error() == 0 && !fastExit) {
			clearDB(solver_);
			solver_->clearAssumptions();
		}
		aggStats.accu(solver_->stats);
		ctx.detach(*solver_);
		if (error()) { 
			clearDB(0);
			solver_->reset();
			solver_->setId(Solver::invalidId); 
		}
		solver_->stats.swapStats(aggStats);
		solver_->stats.parallel->cpuTime = ThreadTime::getTime();
	}
}

void ParallelHandler::clearDB(Solver* s) {
	for (ClauseDB::iterator it = integrated_.begin(), end = integrated_.end(); it != end; ++it) {
		ClauseHead* c = static_cast<ClauseHead*>(*it);
		if (s && c->locked(*s)) { s->addLearnt(c, c->size(), Constraint_t::learnt_other); }
		else                    { c->destroy(s, s != 0); }
	}
	integrated_.clear();
	intTail_= 0;
}

void ParallelHandler::prepareForGP(const LitVec& out, GpType t, uint64 restart) {
	gp_.reset(restart, t);
	aggStats.parallel->newGP(out.size());
	aggStats.accu(solver_->stats);
}

// detach from solver, i.e. ignore any further messages 
void ParallelHandler::handleTerminateMessage() {
	if (this->next != this) {
		// mark removed propagators by creating "self-loop"
		solver_->removePost(&messageHandler_);
		messageHandler_.next = &messageHandler_;
		solver_->removePost(this);
		this->next = this;
	}
}

// split-off new guiding path and add it to solve object
void ParallelHandler::handleSplitMessage() {
	assert(solver_ && "ParallelHandler::handleSplitMessage(): not attached!");
	Solver& s = *solver_;
	s.updateGuidingPath(gp_.path, gp_.pos, gp_.impl);
	LitVec newGP(gp_.path);
	s.pushRootLevel();
	newGP.push_back(~s.decision(s.rootLevel()));
	++s.stats.parallel->splits;
	ctrl()->pushWork(newGP);
}

bool ParallelHandler::handleRestartMessage() {
	// TODO
	// we may want to implement some heuristic, like
	// computing a local var order. 
	return true;
}

bool ParallelHandler::simplify(Solver& s, bool sh) {
	ClauseDB::size_type i, j, end = integrated_.size();
	for (i = j = 0; i != end; ++i) {
		Constraint* c = integrated_[i];
		if (c->simplify(s, sh)) { 
			c->destroy(&s, false); 
			intTail_ -= (i < intTail_);
		}
		else                    { 
			integrated_[j++] = c;  
		}
	}
	shrinkVecTo(integrated_, j);
	if (intTail_ > integrated_.size()) intTail_ = integrated_.size();
	return false;
}

bool ParallelHandler::integrateClauses(Solver& s) {
	assert(!s.hasConflict() && &s == solver_);
	SharedLiterals* buffer[30];
	uint32          rec = s.sharedContext()->receive(s, buffer, 30);
	if (rec != 0) {
		uint32 intFlags   = ctrl()->integrateFlags();
		if (s.strategies().updateLbd || params_->reduce.strategy.glue != 0) {
			intFlags |= ClauseCreator::clause_int_lbd;
		}
		ClauseCreator::Result ret; uint32 added = 0;
		for (uint32 i = 0; i != rec;) {
			uint32 DL = s.decisionLevel();
			ret       = ClauseCreator::integrate(s, buffer[i++], intFlags, Constraint_t::learnt_other);
			added    += ret.status != ClauseCreator::status_subsumed; 
			if (ret.local)  { add(ret.local); }
			if (!ret.ok())  { while (i != rec) { buffer[i++]->release(); } break; }
			if (ret.unit()) { s.stats.addIntegratedAsserting(DL, s.decisionLevel()); }
		}
		s.stats.addIntegrated(added);
		return ret.ok();
	}
	return true;
}

bool ParallelHandler::propagateFixpoint(Solver& s) {
	// Periodically check for updates from new models.
	// Skip update while assumption literal is not yet assigned.
	// This is necessary during hierarchical optimization because otherwise
	// integrating a too strong optimum might irrevocably force the assumption literal
	// which would defeat its purpose.
	if (up_ == 1 || s.decisionLevel() == s.rootLevel()) {
		uint32 upMode = s.sharedContext()->updateMode();
		if (hasPath() && s.isTrue(s.sharedContext()->tagLiteral()) && (upMode == 1 || (s.stats.choices & 63) == 0)) {
			if (!s.sharedContext()->enumerator()->update(s, disjointPath())) {
				return false;
			}
			if (s.queueSize() != 0 && !s.propagateUntil(this)) {
				return false;
			}
		}
		for (;;) {
			if (!integrateClauses(s))    return false;
			if (s.queueSize() == 0)      break;
			if (!s.propagateUntil(this)) return false;
		}
		if (s.stats.conflicts >= gp_.restart) {
			ctrl()->requestRestart();
			gp_.restart *= 2;
		}
		up_ ^= upMode;
	}
	return true;
}

// checks whether s still has a model once all 
// information from previously found models were integrated 
bool ParallelHandler::isModel(Solver& s) {
	assert(s.numFreeVars() == 0);
	// either no unprocessed updates or still a model after
	// updates were integrated
	return s.sharedContext()->enumerator()->update(s, disjointPath())
		&& s.numFreeVars() == 0
		&& s.queueSize()   == 0;
}

void ParallelHandler::add(ClauseHead* h) {
	if (intTail_ < integrated_.size()) {
		ClauseHead* o = (ClauseHead*)integrated_[intTail_];
		integrated_[intTail_] = h;
		assert(o);
		if (!ctrl()->integrateUseHeuristic() || o->locked(*solver_) || o->activity().activity() > 0) {
			solver_->addLearnt(o, o->size(), Constraint_t::learnt_other);
		}
		else {
			o->destroy(solver_, true);
			solver_->stats.removeIntegrated();
		}
	}
	else {
		integrated_.push_back(h);
	}
	if (++intTail_ >= ctrl()->integrateGrace()) {
		intTail_ = 0;
	}
}

/////////////////////////////////////////////////////////////////////////////////////////
// GlobalQueue
/////////////////////////////////////////////////////////////////////////////////////////
GlobalQueue::GlobalQueue(uint32 maxT, uint32 topo) : Distributor(), queue_(0) {
	assert(maxT < ParallelSolveOptions::supportedSolvers());
	queue_     = new Queue(maxT);
	threadId_  = new ThreadInfo[maxT];
	for (uint32 i = 0; i != maxT; ++i) {
		threadId_[i].id       = queue_->addThread();
		threadId_[i].peerMask = populatePeerMask(topo, maxT, i);
	}
}
GlobalQueue::~GlobalQueue() {
	release();
}
void GlobalQueue::release() {
	if (queue_) {
		for (uint32 i = 0; i != queue_->maxThreads(); ++i) {
			Queue::ThreadId& id = getThreadId(i);
			for (DistPair n; queue_->tryConsume(id, n); ) { 
				if (n.sender != i) { n.lits->release(); }
			}
		}
		delete queue_;
		queue_ = 0;
		delete [] threadId_;
	}
}
uint64 GlobalQueue::populateFromCube(uint32 numThreads, uint32 myId, bool ext) const {
	uint32 n = numThreads;
	uint32 k = 1;
	for (uint32 i = n / 2; i > 0; i /= 2, k *= 2) { }
	uint64 res = 0, x = 1;
	for (uint32 m = 1; m <= k; m *= 2) {
		uint32 i = m ^ myId;
		if      (i < n)         { res |= (x << i);     }
		else if (ext && k != m) { res |= (x << (i^k)); }
	}
	if (ext) {
		uint32 s = k ^ myId;
		for(uint32 m = 1; m < k && s >= n; m *= 2) {
			uint32 i = m ^ s;
			if (i < n) { res |= (x << i); }
		}
	}
	assert( (res & (x<<myId)) == 0 );
	return res;
}
uint64 GlobalQueue::populatePeerMask(uint32 topo, uint32 maxT, uint32 id) const {
	switch (topo) {
		case ParallelSolveOptions::Integration::topo_ring: {
			uint32 prev = id > 0 ? id - 1 : maxT - 1;
			uint32 next = (id + 1) % maxT;
			return Distributor::mask(prev) | Distributor::mask(next);
		}
		case ParallelSolveOptions::Integration::topo_cube:  return populateFromCube(maxT, id, false);
		case ParallelSolveOptions::Integration::topo_cubex: return populateFromCube(maxT, id, true);
		default:                                     return Distributor::initSet(maxT) ^ Distributor::mask(id);
	}
}

void GlobalQueue::publish(const Solver& s, SharedLiterals* n) {
	assert(n->refCount() >= (queue_->maxThreads()-1));
	queue_->publish(DistPair(s.id(), n), getThreadId(s.id()));
}
uint32 GlobalQueue::receive(const Solver& in, SharedLiterals** out, uint32 maxn) {
	uint32 r = 0;
	Queue::ThreadId& id = getThreadId(in.id());
	uint64 peers = getPeerMask(in.id());
	for (DistPair n; r != maxn && queue_->tryConsume(id, n); ) {
		if (n.sender != in.id()) {
			if (inSet(peers, n.sender))  { out[r++] = n.lits; }
			else if (n.lits->size() == 1){ out[r++] = n.lits; }
			else                         { n.lits->release(); }
		}
	}
	return r;
}
} } // namespace Clasp::mt

#endif
