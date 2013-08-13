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
#include <clasp/solve_algorithms.h>
#include <clasp/solver.h>
#include <clasp/enumerator.h>
#include <clasp/lookahead.h>
#include <clasp/util/timer.h>
#include <cmath>
namespace Clasp { 
ProgressReport::ProgressReport()  {}
ProgressReport::~ProgressReport() {}
/////////////////////////////////////////////////////////////////////////////////////////
// SolveParams & SolverConfig
/////////////////////////////////////////////////////////////////////////////////////////
SolveParams::SolveParams() 
	: randFreq_(0.0) {
}
void SolverConfig::initFrom(const SolverConfig& other) {
	solver->strategies() = other.solver->strategies();
	params               = other.params;
}
/////////////////////////////////////////////////////////////////////////////////////////
// RestartParams
/////////////////////////////////////////////////////////////////////////////////////////
void RestartParams::disable() {
	sched         = ScheduleStrategy::none();
	std::memset(&counterRestart, 0, sizeof(uint64));
}
/////////////////////////////////////////////////////////////////////////////////////////
// ReduceParams
/////////////////////////////////////////////////////////////////////////////////////////
uint32 ReduceParams::getLimit(uint32 base, double f, const Range32& r) {
	base = (f != 0.0 ? (uint32)std::min(base*f, double(UINT32_MAX)) : UINT32_MAX);
	return r.clamp( base );
}
uint32 ReduceParams::getBase(const SharedContext& ctx) const {
	return strategy.estimate ? ctx.stats().complexity : ctx.stats().size; 
}
void ReduceParams::disable() {
	cflSched  = ScheduleStrategy::none();
	growSched = ScheduleStrategy::none();
	strategy.fReduce = 0;
	fGrow     = 0.0f; fInit = 0.0f; fMax = 0.0f;
	initRange = Range<uint32>(UINT32_MAX, UINT32_MAX); 
	maxRange  = UINT32_MAX;
}
/////////////////////////////////////////////////////////////////////////////////////////
// Schedule
/////////////////////////////////////////////////////////////////////////////////////////
double growR(uint32 idx, double g)       { return pow(g, (double)idx); }
double addR(uint32 idx, double a)        { return a * idx; }
uint32 lubyR(uint32 idx)                 {
	uint32 i = idx + 1;
	while ((i & (i+1)) != 0) {
		i    -= ((1u << log2(i)) - 1);
	}
	return (i+1)>>1;
}
ScheduleStrategy::ScheduleStrategy(Type t, uint32 b, double up, uint32 lim)
	: base(b), type(t), idx(0), len(lim), grow(0.0)  {
	if      (t == geometric_schedule)  { grow = static_cast<float>(std::max(1.0, up)); }
	else if (t == arithmetic_schedule) { grow = static_cast<float>(std::max(0.0, up)); }
	else if (t == user_schedule)       { grow = static_cast<float>(std::max(0.0, up)); }
	else if (t == luby_schedule && lim){ len  = std::max(uint32(2), (static_cast<uint32>(std::pow(2.0, std::ceil(log(double(lim))/log(2.0)))) - 1)*2); }
}

uint64 ScheduleStrategy::current() const {
	enum { t_add = ScheduleStrategy::arithmetic_schedule, t_luby = ScheduleStrategy::luby_schedule };
	if      (base == 0)     return UINT64_MAX;
	else if (type == t_add) return static_cast<uint64>(addR(idx, grow)  + base);
	else if (type == t_luby)return static_cast<uint64>(lubyR(idx)) * base;
	uint64 x = static_cast<uint64>(growR(idx, grow) * base);
	return x + !x;
}
uint64 ScheduleStrategy::next() {
	if (++idx != len) { return current(); }
	// length reached or overflow
	len = (len + !!idx) << uint32(type == luby_schedule);
	idx = 0;
	return current();
}
/////////////////////////////////////////////////////////////////////////////////////////
// solve
/////////////////////////////////////////////////////////////////////////////////////////
bool solve(SharedContext& ctx, const SolveParams& p, const SolveLimits& lim) {
	return SimpleSolve(lim).solve(ctx, p, LitVec());
}

bool solve(SharedContext& ctx, const SolveParams& p, const LitVec& assumptions, const SolveLimits& lim) {
	return SimpleSolve(lim).solve(ctx, p, assumptions);
}
/////////////////////////////////////////////////////////////////////////////////////////
// SolveAlgorithm
/////////////////////////////////////////////////////////////////////////////////////////
SolveAlgorithm::SolveAlgorithm(const SolveLimits& lim) : limits_(lim)  {
}
SolveAlgorithm::~SolveAlgorithm() {}

bool SolveAlgorithm::backtrackFromModel(Solver& s) { 
	return s.sharedContext()->enumerator()->backtrackFromModel(s) == Enumerator::enumerate_continue;
}

bool SolveAlgorithm::solve(SharedContext& ctx, const SolveParams& p, const LitVec& assume) {
	assumptions_ = assume;
	if (!isSentinel(ctx.tagLiteral())) { assumptions_.push_back(ctx.tagLiteral()); }
	bool more = limits_.conflicts == 0 || doSolve(*ctx.master(), p);
	ctx.enumerator()->reportResult(!more);
	if (!isSentinel(ctx.tagLiteral())) { assumptions_.pop_back(); }
	ctx.detach(*ctx.master());
	return more;
}
bool SolveAlgorithm::initPath(Solver& s, const LitVec& path, InitParams& params) {
	assert(!s.hasConflict() && s.decisionLevel() == 0);
	SingleOwnerPtr<Lookahead> look(0);
	if (params.lookType != Lookahead::no_lookahead && params.lookOps != 0) {
		look = new Lookahead(static_cast<Lookahead::Type>(params.lookType));
		look->init(s);
		s.addPost(look.release());
		--params.lookOps;
	}
	bool ok = s.propagate() && s.simplify();
	if (look.get()) { 
		s.removePost(look.get());
		look = look.get(); // restore ownership
	}
	if (!ok) { return false; }
	// setup path
	for (LitVec::size_type i = 0, end = path.size(); i != end; ++i) {
		Literal p = path[i];
		if (s.value(p.var()) == value_free) {
			s.assume(p); --s.stats.choices;
			// increase root level - assumption can't be undone during search
			s.pushRootLevel();
			if (!s.propagate())  return false;
		}
		else if (s.isFalse(p)) return false;
	}
	// do random probings if any
	if (uint32 i = params.randRuns) {
		params.randRuns = 0;
		do {
			if (s.search(params.randConf, UINT32_MAX, false, 1.0) != value_free) { return !s.hasConflict(); }
			s.undoUntil(0);
		} while (--i);
	}
	// do initial lookahead choices if requested
	if (uint32 i = params.lookOps) {
		params.lookOps = 0;
		assert(look.get());
		RestrictedUnit::decorate(s, i, look.release());
	}
	return true;
}

ValueRep SolveAlgorithm::solvePath(Solver& s, const SolveParams& p, SolveLimits& lim) {
	if (s.hasConflict()) return value_false;
	if (lim.reached())   return value_free;
	struct  ConflictLimits {
		uint64 restart; // current restart limit
		uint64 reduce;  // current reduce limit
		uint64 grow;    // current limit for next growth operation
		uint64 global;  // current global limit
		uint64 min()      const { return std::min(std::min(restart, grow), std::min(reduce, global)); }
		void  update(uint64 x)  { restart -= x; reduce -= x; grow -= x; global -= x; }
	}            cLimit;
	typedef Range<double> RangeD;
	typedef SolvePathEvent EventType;
	SearchLimits sLimit;
	WeightLitVec inDegree;
	ScheduleStrategy ds = p.reduce.cflSched;
	ScheduleStrategy dg = p.reduce.growSched;
	ScheduleStrategy rs = p.restart.sched;
	Solver::DBInfo   db = {0,0,0};
	ValueRep result     = value_free;
	uint64 lastC        = s.stats.conflicts;
	uint64 lastR        = s.stats.restarts;
	uint64 nextUp       = 16000;
	s.stats.cflLast     = s.stats.analyzed;
	uint32 shuffle      = p.restart.shuffle;
	RangeD dbSizeLimit  = !dg.disabled() || dg.defaulted() 
	                    ? RangeD(p.reduce.initLimit(*s.sharedContext()), p.reduce.maxLimit(*s.sharedContext())) 
	                    : RangeD(p.reduce.maxRange, p.reduce.maxRange);
	uint32 dbRedInit    = ds.disabled() ? 0 : p.reduce.initLimit(*s.sharedContext());
	if (dbSizeLimit.lo < s.numLearntConstraints()) { dbSizeLimit.lo = dbSizeLimit.clamp(s.numLearntConstraints() + p.reduce.initRange.lo); }
	if (dbSizeLimit.lo > p.reduce.maxRange)        { dbSizeLimit.lo = p.reduce.maxRange; }
	if (dbSizeLimit.hi > p.reduce.maxRange)        { dbSizeLimit.hi = p.reduce.maxRange; }
	if (dbRedInit && ds.type != ScheduleStrategy::luby_schedule) {
		if (dbRedInit < ds.base) {
			dbRedInit = std::min(ds.base, std::max(dbRedInit,(uint32)5000));
			ds.grow   = dbRedInit != ds.base ? std::min(ds.grow, dbRedInit/2.0f) : ds.grow;
			ds.base   = dbRedInit;
		}
		dbRedInit   = 0;
	}
	double dbMax        = dbSizeLimit.lo;
	cLimit.grow         = dg.current(); 
	cLimit.reduce       = ds.current() + dbRedInit; 
	cLimit.global       = lim.conflicts;
	cLimit.restart      = UINT64_MAX;
	uint64& rsLimit     = p.restart.local() ? sLimit.local : cLimit.restart;
	uint64 nRestart     = 0;
	rsLimit             = rs.current();
	if (p.restart.dynamic()) {
		s.stats.enableQueue(rs.base);
		s.stats.queue->reset();
		sLimit.xLbd = (float)rs.grow;
		rsLimit     = nextUp;
	}
	EventType progress(s, SolvePathEvent::event_restart, 0, 0);
	while (result == value_free && cLimit.global) {
		uint64 minLimit = cLimit.min(); assert(minLimit);
		sLimit.learnts  = (uint32)dbSizeLimit.clamp(dbMax + (db.pinned*p.reduce.strategy.noGlue));
		sLimit.conflicts= minLimit;
		progress.cLimit = std::min(minLimit, sLimit.local);
		progress.lLimit = sLimit.learnts;
		if (progress.evType) { s.sharedContext()->reportProgress(progress); }
		result     = s.search(sLimit, p.randomProbability());
		minLimit   = (minLimit - sLimit.conflicts); // number of actual conflicts
		cLimit.update(minLimit);
		if (result == value_true && backtrackFromModel(s)) {
			result   = value_free; // continue enumeration
			if (p.restart.resetOnModel()) {
				rs.reset();
			}
			// After the first solution was found, we allow further restarts only if this
			// is compatible with the enumerator used. 
			cLimit.restart  = std::max(cLimit.restart, rs.current());
			cLimit.reduce   = ds.current() + dbRedInit;
			cLimit.grow     = std::max(cLimit.grow, uint64(1));
			progress.evType = SolvePathEvent::event_model;
			if (!p.restart.bounded() && s.backtrackLevel() > s.rootLevel()) {
				sLimit        = SearchLimits();
				cLimit.restart= UINT64_MAX;
			}
		}
		else if (result == value_free){  // limit reached
			minLimit        = 0;
			progress.evType = SolvePathEvent::event_none;
			if (rsLimit == 0 || sLimit.dynamicRestart(s.stats)) {
				// restart reached - do restart
				++s.stats.restarts; ++nRestart;
				if (p.restart.counterRestart && (nRestart % p.restart.counterRestart) == 0 ) {
					inDegree.clear();
					s.heuristic()->bump(s, inDegree, p.restart.counterBump / (double)s.inDegree(inDegree));
				}
				if (p.restart.dynamic()) {
					uint64 num = s.stats.restarts  - lastR;
					uint64 cfl = s.stats.conflicts - lastC;
					if (cfl   >=  nextUp) {
						float& lim = sLimit.xLbd != 0.0f ? sLimit.xLbd : sLimit.xCfl;
						double avg = cfl / double(num);
						bool   sx  = (s.stats.analyzed - s.stats.cflLast) >= nextUp;
						bool   tog = rs.len != 0 && (s.stats.avgLbd() > rs.len) == (lim == sLimit.xLbd);
						lastR      = s.stats.restarts;
						lastC      = s.stats.conflicts;
						if      (avg >= 16000.0) { lim += 0.1f;  nextUp = 16000; }
						else if (sx)             { lim += 0.05f; nextUp = std::max(uint64(16000), nextUp-10000); }
						else if (avg >= 4000.0)  { lim += 0.05f; }
						else if (avg >= 1000.0)  { nextUp += 10000u; }
						else if (lim > rs.grow)  { lim -= 0.05f; }
						if (tog) {
							lim    = (float)rs.grow;
							nextUp = 16000;
							std::swap(sLimit.xLbd, sLimit.xCfl);
						}
					}
					rsLimit  = nextUp;;
					minLimit = s.stats.queue->samples;
					s.stats.queue->reset();
				}
				s.undoUntil(0);
				if (rsLimit == 0)              { rsLimit = rs.next(); }
				if (!minLimit)                 { minLimit= rs.current(); }
				if (p.reduce.strategy.fRestart){ db      = s.reduceLearnts(p.reduce.fRestart(), p.reduce.strategy); }
				if (nRestart == shuffle)       { shuffle+= p.restart.shuffleNext; s.shuffleOnNextSimplify();}
				if (nRestart == lim.restarts)  { break; }
				s.stats.cflLast = s.stats.analyzed;
				progress.evType = SolvePathEvent::event_restart;
			}	
			if (cLimit.reduce == 0 || s.learntLimit(sLimit)) {
				// reduction reached - remove learnt constraints
				db              = s.reduceLearnts(p.reduce.fReduce(), p.reduce.strategy);
				cLimit.reduce   = dbRedInit + (cLimit.reduce == 0 ? ds.next() : ds.current());
				progress.evType = std::max(progress.evType, SolvePathEvent::event_deletion);
				if (s.learntLimit(sLimit) || db.pinned >= dbMax) { 
					ReduceStrategy t; t.algo = 2; t.score = 2; t.glue = 0;
					db.pinned /= 2;
					db.size    = s.reduceLearnts(0.5f, t).size;
					if (db.size >= sLimit.learnts) { dbMax = dbSizeLimit.clamp(dbMax + std::max(100.0, s.numLearntConstraints()/10.0)); }
				}
			}
			if (cLimit.grow == 0 || (dg.defaulted() && progress.evType == SolvePathEvent::event_restart)) {
				// grow sched reached - increase max db size
				if (cLimit.grow == 0)                             { cLimit.grow = dg.next(); minLimit = cLimit.grow; }
				if ((s.numLearntConstraints() + minLimit) > dbMax){ dbMax  *= p.reduce.fGrow; progress.evType = std::max(progress.evType, SolvePathEvent::event_grow); }
				if (dbMax > dbSizeLimit.hi)                       { dbMax   = dbSizeLimit.hi; cLimit.grow = UINT64_MAX; dg = ScheduleStrategy::none(); }
			}
		}
	}
	s.stats.cflLast = s.stats.analyzed - s.stats.cflLast;
	if (lim.conflicts != UINT64_MAX) { lim.conflicts = cLimit.global; }
	if (lim.restarts  != UINT64_MAX) { lim.restarts -= nRestart;   }
	return result;
}
/////////////////////////////////////////////////////////////////////////////////////////
// SimpleSolve
/////////////////////////////////////////////////////////////////////////////////////////
bool SimpleSolve::terminate() { return false; }
bool SimpleSolve::doSolve(Solver& s, const SolveParams& p) {
	s.stats.reset();
	Enumerator*  enumerator = s.sharedContext()->enumerator();
	bool hasWork    = true, complete = true;
	InitParams  init= p.init;
	SolveLimits lim = getSolveLimits();
	Timer<RealTime> tt; tt.start();
	s.sharedContext()->reportProgress(SolveStateEvent(s, "algorithm"));
	// Remove any existing assumptions and restore solver to a usable state.
	// If this fails, the problem is unsat, even under no assumptions.
	while (s.clearAssumptions() && hasWork) {
		// Add assumptions - if this fails, the problem is unsat 
		// under the current assumptions but not necessarily unsat.
		if (initPath(s, getInitialPath(), init)) {
			complete = (solvePath(s, p, lim) != value_free && s.decisionLevel() == s.rootLevel());
		}
		// finished current work item
		hasWork    = complete && enumerator->optimizeNext();
	}
	setSolveLimits(lim);
	tt.stop();
	s.sharedContext()->reportProgress(SolveStateEvent(s, "algorithm", tt.total()));
	return !complete;
}
}
