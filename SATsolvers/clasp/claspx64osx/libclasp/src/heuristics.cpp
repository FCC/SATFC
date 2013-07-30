// 
// Copyright (c) 2006-2010, Benjamin Kaufmann
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
#include <clasp/heuristics.h>
#include <algorithm>
#include <limits>
namespace Clasp {
/////////////////////////////////////////////////////////////////////////////////////////
// Lookback selection strategies
/////////////////////////////////////////////////////////////////////////////////////////
uint32 momsScore(const Solver& s, Var v) {
	uint32 sc;
	if (s.sharedContext()->numBinary()) {
		uint32 s1 = s.estimateBCP(posLit(v), 0) - 1;
		uint32 s2 = s.estimateBCP(negLit(v), 0) - 1;
		sc = ((s1 * s2)<<10) + (s1 + s2);
	}
	else {
		// problem does not contain binary constraints - fall back to counting watches
		uint32 s1 = s.numWatches(posLit(v));
		uint32 s2 = s.numWatches(negLit(v));
		sc = ((s1 * s2)<<10) + (s1 + s2);
	}
	return sc;
}

/////////////////////////////////////////////////////////////////////////////////////////
// Berkmin selection strategy
/////////////////////////////////////////////////////////////////////////////////////////
#define BERK_NUM_CANDIDATES 5
#define BERK_CACHE_GROW 2.0
#define BERK_MAX_MOMS_VARS 9999
#define BERK_MAX_MOMS_DECS 50
#define BERK_MAX_DECAY 65534

ClaspBerkmin::ClaspBerkmin(uint32 maxB) 
	: order_(false)
	, topConflict_(UINT32_MAX)
	, topOther_(UINT32_MAX)
	, front_(1) 
	, cacheSize_(5)
	, numVsids_(0)
	, maxBerkmin_(maxB == 0 ? UINT32_MAX : maxB) {
}

Var ClaspBerkmin::getMostActiveFreeVar(const Solver& s) {
	++numVsids_;
	// first: check for a cache hit
	for (Pos end = cache_.end(); cacheFront_ != end; ++cacheFront_) {
		if (s.value(*cacheFront_) == value_free) {
			return *cacheFront_;
		}
	}
	// Second: cache miss - refill cache with most active vars
	if (!cache_.empty() && cacheSize_ < s.numFreeVars()/10) {
		cacheSize_ = static_cast<uint32>( (cacheSize_*BERK_CACHE_GROW) + .5 );
	}
	cache_.clear();
	Order::Compare  comp(&order_);
	// Pre: At least one unassigned var!
	for (; s.value( front_ ) != value_free; ++front_) {;}
	Var v = front_;
	LitVec::size_type cs = std::min(cacheSize_, s.numFreeVars());
	for (;;) { // add first cs free variables to cache
		cache_.push_back(v);
		std::push_heap(cache_.begin(), cache_.end(), comp);
		if (cache_.size() == cs) break;
		while ( s.value(++v) != value_free ) {;} // skip over assigned vars
	} 
	for (v = cs == cacheSize_ ? v+1 : s.numVars()+1; v <= s.numVars(); ++v) {
		// replace vars with low activity
		if (s.value(v) == value_free && comp(v, cache_[0])) {
			std::pop_heap(cache_.begin(), cache_.end(), comp);
			cache_.back() = v;
			std::push_heap(cache_.begin(), cache_.end(), comp);
		}
	}
	std::sort_heap(cache_.begin(), cache_.end(), comp);
	return *(cacheFront_ = cache_.begin());
}

Var ClaspBerkmin::getTopMoms(const Solver& s) {
	// Pre: At least one unassigned var!
	for (; s.value( front_ ) != value_free; ++front_) {;}
	Var var   = front_;
	uint32 ms = momsScore(s, var);
	uint32 ls = 0;
	for (Var v = var+1; v <= s.numVars(); ++v) {
		if (s.value(v) == value_free && (ls = momsScore(s, v)) > ms) {
			var = v;
			ms  = ls;
		}
	}
	if (++numVsids_ >= BERK_MAX_MOMS_DECS || ms < 2) { 
		// Scores are not relevant or too many moms-based decisions
		// - disable MOMS
		hasActivities(true);           
	}
	return var;
}

void ClaspBerkmin::Order::init(const Solver& s) {
	if (s.strategies().heuReinit) { score.clear(); }
	score.resize(s.numVars()+1);
}

void ClaspBerkmin::startInit(const Solver& s) {
	if (order_.score.empty()) {
		order_.huang = s.strategies().berkHuang != 0;
		rng_.srand(s.strategies().rng.seed());
	}
	order_.init(s);
	initHuang(order_.huang);

	cache_.clear();
	cacheSize_ = 5;
	cacheFront_ = cache_.end();
	
	freeLits_.clear();
	freeOtherLits_.clear();
	topConflict_ = topOther_ = (uint32)-1;
	
	front_ = 1;
	order_.decay = numVsids_ = 0;
}

void ClaspBerkmin::endInit(Solver& s) {
	if (initHuang()) {
		const bool clearScore = s.strategies().heuMoms != 0;
		// initialize preferred values of vars
		cache_.clear();
		for (Var v = 1; v <= s.numVars(); ++v) {
			order_.decayedScore(v);
			if (order_.occ(v) != 0) {
				s.initSavedValue(v, order_.occ(v) > 0 ? value_true : value_false);
			}
			if   (clearScore) order_.score[v] = HScore(order_.decay);
			else cache_.push_back(v);
		}
		initHuang(false);
	}
	if (s.strategies().heuMoms == 0 || s.numFreeVars() > BERK_MAX_MOMS_VARS) {
		hasActivities(true);
	}
	std::stable_sort(cache_.begin(), cache_.end(), Order::Compare(&order_));
	cacheFront_   = cache_.begin();
}

void ClaspBerkmin::newConstraint(const Solver&, const Literal* first, LitVec::size_type size, ConstraintType t) {
	if (t == Constraint_t::learnt_conflict) {
		hasActivities(true);
	}
	if (order_.huang == (t == Constraint_t::static_constraint)) {
		for (const Literal* x = first, *end = first+size; x != end; ++x) {
			order_.incOcc(*x);
		}
	}
}

void ClaspBerkmin::updateReason(const Solver& s, const LitVec& lits, Literal r) {
	const bool ms = s.strategies().berkOnce == 0;
	for (LitVec::size_type i = 0, e = lits.size(); i != e; ++i) {
		if (ms || !s.seen(lits[i])) {
			order_.inc(~lits[i]);
		}
	}
	if (!isSentinel(r)) { order_.inc(r); }
}

bool ClaspBerkmin::bump(const Solver&, const WeightLitVec& lits, double adj) {
	for (WeightLitVec::const_iterator it = lits.begin(), end = lits.end(); it != end; ++it) {
		uint32 xf = order_.decayedScore(it->first.var()) + static_cast<weight_t>(it->second*adj);
		order_.score[it->first.var()].act = (uint16)std::min(xf, UINT32_MAX>>16);
	}
	return true;
}

void ClaspBerkmin::undoUntil(const Solver&, LitVec::size_type) {
	topConflict_ = topOther_ = static_cast<uint32>(-1);
	front_ = 1;
	cache_.clear();
	cacheFront_ = cache_.end();
	if (cacheSize_ > 5 && numVsids_ > 0 && (numVsids_*3) < cacheSize_) {
		cacheSize_ = static_cast<uint32>(cacheSize_/BERK_CACHE_GROW);
	}
	numVsids_ = 0;
}

bool ClaspBerkmin::hasTopUnsat(Solver& s) {
	topConflict_  = std::min(s.numLearntConstraints(), topConflict_);
	topOther_     = std::min(s.numLearntConstraints(), topOther_);
	assert(topConflict_ <= topOther_);
	freeOtherLits_.clear();
	freeLits_.clear();
	TypeSet ts;
	if (s.strategies().heuOther != 0) {
		ts.addSet(Constraint_t::learnt_loop);
		if (s.strategies().heuOther == 2) { ts.addSet(Constraint_t::learnt_other); }
		while (topOther_ > topConflict_) {
			if (s.getLearnt(topOther_-1).isOpen(s, ts, freeLits_) != 0) {
				freeLits_.swap(freeOtherLits_);
				ts.m = 0;
				break;
			}
			--topOther_;
			freeLits_.clear();
		}
	}
	ts.addSet(Constraint_t::learnt_conflict);
	uint32 stopAt = topConflict_ < maxBerkmin_ ? 0 : topConflict_ - maxBerkmin_;
	while (topConflict_ != stopAt) {
		uint32 x = s.getLearnt(topConflict_-1).isOpen(s, ts, freeLits_);
		if (x != 0) {
			if (x == Constraint_t::learnt_conflict) { break; }
			topOther_  = topConflict_;
			freeLits_.swap(freeOtherLits_);
			ts.m = 0;
			ts.addSet(Constraint_t::learnt_conflict);
		}
		--topConflict_;
		freeLits_.clear();
	}
	if (freeOtherLits_.empty())  topOther_ = topConflict_;
	if (freeLits_.empty())      freeOtherLits_.swap(freeLits_);
	return !freeLits_.empty();
}

Literal ClaspBerkmin::doSelect(Solver& s) {
	const uint32 decayMask = order_.huang ? 127 : 511;
	if ( ((s.stats.choices + 1)&decayMask) == 0 ) {
		if ((order_.decay += (1+!order_.huang)) == BERK_MAX_DECAY) {
			order_.resetDecay();
		}
	}
	if (hasTopUnsat(s)) {        // Berkmin based decision
		assert(!freeLits_.empty());
		Literal x = selectRange(s, &freeLits_[0], &freeLits_[0]+freeLits_.size());
		return selectLiteral(s, x.var(), false);
	}
	else if (hasActivities()) {  // Vsids based decision
		return selectLiteral(s, getMostActiveFreeVar(s), true);
	}
	else {                       // Moms based decision
		return selectLiteral(s, getTopMoms(s), true);
	}
}

Literal ClaspBerkmin::selectRange(Solver& s, const Literal* first, const Literal* last) {
	Literal candidates[BERK_NUM_CANDIDATES];
	candidates[0] = *first;
	uint32 c = 1;
	uint32  ms  = static_cast<uint32>(-1);
	uint32  ls  = 0;
	for (++first; first != last; ++first) {
		Var v = first->var();
		assert(s.value(v) == value_free);
		int cmp = order_.compare(v, candidates[0].var());
		if (cmp > 0) {
			candidates[0] = *first;
			c = 1;
			ms  = static_cast<uint32>(-1);
		}
		else if (cmp == 0) {
			if (ms == static_cast<uint32>(-1)) ms = momsScore(s, candidates[0].var());
			if ( (ls = momsScore(s,v)) > ms) {
				candidates[0] = *first;
				c = 1;
				ms  = ls;
			}
			else if (ls == ms && c != BERK_NUM_CANDIDATES) {
				candidates[c++] = *first;
			}
		}
	}
	return c == 1 ? candidates[0] : candidates[rng_.irand(c)];
}

Literal ClaspBerkmin::selectLiteral(Solver& s, Var v, bool vsids) const {
	ValueRep pv = s.prefValue(v);
	ValueRep sv = s.savedValue(v);
	if (sv == 0 && pv == 0) {
		int32 w0 = vsids ? (int32)s.estimateBCP(posLit(v), 5) : order_.occ(v);
		int32 w1 = vsids ? (int32)s.estimateBCP(negLit(v), 5) : 0;
		if (w1 == 1 && w0 == w1) {
			// no binary bcp - use occurrences
			w0 = order_.occ(v);
			w1 = 0;
		}
		return w0 != w1 ? Literal(v, (w0-w1)<0) : s.defaultLiteral(v);
	}
	else if (pv == 0) { 
		Literal x(v, valSign(sv));
		if (order_.huang && (order_.occ(v)*(-1+(2*x.sign()))) > 32) {
			x = ~x;
		}
		return x;
	}
	else { return Literal(v, valSign(pv)); }
}

void ClaspBerkmin::Order::resetDecay() {
	for (Scores::size_type i = 1, end = score.size(); i < end; ++i) {
		decayedScore(i);
		score[i].dec = 0;
	}
	decay = 0;
}
/////////////////////////////////////////////////////////////////////////////////////////
// ClaspVmtf selection strategy
/////////////////////////////////////////////////////////////////////////////////////////
ClaspVmtf::ClaspVmtf(LitVec::size_type mtf) : MOVE_TO_FRONT(mtf >= 2 ? mtf : 2) { 
}


void ClaspVmtf::startInit(const Solver& s) {
	if (s.strategies().heuReinit) { score_.clear(); vars_.clear(); }
	score_.resize(s.numVars()+1, VarInfo(vars_.end()));
}

void ClaspVmtf::endInit(Solver& s) {
	if (s.strategies().heuReinit)     { vars_.clear(); }
	bool moms = s.strategies().heuMoms != 0;
	for (Var v = 1; v <= s.numVars(); ++v) {
		if (s.value(v) == value_free && score_[v].pos_ == vars_.end()) {
			if (moms) {
				score_[v].activity_ = momsScore(s, v);
			}
			score_[v].pos_ = vars_.insert(vars_.end(), v);
		}
	}
	if (moms) {
		vars_.sort(LessLevel(s, score_));
		for (VarList::iterator it = vars_.begin(); it != vars_.end(); ++it) {
			score_[*it].activity_ = 0;
		}
	}
	front_ = vars_.begin();
	decay_ = 0;
}

void ClaspVmtf::simplify(const Solver& s, LitVec::size_type i) {
	for (; i < s.numAssignedVars(); ++i) {
		if (score_[s.trail()[i].var()].pos_ != vars_.end()) {
			vars_.erase(score_[s.trail()[i].var()].pos_);
			score_[s.trail()[i].var()].pos_ = vars_.end();
		}
	}
	front_ = vars_.begin();
}

void ClaspVmtf::newConstraint(const Solver& s, const Literal* first, LitVec::size_type size, ConstraintType t) {
	if (t != Constraint_t::static_constraint) {
		LessLevel comp(s, score_);
		VarVec::size_type maxMove = t == Constraint_t::learnt_conflict ? MOVE_TO_FRONT : MOVE_TO_FRONT/2;
		const bool upAct = (t == Constraint_t::learnt_conflict)
		                || (((s.strategies().heuOther + 1) & 3u) > uint32(t-1));
		for (LitVec::size_type i = 0; i < size; ++i, ++first) {
			Var v = first->var(); 
			score_[v].occ_ += 1 - (((int)first->sign())<<1);
			if (upAct) {
				++score_[v].activity(decay_);
				if (mtf_.size() < maxMove) {
					mtf_.push_back(v);
					std::push_heap(mtf_.begin(), mtf_.end(), comp);
				}
				else if (comp(v, mtf_[0])) {
					assert(s.level(v) <= s.level(mtf_[0]));
					std::pop_heap(mtf_.begin(), mtf_.end(), comp);
					mtf_.back() = v;
					std::push_heap(mtf_.begin(), mtf_.end(), comp);
				}
			}
		}
		for (VarVec::size_type i = 0; i != mtf_.size(); ++i) {
			Var v = mtf_[i];
			if (score_[v].pos_ != vars_.end()) {
				vars_.splice(vars_.begin(), vars_, score_[v].pos_);
			}
		}
		mtf_.clear();
		front_ = vars_.begin();
	} 
}

void ClaspVmtf::undoUntil(const Solver&, LitVec::size_type) {
	front_ = vars_.begin();
}

void ClaspVmtf::updateReason(const Solver&, const LitVec&, Literal r) {
	++score_[r.var()].activity(decay_);
}

bool ClaspVmtf::bump(const Solver&, const WeightLitVec& lits, double adj) {
	for (WeightLitVec::const_iterator it = lits.begin(), end = lits.end(); it != end; ++it) {
		score_[it->first.var()].activity(decay_) += static_cast<uint32>(it->second*adj);
	}
	return true;
}

Literal ClaspVmtf::doSelect(Solver& s) {
	decay_ += ((s.stats.choices + 1) & 511) == 0;
	for (; s.value(*front_) != value_free; ++front_) {;}
	Literal c;
	if (s.numFreeVars() > 1) {
		VarList::iterator v2 = front_;
		uint32 distance = 0;
		do {
			++v2;
			++distance;
		} while (s.value(*v2) != value_free);
		c = (score_[*front_].activity(decay_) + (distance<<1)+ 3) > score_[*v2].activity(decay_)
		    ? selectLiteral(s, *front_, score_[*front_].occ_)
		    : selectLiteral(s, *v2, score_[*v2].occ_);
	}
	else {
		c = selectLiteral(s, *front_, score_[*front_].occ_);
	}
	return c;
}

Literal ClaspVmtf::selectRange(Solver&, const Literal* first, const Literal* last) {
	Literal best = *first;
	for (++first; first != last; ++first) {
		if (score_[first->var()].activity(decay_) > score_[best.var()].activity(decay_)) {
			best = *first;
		}
	}
	return best;
}

/////////////////////////////////////////////////////////////////////////////////////////
// ClaspVsids selection strategy
/////////////////////////////////////////////////////////////////////////////////////////
ClaspVsids::ClaspVsids(double decay) 
	: vars_(GreaterActivity(score_)) 
	, decay_(1.0 / std::max(0.01, std::min(1.0, decay)))
	, inc_(1.0) {}

void ClaspVsids::startInit(const Solver& s) {
	if (s.strategies().heuReinit) score_.clear();
	score_.resize(s.numVars()+1);
}

void ClaspVsids::endInit(Solver& s) {
	if (s.strategies().heuReinit)     { vars_.clear(); }
	bool moms = s.strategies().heuMoms != 0;
	inc_ = 1.0;
	double maxS = 0.0;
	for (Var v = 1; v <= s.numVars(); ++v) {
		if (s.value(v) == value_free && !vars_.is_in_queue(v)) {
			if (moms) {
				// initialize activity to moms-score
				score_[v].first = momsScore(s, v);
				if (score_[v].first > maxS) {
					maxS = score_[v].first;
				}
			}
			vars_.push(v);
		}
	}
	if (moms) {
		for (VarVec::size_type i = 0; i != score_.size(); ++i) {
			score_[i].first /= maxS;
		}
	}
}

void ClaspVsids::simplify(const Solver& s, LitVec::size_type i) {
	for (; i < s.numAssignedVars(); ++i) {
		vars_.remove(s.trail()[i].var());
	}
}

void ClaspVsids::normalize() {
	const double min  = std::numeric_limits<double>::min();
	const double minD = min * 1e100;
	inc_             *= 1e-100;
	for (LitVec::size_type i = 0; i != score_.size(); ++i) {
		double d = score_[i].first;
		if (d > 0) {
			// keep relative ordering but 
			// actively avoid denormals
			d += minD;
			d *= 1e-100;
		}
		score_[i].first = d;
	}
}

void ClaspVsids::newConstraint(const Solver& s, const Literal* first, LitVec::size_type size, ConstraintType t) {
	if (t != Constraint_t::static_constraint) {
		const bool upAct = (t == Constraint_t::learnt_conflict)
		                || (((s.strategies().heuOther + 1) & 3u) > uint32(t-1));
		for (LitVec::size_type i = 0; i < size; ++i, ++first) {
			score_[first->var()].second += 1 - (first->sign() + first->sign());
			if (upAct) {
				updateVarActivity(first->var());
			}
		}
		if (t == Constraint_t::learnt_conflict) {
			inc_ *= decay_;
		}
	}
}

void ClaspVsids::updateReason(const Solver&, const LitVec&, Literal r) {
	if (r.var() != 0) updateVarActivity(r.var());
}

bool ClaspVsids::bump(const Solver&, const WeightLitVec& lits, double adj) {
	for (WeightLitVec::const_iterator it = lits.begin(), end = lits.end(); it != end; ++it) {
		updateVarActivity(it->first.var(), it->second*adj);
	}
	return true;
}

void ClaspVsids::undoUntil(const Solver& s , LitVec::size_type st) {
	const LitVec& a = s.trail();
	for (; st < a.size(); ++st) {
		if (!vars_.is_in_queue(a[st].var())) {
			vars_.push(a[st].var());
		}
	}
}

Literal ClaspVsids::doSelect(Solver& s) {
	while ( s.value(vars_.top()) != value_free ) {
		vars_.pop();
	}
	return selectLiteral(s, vars_.top(), score_[vars_.top()].second);
}

Literal ClaspVsids::selectRange(Solver&, const Literal* first, const Literal* last) {
	Literal best = *first;
	for (++first; first != last; ++first) {
		if (score_[first->var()].first > score_[best.var()].first) {
			best = *first;
		}
	}
	return best;
}
}
