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
#include <clasp/minimize_constraint.h>
#include <clasp/solver.h>

namespace Clasp { 
/////////////////////////////////////////////////////////////////////////////////////////
// MinimizeConstraint
/////////////////////////////////////////////////////////////////////////////////////////
#define STRATEGY(x) (x)

MinimizeConstraint::MinimizeConstraint(SharedData* d, bool owner)
	: shared_(d)
	, pos_(d->lits)
	, undo_(0)
	, undoTop_(0)
	, type_(d->weights.empty() ? SINGLE_LEVEL_ARITH : MULTI_LEVEL_ARITH)
	, ownsShared_(owner) {
	sum_      = new wsum_t[numRules()*4](); // [0-numRules()) stores current sum
	opt_      = &sum_[1 * numRules()];      // current opt
	temp_     = &sum_[2 * numRules()];      // temp sum
	tempOpt_  = &sum_[3 * numRules()];      // temp opt
	active_[0]= active_[1] = 0;
}

MinimizeConstraint::~MinimizeConstraint() {
	assert(shared_ == 0 && "MinimizeConstraint not destroyed!");
	delete [] sum_;
}

bool MinimizeConstraint::attach(Solver& s) {
	assert(s.decisionLevel() == 0);
	uint32 numL = 0;
	VarVec up;
	const SharedData* d = shared_;
	const bool heuristic= (s.strategies().optHeu & SolverStrategies::opt_sign) != 0;
	for (const WeightLiteral* it = d->lits; !isSentinel(it->first); ++it, ++numL) {
		if (s.value(it->first.var()) == value_free) {
			s.addWatch(it->first, this, numL);
			if (heuristic) { s.initSavedValue(it->first.var(), falseValue(it->first)); }
		}
		else if (s.isTrue(it->first)) {
			up.push_back(numL);
		}
	}
	// [0,numL+1)      : undo stack
	// [numL+1, numL*2): pos  stack
	undo_    = new UndoInfo[(numL*2)+1]; 
	undoTop_ = 0;
	posTop_  = numL+1;
	for (uint32 i = 0; i != numRules(); ++i) {
		sum_[i]= shared_->sum(i); 
	}
	STRATEGY(convert(sum_));
	*opt_    = std::numeric_limits<wsum_t>::max();
	for (WeightVec::size_type i = 0; i != up.size(); ++i) {
		MinimizeConstraint::propagate(s, shared_->lits[up[i]].first, up[i]);
	}
	if (shared()->mode() == MinimizeMode_t::enumerate) {
		restoreOptimum();
	}
	bool intOpt = s.isTrue(s.sharedContext()->tagLiteral());
	return !s.hasConflict() && (!intOpt || integrateNext(s));
}

void MinimizeConstraint::destroy(Solver* s, bool detach) {
	if (s && detach) {
		for (const WeightLiteral* it = shared_->lits; !isSentinel(it->first); ++it) {
			s->removeWatch(it->first, this);
		}
	}
	delete [] undo_;
	undo_ = 0;
	if (ownsShared_) {
		shared_->destroy();
	}
	shared_ = 0;
	Constraint::destroy(s, detach);
}

// returns the numerical highest decision level watched by this constraint.
uint32 MinimizeConstraint::lastUndoLevel(const Solver& s) const {
	return undoTop_ != 0
		? s.level(shared_->lits[undo_[undoTop_-1].index()].first.var())
		: 0;
}

// pushes the literal at index idx onto the undo stack
// and marks it as seen; if literal is first in current level
// adds a new undo watch
void MinimizeConstraint::pushUndo(Solver& s, uint32 idx) {
	assert(idx >= static_cast<uint32>(pos_ - shared_->lits));
	undo_[undoTop_].data.idx  = idx;
	undo_[undoTop_].data.newDL= 0;
	if (lastUndoLevel(s) != s.decisionLevel()) {
		// remember current "look at" position and start
		// a new decision level on the undo stack
		undo_[posTop_++].data.idx = static_cast<uint32>(pos_-shared_->lits);
		s.addUndoWatch(s.decisionLevel(), this);
		undo_[undoTop_].data.newDL = 1;
	}
	undo_[idx].data.idxSeen   = 1;
	++undoTop_;
}

Constraint::PropResult MinimizeConstraint::propagate(Solver& s, Literal, uint32& data) {
	pushUndo(s, data);
	STRATEGY(add(shared_->lits[data].second));
	return PropResult(propagateImpl(s, propagate_new_sum), true);
}

// computes the set of literals implying p and returns 
// the highest decision level of that set
// PRE: p is implied on highest undo level
uint32 MinimizeConstraint::computeImplicationSet(const Solver& s, const WeightLiteral& p, uint32& undoPos) {
	// start from current sum
	STRATEGY(assign(temp_, sum_));
	uint32 up       = undoTop_;
	// start with full set
	for (UndoInfo u; up != 0; --up) {
		u = undo_[up-1];
		// subtract last last element from set
		STRATEGY(subtract(temp_, shared_->lits[u.index()].second));
		if (!STRATEGY(implied(temp_, p.second))) {
			// p is no longer implied after we removed last literal,
			// hence [0, up) implies p @ level of last literal
			undoPos = up;
			return s.level(shared_->lits[u.index()].first.var());
		}
	}
	undoPos = 0; 
	return 0;
}

bool MinimizeConstraint::propagateImpl(Solver& s, PropMode m) {
	Iter it    = pos_;
	uint32 idx = static_cast<uint32>(it - shared_->lits);
	uint32 DL  = s.decisionLevel();
	// current implication level or "unknown" if
	// we propagate a new optimum 
	uint32 impLevel = DL + (m == propagate_new_opt);
	weight_t lastW  = -1;
	uint32 undoPos  = undoTop_;
	bool ok         = true;
	for (; ok && !isSentinel(it->first); ++it, ++idx) {
		// skip propagated/false literals
		if (litSeen(idx) || (m == propagate_new_sum && s.isFalse(it->first))) {
			continue;
		}
		if (lastW != it->second) {
			// check if the current weight is implied
			if (!STRATEGY(implied(sum_, it->second))) {
				// all good - current optimum is safe
				pos_    = it;
				return true;
			}
			// compute implication set and level of current weight
			if (m == propagate_new_opt) {
				impLevel = computeImplicationSet(s, *it, undoPos);
			}
			lastW = it->second;
		}
		// force implied literals
		if (!s.isFalse(it->first) || (impLevel < DL && s.level(it->first.var()) > impLevel)) {
			if (impLevel != DL) { DL = s.undoUntil(impLevel, true); }
			ok = s.force(~it->first, impLevel, this, undoPos);
		}
	}
	return ok;
}

// pops free literals from the undo stack and decreases current sum
void MinimizeConstraint::undoLevel(Solver&) {
	assert(undoTop_ != 0 && posTop_ > undoTop_);
	uint32 up  = undoTop_;
	uint32 idx = undo_[--posTop_].index();
	for (;;) {
		UndoInfo& u = undo_[--up];
		undo_[u.index()].data.idxSeen = 0;
		STRATEGY(subtract(sum_, shared_->lits[u.index()].second));
		if (u.newDL()) {
			break;
		}
	}
	undoTop_ = up;
	Iter temp= shared_->lits + idx;
	if (temp < pos_) pos_ = temp;
}

// computes the reason for p - 
// all literals that were propagated before p
void MinimizeConstraint::reason(Solver& s, Literal p, LitVec& lits) {
	uint32 stop = s.reasonData(p);
	assert(stop <= undoTop_);
	Literal x;
	for (uint32 i = 0; i != stop; ++i) {
		UndoInfo u = undo_[i];
		x = shared_->lits[u.index()].first;
		lits.push_back(x);
	}
}

bool MinimizeConstraint::minimize(Solver& s, Literal p, CCMinRecursive* rec) {
	uint32 stop = s.reasonData(p);
	assert(stop <= undoTop_);
	Literal x;
	for (uint32 i = 0; i != stop; ++i) {
		UndoInfo u = undo_[i];
		x = shared_->lits[u.index()].first;
		if (!s.ccMinimize(x, rec)) {
			return false;
		}
	}
	return true;
}

Constraint* MinimizeConstraint::cloneAttach(Solver&) {
	assert(false);
	return 0;
}

// Stores the current model as the optimum and determines the decision level 
// on which the search should continue.
uint32 MinimizeConstraint::commitCurrent(const Solver& s) {
	uint32 dl          = s.decisionLevel();
	// store current sum as new optimum in shared...
	const Optimum* opt = shared_->setOptimum(STRATEGY(sumToOpt()));
	if (shared_->mode() == MinimizeMode_t::optimize) {
		// temporarily assume next tentative optimum and
		// compute first level on which constraint is violated
		std::memcpy(tempOpt_, sum_, numRules()*sizeof(wsum_t));
		dl = updateOpt(&s, true, opt);
		// restore to new optimum
		std::swap(tempOpt_, opt_);
	}
	// else: keep bound during enumeration
	return dl - 1;
}

// resets the optimum to the one stored in the shared data object
bool MinimizeConstraint::restoreOptimum() {
	pos_ = shared_->lits;
	updateOpt(0, false, shared_->optimum());
	return opt_[0] != std::numeric_limits<wsum_t>::max();
}

// integrates a new tentative optimum
bool MinimizeConstraint::integrateNext(Solver& s) {
	assert(!s.hasConflict() && s.isTrue(s.sharedContext()->tagLiteral()));
	uint32 cl = UINT32_MAX;
	if (shared_->mode() != MinimizeMode_t::enumerate) {
		std::swap(tempOpt_, opt_);
		cl      = updateOpt(&s, true, shared_->optimum());
	}
	do {
		pos_    = shared_->lits;
		if (cl > s.decisionLevel() && propagateImpl(s, propagate_new_opt)) {
			return true;
		}
		if (cl <= s.decisionLevel()&& s.undoUntil(cl-1, true) != cl-1) {
			s.undoUntil(cl, true);
			if (cl != s.rootLevel()) { s.force(negLit(0), this, undoTop_); }
			else                     { s.setStopConflict(); }
		}
	} while (!s.hasConflict() || s.resolveConflict());
	// integration failed - restore current optimum
	std::swap(tempOpt_, opt_);
	return false;
}

bool MinimizeConstraint::modelHeuristic(Solver& s) {
	const bool modelHeu  = (s.strategies().optHeu & SolverStrategies::opt_model) != 0;
	const bool heuristic = modelHeu || (s.queueSize() == 0 && s.decisionLevel() == s.rootLevel());
	if (heuristic && s.propagate()) {
		for (const WeightLiteral* w = pos_; !isSentinel(w->first); ++w) {
			if (s.value(w->first.var()) == value_free) {
				s.assume(~w->first);
				if (!modelHeu || !s.propagate()) { break; }
			}
		}
	}
	return !s.hasConflict() || s.resolveConflict();
}

// Sets (opt-applyStep?step:0) as the new optimum and returns the numerical
// lowest decision level on which the constraint is conflicting w.r.t
// the new optimum or UINT32_MAX if the constraint is not conflicting.
uint32 MinimizeConstraint::updateOpt(const Solver* s, bool applyStep, const Optimum* opt) {
	assert(s != 0 || !applyStep);
	active_[0] = active_[1] = 0;
	uint32 seq;
	bool   ret;
	Optimum::Step step;
	for (;;) {
		seq = opt->seq;
 		ret = shared_->convert(opt, applyStep, opt_);
 		step= opt->step;
		if (seq == opt->seq && opt == shared_->optimum()) {
 			break;
 		}
		// optimum has changed - reload
		opt = shared_->optimum();
	}
	if (!ret) return 0;
	if (greater(sum_, opt_, STRATEGY(convert(opt_)))) {
		assert(s);
		uint32 lev = step.first;
		if (lev == shared_->maxLevel() && step.second == 1 && (sum_[lev]-opt_[lev]) == 1) {
			return lastUndoLevel(*s);
		}
		WeightLiteral x;
		x.second = shared_->weights.empty() ? 0 : (weight_t)shared_->weights.size()-1;
		uint32 ignore = 0;
		return computeImplicationSet(*s, x, ignore);
	}
	return UINT32_MAX;
}
#undef STRATEGY
/////////////////////////////////////////////////////////////////////////////////////////
// MinimizeConstraint - arithmetic strategy implementation
//
// For now we use a simple "switch-on-type" approach. 
// In the future, if new strategies emerge, we may want to use a full-blown strategy 
// hierarchy.
/////////////////////////////////////////////////////////////////////////////////////////
// set *lhs = *rhs, where lhs != rhs
void MinimizeConstraint::assign(wsum_t* lhs, wsum_t* rhs) {
	if (type_ == SINGLE_LEVEL_ARITH) { *lhs = *rhs; return; }
	std::memcpy(lhs, rhs, numRules()*sizeof(wsum_t));
	active(lhs) = active(rhs);
}
// sum += weight
void MinimizeConstraint::add(weight_t wOrIdx) {
	if (type_ == SINGLE_LEVEL_ARITH) { *sum_ += wOrIdx; return; }
	const SharedData::LevelWeight* w = &shared_->weights[wOrIdx];
	do { sum_[w->level] += w->weight; } while (w++->next);
}
// lhs -= weight, where lhs either sum or temp
void MinimizeConstraint::subtract(wsum_t* lhs, weight_t wOrIdx) {
	if (type_ == SINGLE_LEVEL_ARITH) { *lhs -= wOrIdx; return; }
	const SharedData::LevelWeight* w = &shared_->weights[wOrIdx];
	uint32& a = active(lhs);
	if (w->level < a) {  a = w->level;  }
	do { lhs[w->level] -= w->weight; } while (w++->next);
}
// (lhs + weight) > opt
bool MinimizeConstraint::implied(wsum_t* lhs, weight_t wOrIdx) {
	if (type_ == SINGLE_LEVEL_ARITH) { return (*lhs+wOrIdx) > *opt_; }
	const SharedData::LevelWeight* w = &shared_->weights[wOrIdx];
	uint32& a = active(lhs); 
	if (w->level < a) { a = w->level; }
	while (a != w->level) {
		if (lhs[a] != opt_[a]) {
			return lhs[a] > opt_[a];
		}
		++a;
	}
	wsum_t temp;
	for (uint32 i = a, end = shared_->numRules(); i != end; ++i) {
		temp = lhs[i];
		if (i == w->level) {
			temp += w->weight;
			if (w->next) ++w;
		}
		if (temp != opt_[i]) { return temp > opt_[i]; }
	}
	return false;
}
/////////////////////////////////////////////////////////////////////////////////////////
// SharedMinimizeData
/////////////////////////////////////////////////////////////////////////////////////////
SharedMinimizeData::SharedMinimizeData(const SumVec& lhsSum, const SumVec& lhsAdjust, const SumVec& rhsSum, MinimizeMode m) : mode_(m) {
	sum_         = lhsSum;
	opts_[0].seq = 0;
	opts_[0].opt = rhsSum;
	opts_[0].step= Optimum::Step(numRules()-1, 0);
	opt_         = &opts_[0];
	next_        = &opts_[1];
	adjust_      = 0;
	low_         = -static_cast<int64>(UINT64_MAX/2);
	hierarch_    = 0;
	if (!lhsAdjust.empty()) {
		std::copy(lhsAdjust.begin(), lhsAdjust.end(), (adjust_ = new wsum_t[lhsAdjust.size()]));
	}
}
SharedMinimizeData::~SharedMinimizeData() {
	delete [] adjust_;
}

void SharedMinimizeData::destroy() const {
	this->~SharedMinimizeData();
	::operator delete(const_cast<SharedMinimizeData*>(this));
}

void SharedMinimizeData::setMode(MinimizeMode m, int hierarchical)  { 
	mode_     = m; 
	hierarch_ = 0;
	if (mode_ == MinimizeMode_t::optimize && hierarchical > 0) {
		opt_->step.first = 0;
		hierarch_        = (uint32)hierarchical;
	}
}

MinimizeConstraint* SharedMinimizeData::attach(Solver& s, bool setOwner) {
	MinimizeConstraint* ret = new MinimizeConstraint(this, setOwner);
	ret->attach(s);
	return ret;
}

const SharedMinimizeData::Optimum* SharedMinimizeData::setOptimum(const wsum_t* newOpt) {
	Optimum* temp = opt_;
	next_->opt.assign(newOpt, newOpt+numRules());
	next_->step   = temp->step;
	setStep(next_);
	next_->seq    = temp->seq+1;
	opt_          = next_;
	next_         = temp;
	return opt_;
}

bool SharedMinimizeData::optimizeNext(Optimum* opt) {
	wsum_t&  step= opt->step.second;
	uint32&  lev = opt->step.first;
	if (step > 1) {
		low_  = (opt->opt[lev] - step)+1;
		step  = hierarch_ != 3 ? 1 : step/2;
		return true;
	}
	for (uint32 maxL = maxLevel(); lev < maxL && step; ) {
		++lev;
		if (opt->opt[lev] > sum_[lev]) {
			low_ = -static_cast<int64>(UINT64_MAX/2);
			step = 1;
			return true;
		}
	}
	lev = UINT32_MAX;
	return false;
}

void SharedMinimizeData::setStep(Optimum* o) {
	assert(o->step.first != UINT32_MAX);
	wsum_t& step   = o->step.second;
	if (!step) step= 1;
	if (hierarch_ > 1) {
		uint32& lev  = o->step.first;
		wsum_t  val  = o->opt[lev];
		low_         = std::max(sum_[lev], low_);
		wsum_t maxS  = val - low_;
		maxS         = (maxS + (maxS&1))/2;
		if ( (step  *= 2) > maxS ) {
			step       = maxS;
		}
		if (hierarch_ == 3) {
			step = val - low_;
			if (lev == 0 && low_ == sum_[0]) {
				weight_t wOrIdx = lits[0].second;
				step -= weights.empty() ? wOrIdx : weights[wOrIdx].weight;
			}
		}
		if ( step == 0 || (val - step) < low_ ) {
			step = 1;
			optimizeNext(o);
		}
	}
}

bool SharedMinimizeData::convert(const Optimum* opt, bool applyStep, wsum_t* optOut) const {
	wsum_t n    = opt->step.second;
	applyStep  &= (n != 0);
	uint32 i    = static_cast<uint32>(opt->opt.size());
	uint32 lev  = applyStep ? opt->step.first : i;
	bool   ap   = !applyStep;
	for (wsum_t t; i--; ) {
		t         = opt->opt[i];
		if (i > lev) { 
			t       = std::numeric_limits<wsum_t>::max(); 
		}
		else if (!ap && lev != UINT32_MAX) {
			t       = opt->opt[i] - n;
			if (t  >= sum_[i]) { ap = true; }
			else               { t  = std::numeric_limits<wsum_t>::max(); }
		}
		optOut[i] = t;
	}
	return ap;
}
/////////////////////////////////////////////////////////////////////////////////////////
// MinimizeBuilder
/////////////////////////////////////////////////////////////////////////////////////////
MinimizeBuilder::MinimizeBuilder() : ready_(false) { }
MinimizeBuilder::~MinimizeBuilder() { 
	for (LitVec::size_type i = 0; i != lits_.size(); ++i) {
		Weight::free(lits_[i].second);
	}
}

// adds a new minimize statement
MinimizeBuilder& MinimizeBuilder::addRule(const WeightLitVec& lits, wsum_t initSum) {
	if (ready_) {
		assert(isSentinel(lits_.back().first));
		lits_.pop_back();
		ready_ = false;
	}
	uint32 lev = (uint32)sum_.size();
	for (WeightLitVec::const_iterator it = lits.begin(); it != lits.end(); ++it) {
		assert(it->second >= 0);
		if (it->second > 0) {
			lits_.push_back(LitRep(it->first, new Weight(lev, it->second)));
		}
	}
	sum_.push_back(initSum);
	return *this;
}

// sets an initial optimum
MinimizeBuilder& MinimizeBuilder::setOptimum(uint32 lev, wsum_t opt) {
	if (lev >= rhs_.size()) {
		rhs_.resize(lev+1, std::numeric_limits<wsum_t>::max());
	}
	rhs_[lev] = opt;
	return *this;
}

// adds the weights of the given lit to the appropriate levels in vec
void MinimizeBuilder::addTo(LitRep lit, SumVec& vec) {
	vec.resize(numRules());
	for (Weight* r = lit.second; r; r = r->next) {
		vec[r->level] += r->weight;
	}
}

// merges duplicate literals and removes literals that are already assigned
// POST: the literals in lits_ are unique and sorted by decreasing weight
bool MinimizeBuilder::prepare(SharedContext& ctx) {
	std::sort(lits_.begin(), lits_.end(), CmpByLit());
	LitVec::size_type j = 0;
	Solver& s           = *ctx.master();
	Weight* w           = 0;
	for (LitVec::size_type i = 0, k = 0, end = lits_.size(); i != end;) {
		w    = lits_[i].second;
		if (s.value(lits_[i].first.var()) == value_free) {
			for (k = i+1; k < end && lits_[i].first == lits_[k].first; ++k) {
				// duplicate literal - merge weights
				if (w->level == lits_[k].second->level) {
					// add up weights from same level
					w->weight += lits_[k].second->weight;
				}
				else {
					// extend weight vector with new level
					w->next         = lits_[k].second;
					w               = w->next;
					lits_[k].second = 0;
				}
				Weight::free(lits_[k].second);
			}	
			// exempt from variable elimination
			ctx.setFrozen(lits_[i].first.var(), true);
			lits_[j++] = lits_[i];
			i = k;
		}
		else {
			if (s.isTrue(lits_[i].first)) {
				addTo(lits_[i], sum_);
			}
			Weight::free(lits_[i].second);
			++i;	
		}
	}
	shrinkVecTo(lits_, j);
	// allocate enough reason data for all our vars
	ctx.requestData(!lits_.empty() ? lits_.back().first.var() : 0);
	// now literals are unique - merge any complementary literals
	j = 0; CmpByWeight greaterW; int cmp;
	for (LitVec::size_type i = 0, k = 1; i < lits_.size(); ) {
		if (k == lits_.size() || lits_[i].first.var() != lits_[k].first.var()) {
			lits_[j++] = lits_[i];
			++i, ++k;
		}
		else if ( (cmp = greaterW.compare(lits_[i], lits_[k])) != 0 ) {
			LitVec::size_type wMin = cmp > 0 ? k : i;
			LitVec::size_type wMax = cmp > 0 ? i : k;
			addTo(lits_[wMin], adjust_);
			mergeReduceWeight(lits_[wMax], lits_[wMin]);
			assert(lits_[wMin].second == 0);
			lits_[j++] = lits_[wMax];
			i += 2;
			k += 2;
		}
		else {
			// weights are equal
			addTo(lits_[i], adjust_);
			Weight::free(lits_[i].second);
			Weight::free(lits_[k].second);
			i += 2;
			k += 2;
		}
	}
	shrinkVecTo(lits_, j);
	std::stable_sort(lits_.begin(), lits_.end(), greaterW);
	if (sum_.empty()) {
		sum_.push_back(0);
	}
	// add terminating sentinel literal
	lits_.push_back(LitRep(posLit(0), new Weight(static_cast<uint32>(sum_.size()-1), 0)));
	bool ok = true;
	for (uint32 i = 0, end = std::min((uint32)sum_.size(), (uint32)rhs_.size()); i != end; ++i) {
		if (sum_[i] != rhs_[i]) {
			ok = sum_[i] < rhs_[i];
			break;
		}
	}
	return ok;
}

// PRE: the literals in lits_ are unique and sorted by decreasing weight
void MinimizeBuilder::addAssumption(Literal ma) {
	if (ma != posLit(0)) {
		weight_t highest = lits_[0].second->weight + 1;
		lits_.insert(lits_.begin(), LitRep(ma, new Weight(0, highest)));
		sum_[0] -= highest;
	}
}

// creates a suitable minimize constraint from the 
// previously added minimize statements
MinimizeBuilder::SharedData* MinimizeBuilder::build(SharedContext& ctx, Literal ma) {
	assert(!ctx.master()->hasConflict());
	if (!ctx.master()->propagate()) return 0;
	if (!ready_) {
		if (!prepare(ctx)) return 0;
		addAssumption(ma);
		ready_ = true;
	}
	rhs_.resize(sum_.size(), std::numeric_limits<wsum_t>::max());
	SharedData* srep = new (::operator new(sizeof(SharedData) + (lits_.size()*sizeof(WeightLiteral)))) SharedData(sum_, adjust_, rhs_);
	if (sum_.size() == 1) {
		for (LitVec::size_type i = 0; i != lits_.size(); ++i) {
			srep->lits[i] = WeightLiteral(lits_[i].first, lits_[i].second->weight);
		}
	}
	else {
		// The weights of a multi-level constraint are stored in a flattened way,
		// i.e. we store all weights in one vector and each literal stores
		// an index into that vector. For a (weight) literal i, weights[i.second]
		// is the first weight of literal i and weights[i.second].next denotes
		// whether i has more than one weight.
		srep->lits[0].first  = lits_[0].first;
		srep->lits[0].second = addFlattened(srep->weights, *lits_[0].second);
		for (LitVec::size_type i = 1; i < lits_.size(); ++i) {
			srep->lits[i].first = lits_[i].first;
			if (eqWeight(&srep->weights[srep->lits[i-1].second], *lits_[i].second)) {
				// reuse existing weight
				srep->lits[i].second = srep->lits[i-1].second;
			}
			else {
				// add a new flattened list of weights to srep->weights
				srep->lits[i].second = addFlattened(srep->weights, *lits_[i].second);
			}
		}
	}
	return srep;
}

MinimizeConstraint* MinimizeBuilder::buildAndAttach(SharedContext& ctx, MinimizeMode mode) {
	if (SharedData* d = build(ctx)) {
		d->setMode(mode, false);
		MinimizeConstraint* m = d->attach(*ctx.master(), true);
		return m;
	}
	return 0;
}

// computes x.weight -= by.weight
// PRE: x.weight > by.weight
void MinimizeBuilder::mergeReduceWeight(LitRep& x, LitRep& by) {
	assert(x.second->level <= by.second->level);
	Weight dummy(0,0);
	dummy.next  = x.second;
	Weight* ins = &dummy;
	for (;by.second;) {
		// unlink head
		Weight* t = by.second;
		by.second = by.second->next;
		// prepare for subtraction
		t->weight*= -1;
		// find correct insert location
		while (ins->next && ins->next->level < t->level) {
			ins = ins->next;
		}
		if (!ins->next || ins->next->level > t->level) {
			t->next   = ins->next ? ins->next : 0;
			ins->next = t;
		}
		else if ( (ins->next->weight += t->weight) != 0 ) {
			delete t;
		}
		else {
			Weight* t2 = ins->next;
			ins->next  = t2->next;
			delete t2;
			delete t;
		}
	}
	x.second = dummy.next;
}

// sort by literal id followed by weight
bool MinimizeBuilder::CmpByLit::operator()(const LitRep& lhs, const LitRep& rhs) const {
	return lhs.first < rhs.first ||
		(lhs.first == rhs.first && lhs.second->level < rhs.second->level);
}
// sort by final weight
bool MinimizeBuilder::CmpByWeight::operator()(const LitRep& lhs, const LitRep& rhs) const {
	Weight* wLhs = lhs.second;
	Weight* wRhs = rhs.second;
	while (wLhs && wRhs) {
		if (wLhs->level != wRhs->level) {
			return wLhs->level < wRhs->level;
		}
		if (wLhs->weight != wRhs->weight) {
			return wLhs->weight > wRhs->weight;
		}
		wLhs = wLhs->next;
		wRhs = wRhs->next;
	}
	return (wLhs && wLhs->weight > 0)
	  ||   (wRhs && wRhs->weight < 0);
}
int MinimizeBuilder::CmpByWeight::compare(const LitRep& lhs, const LitRep& rhs) const {
	if (this->operator()(lhs, rhs)) return 1;
	if (this->operator()(rhs, lhs)) return -1;
	return 0;
}

// frees the given weight list
void MinimizeBuilder::Weight::free(Weight*& head) {
	for (Weight* r = head; r;) {
		Weight* t = r;
		r         = r->next;
		delete t;
	}
	head = 0;
}

// flattens the given weight w and adds the flattened representation to x
// RETURN: starting position of w in x
weight_t MinimizeBuilder::addFlattened(SharedData::WeightVec& x, const Weight& w) {
	typedef SharedData::LevelWeight WT;
	uint32 idx       = static_cast<uint32>(x.size());
	const Weight* r  = &w;
	while (r) {
		x.push_back(WT(r->level, r->weight));
		x.back().next  = (r->next != 0);
		r              = r->next;
	}
	return idx;
}
// returns true if lhs is equal to w
bool MinimizeBuilder::eqWeight(const SharedData::LevelWeight* lhs, const Weight& w) {
	const Weight* r = &w;
	do {
		if (lhs->level != r->level || lhs->weight != r->weight) {
			return false;
		}
		r = r->next;
		if (lhs->next == 0) return r == 0;
		++lhs;
	} while(r);
	return false;
}
} // end namespaces Clasp
