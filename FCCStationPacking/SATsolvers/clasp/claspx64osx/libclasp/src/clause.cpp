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
#include <clasp/clause.h>
#include <clasp/solver.h>
#include <clasp/util/misc_types.h>
#include <algorithm>

namespace Clasp { namespace Detail {

struct GreaterLevel {
	GreaterLevel(const Solver& s) : solver_(s) {}
	bool operator()(const Literal& p1, const Literal& p2) const {
		assert(solver_.value(p1.var()) != value_free && solver_.value(p2.var()) != value_free);
		return solver_.level(p1.var()) > solver_.level(p2.var());
	}
private:
	GreaterLevel& operator=(const GreaterLevel&);
	const Solver& solver_;
};

struct Sink { 
	explicit Sink(SharedLiterals* c) : clause(c) {}
	~Sink() { if (clause) clause->release(); }
	SharedLiterals* clause;   
};

// returns an abstraction of p's decision level, s.th
// abstraction(any true literal) > abstraction(any free literal) > abstraction(any false literal).
static uint32 levelAbstraction(const Solver& s, Literal p) {
	ValueRep value_p = s.value(p.var());
	// DL+1,  if isFree(p)
	// DL(p), if isFalse(p)
	// ~DL(p),if isTrue(p)
	uint32   abstr_p = value_p == value_free ? s.decisionLevel()+1 : s.level(p.var()) ^ -(value_p==trueValue(p));
	assert(abstr_p > 0 || (s.isFalse(p) && s.level(p.var()) == 0));
	return abstr_p;
}

void* alloc(uint32 size) {
	#pragma message TODO("replace with CACHE_LINE_ALIGNED alloc")
	return ::operator new(size);
}
void free(void* mem) {
	::operator delete(mem);
}

} // namespace Detail

/////////////////////////////////////////////////////////////////////////////////////////
// SharedLiterals
/////////////////////////////////////////////////////////////////////////////////////////
SharedLiterals* SharedLiterals::newShareable(const Literal* lits, uint32 size, ConstraintType t, uint32 numRefs) {
	void* m = Detail::alloc(sizeof(SharedLiterals)+(size*sizeof(Literal)));
	return new (m) SharedLiterals(lits, size, t, numRefs);
}

SharedLiterals::SharedLiterals(const Literal* a_lits, uint32 size, ConstraintType t, uint32 refs) 
	: size_type_( (size << 2) + t ) {
	refCount_ = std::max(uint32(1),refs);
	std::memcpy(lits_, a_lits, size*sizeof(Literal));
}

uint32 SharedLiterals::simplify(Solver& s) {
	bool   removeFalse = unique();
	uint32   newSize   = 0;
	Literal* r         = lits_;
	Literal* e         = lits_+size();
	ValueRep v;
	for (Literal* c = r; r != e; ++r) {
		if ( (v = s.value(r->var())) == value_free ) {
			if (c != r) *c = *r;
			++c; ++newSize;
		}
		else if (v == trueValue(*r)) {
			newSize = 0; break;
		}
		else if (!removeFalse) ++c;
	}
	if (removeFalse && newSize != size()) {
		size_type_ = (newSize << 2) | (size_type_ & uint32(3));
	}
	return newSize;
}

void SharedLiterals::release() {
	if (--refCount_ == 0) {
		this->~SharedLiterals();
		Detail::free(this);
	}
}
SharedLiterals* SharedLiterals::share() {
	++refCount_;
	return this;
}
/////////////////////////////////////////////////////////////////////////////////////////
// ClauseCreator::Watches
/////////////////////////////////////////////////////////////////////////////////////////
struct ClauseCreator::Watches {
	Watches(const Solver& s, const Literal* begin, const Literal* end, bool knownOrder) {
		uint32 size   = uint32(end - begin);
		simpSize      = size;
		std::fill_n(lits, (int)Clause::MAX_SHORT_LEN, negLit(0));
		if (!knownOrder) { init(s, (Literal*)begin, size, lits); }
		else             { 
			uint32 i= size;
			while (i-- && Detail::levelAbstraction(s, begin[i]) == 0) {
				--simpSize;
			}
			std::memcpy(lits, begin, std::min(simpSize, uint32(Clause::MAX_SHORT_LEN))*sizeof(Literal));
			abstr_f = Detail::levelAbstraction(s, lits[0]);
			abstr_s = Detail::levelAbstraction(s, lits[1]);
		}
		assert(simpSize <= size);
	}
	Watches(const Solver& s, LitVec& in, bool knownOrder) {
		assert(in.size() > 1);
		simpSize = in.size();
		if (!knownOrder) { init(s, &in[0], in.size(), 0); }
		else             { abstr_f = Detail::levelAbstraction(s, in[0]); abstr_s = Detail::levelAbstraction(s, in[1]);  }
		lits[0] = in[0];
		lits[1] = in[1];
	}
	void init(const Solver& s, Literal* in, uint32 size, Literal* out) {
		abstr_f     = abstr_s = simpSize = 0;
		uint32 fp   = 0,  sp  = 0;
		for (uint32 i = 0, j  = 0, abstr_p; i != size; ++i) {
			Literal p = in[i];
			abstr_p   = Detail::levelAbstraction(s, p);
			if (abstr_p) {
				++simpSize;
				if (out) {
					out[j++]= abstr_p > abstr_s ? in[sp] : in[i];
					if (j  == Clause::MAX_SHORT_LEN) { j = 2; }
				}
				if (abstr_p > abstr_f) {
					assert(abstr_f >= abstr_s);
					sp      = fp;
					abstr_s = abstr_f;
					fp      = i;
					abstr_f = abstr_p;
				}
				else if (abstr_p > abstr_s) {
					sp      = i;
					abstr_s = abstr_p;
				}
			}
		}
		if (out) {
			if (simpSize > 0) out[0] = in[fp];
			if (simpSize > 1) out[1] = in[sp];
		}
		else if (size > 1) {
			std::swap(in[0], in[fp]);
			std::swap(in[1], sp != 0 ? in[sp] : in[fp]);
		}
	}
	uint32  abstr_f;
	uint32  abstr_s;
	uint32  simpSize;
	Literal lits[Clause::MAX_SHORT_LEN];
};
/////////////////////////////////////////////////////////////////////////////////////////
// ClauseCreator
/////////////////////////////////////////////////////////////////////////////////////////
ClauseCreator::ClauseCreator(Solver* s) 
: solver_(s) { }

void ClauseCreator::init(ConstraintType t) {
	assert(!solver_ || solver_->decisionLevel() == 0 || t != Constraint_t::static_constraint);
	literals_.clear();
	extra_     = ClauseInfo(t);
	fwLev_     = 0;
	swLev_     = 0;
}

ClauseCreator& ClauseCreator::start(ConstraintType t) {
	init(t);
	return *this;
}

ClauseCreator& ClauseCreator::startAsserting(ConstraintType t, const Literal& p) {
	init(t);
	literals_.push_back(p);
	fwLev_     = UINT32_MAX;
	return *this;
}

ClauseCreator& ClauseCreator::add(const Literal& p) {
	assert(solver_); const Solver& s = *solver_;
	uint32 abstr_p = Detail::levelAbstraction(s, p);
	if (abstr_p != 0) {
		literals_.push_back(p);
		// watch the two literals with highest abstraction, i.e.
		// prefer true over free over false literals
		if (abstr_p > fwLev_) {
			std::swap(abstr_p, fwLev_);
			std::swap(literals_[0], literals_.back());
		}
		if (abstr_p > swLev_) {
			std::swap(literals_[1], literals_.back());
			swLev_= abstr_p;
		}
	}
	// else: drop uncoditionally false literals 
	return *this;
}

void ClauseCreator::simplify() {
	if (literals_.empty()) return;
	swLev_    = 0;
	uint32 sw = 0;
	solver_->markSeen(literals_[0]);
	LitVec::size_type i, j = 1;
	for (i = 1; i != literals_.size(); ++i) {
		Literal x = literals_[i];
		if (solver_->seen(~x)) { 
			fwLev_     = UINT32_MAX; 
			continue;
		}
		if (!solver_->seen(x)) {
			solver_->markSeen(x);
			if (i != j) { literals_[j] = literals_[i]; }
			if (swLev_ != uint32(-1)) {
				uint32 xLev = solver_->value(x.var()) == value_free ? uint32(-1) : solver_->level(x.var());
				if (swLev_ < xLev) {
					swLev_ = xLev;
					sw     = (uint32)j;
				}
			}
			++j;
		}
	}
	literals_.erase(literals_.begin()+j, literals_.end());
	for (LitVec::iterator it = literals_.begin(), end = literals_.end(); it != end; ++it) {
		solver_->clearSeen(it->var());
	}
	if (literals_.size() >= 2 && sw != 1) {
		std::swap(literals_[1], literals_[sw]);
	}
	if (fwLev_ == UINT32_MAX) { literals_[0] = posLit(0); }
}

ClauseCreator::Status ClauseCreator::status(uint32 dl, uint32 fw, uint32 sw) {
	if (fw > varMax)  {
		if (fw == UINT32_MAX)      { return status_subsumed; }
		if ((fw^UINT32_MAX) >= sw) { return status_asserting;}
		return status_sat;
	}
	if (sw <= dl) {
		if (fw > dl) { return status_unit; }
		if (fw > sw) { return status_asserting; }
		return status_conflicting;
	}
	return status_open;
}
ClauseCreator::Status ClauseCreator::status() const {
	if (literals_.empty()) { return status_empty; }
	return status(solver_->decisionLevel(), Detail::levelAbstraction(*solver_, literals_[0]), swLev_);
}

ClauseCreator::Status ClauseCreator::status(const Solver& s, const Literal* clause_begin, const Literal* clause_end) {
	if (clause_end <= clause_begin) { return status_empty; }
	Watches w(s, clause_begin, clause_end, false);
	return status(s.decisionLevel(), w.abstr_f, w.abstr_s);
}

ClauseCreator::Status ClauseCreator::status(const Solver& s, const Watches& w, uint32 flags) {
	Status x    = status(s.decisionLevel(), w.abstr_f, w.abstr_s);
	bool notSat = (flags & clause_not_sat) != 0;
	bool notRoot= (flags & clause_not_root_sat) != 0;
	if (x == status_conflicting || (x == status_asserting && s.isFalse(w.lits[0]))) {
		x = (flags & clause_not_conflict) == 0 ? status_unit : status_conflicting;
	}
	else if (x == status_sat && (notSat || (notRoot && s.level(w.lits[0].var()) <= s.rootLevel()))) {
		x = status_subsumed;
	}
	return x;
}

ClauseCreator::Result ClauseCreator::end() {
	assert(solver_);
	Result ret(0);
	Solver& s     = *solver_;
	uint32  flags = clause_known_order | clause_not_sat | clause_not_conflict;
	if (extra_.learnt() && !isSentinel(s.sharedContext()->tagLiteral())) {
		extra_.setTagged(std::find(literals_.begin(), literals_.end(), ~s.sharedContext()->tagLiteral()) != literals_.end());
	}
	return ClauseCreator::create(s, literals_, flags, extra_);
}

ClauseCreator::Status ClauseCreator::initWatches(Solver& s, LitVec& lits, bool allFree) {
	uint32 size = static_cast<uint32>(lits.size());
	if (size < 2) return size == 1 ? status_unit : status_empty;
	if (allFree) {
		uint32 fw = 0, sw = 1;
		uint32 w  = s.strategies().initWatches;
		if (w == SolverStrategies::watch_rand) {
			RNG& r = s.strategies().rng;
			fw = r.irand(size);
			while ( (sw = r.irand(size)) == fw) {/*intentionally empty*/}
		}
		else if (w == SolverStrategies::watch_least) {
			uint32 watch[2] = {0, size-1};
			uint32 count[2] = {s.numWatches(~lits[0]), s.numWatches(~lits[size-1])};
			uint32 maxCount = count[0] < count[1];
			for (uint32 x = 1, end = size-1; count[maxCount] > 0u && x != end; ++x) {
				uint32 cxw = s.numWatches(~lits[x]);
				if (cxw < count[maxCount]) {
					if (cxw < count[1-maxCount]) {
						watch[maxCount]   = watch[1-maxCount];
						count[maxCount]   = count[1-maxCount];
						watch[1-maxCount] = x;
						count[1-maxCount] = cxw;
					}
					else {
						watch[maxCount]   = x;
						count[maxCount]   = cxw;
					}
				}
			}
			fw = watch[0];
			sw = watch[1];
		}
		std::swap(lits[0], lits[fw]);
		std::swap(lits[1], lits[sw]);
		return status_open;
	}
	else {
		Watches w(s, lits, false);
		return ClauseCreator::status(s.decisionLevel(), w.abstr_f, w.abstr_s);
	}
}

ClauseHead* ClauseCreator::newProblemClause(Solver& s, LitVec& lits, const ClauseInfo& e, uint32 flags) {
	ClauseHead* ret;
	initWatches(s, lits, true);
	if (lits.size() <= Clause::MAX_SHORT_LEN || !s.sharedContext()->physicalShareProblem()) {
		ret = Clause::newClause(s, &lits[0], (uint32)lits.size(), e);
	}
	else {
		ret = Clause::newShared(s, SharedLiterals::newShareable(lits, e.type(), 1), e, &lits[0], false);
	}
	if ( (flags & clause_no_add) == 0 ) {
		s.add(ret);
	}
	return ret;
}

ClauseHead* ClauseCreator::newLearntClause(Solver& s, LitVec& lits, const ClauseInfo& e, uint32 flags) {
	ClauseHead* ret;
	Detail::Sink sharedPtr(0);
	sharedPtr.clause = s.sharedContext()->distribute(s, &lits[0], static_cast<uint32>(lits.size()), e);
	if (lits.size() <= Clause::MAX_SHORT_LEN || sharedPtr.clause == 0) {
		if (!s.isFalse(lits[1]) || lits.size() < s.strategies().compress) {
			ret = Clause::newLearntClause(s, lits, e);
		}
		else {
			std::stable_sort(lits.begin()+2, lits.end(), Detail::GreaterLevel(s));
			ret = Clause::newContractedClause(s, lits, e, 2, true);
		}
	}
	else {
		ret              = Clause::newShared(s, sharedPtr.clause, e, &lits[0], false);
		sharedPtr.clause = 0;
	}
	if ( (flags & clause_no_add) == 0 ) {
		s.addLearnt(ret, (uint32)lits.size(), e.type());
	}
	return ret;
}

ClauseHead* ClauseCreator::newUnshared(Solver& s, SharedLiterals* clause, const Watches& w, const ClauseInfo& e) {
	LitVec temp; temp.reserve(clause->size());
	temp.assign(w.lits, w.lits+2);
	for (const Literal* x = clause->begin(), *end = clause->end(); x != end; ++x) {
		if (Detail::levelAbstraction(s, *x) > 0 && *x != temp[0] && *x != temp[1]) {
			temp.push_back(*x);
		}
	}
	return Clause::newClause(s, &temp[0], (uint32)temp.size(), e);
}

ClauseCreator::Result ClauseCreator::create(Solver& s, LitVec& lits, uint32 flags, const ClauseInfo& extra) {
	assert(s.decisionLevel() == 0 || extra.learnt());
	Result ret;
	if (lits.size() > 1) {
		Watches w(s, lits, (flags & clause_known_order) != 0);
		Status x   = status(s, w, flags);
		ret.status = x;
		if (x == status_subsumed || x == status_conflicting) { 
			return ret;
		}
		if (!extra.learnt() && s.satPrepro()) {
			if (!s.satPrepro()->addClause(lits)) { ret.status = status_conflicting; }
			return ret;
		}		
		s.heuristic()->newConstraint(s, &lits[0], lits.size(), extra.type());
		if (lits.size() > 3 || extra.tagged() || (flags&clause_explicit) != 0) {
			ret.local = extra.learnt() ? newLearntClause(s, lits, extra, flags) : newProblemClause(s, lits, extra, flags);
		}
		else {
			s.addShort(&lits[0], lits.size(), extra);
		}
		if ((x & status_unit) != 0) {
			Antecedent ante(ret.local);
			if (!ret.local){ ante = lits.size() == 3 ? Antecedent(~lits[1], ~lits[2]) : Antecedent(~lits[1]); }
			if (!s.force(lits[0], s.level(lits[1].var()), ante)) { ret.status = status_conflicting; }
		}
	}
	else {
		Literal imp = !lits.empty()	? lits[0] : negLit(0);
		if (!s.addUnary(imp, extra.type())) { ret.status = status_conflicting; }
	}	
	return ret;
}


ClauseCreator::Result ClauseCreator::integrate(Solver& s, SharedLiterals* clause, uint32 modeFlags, ConstraintType t) {
	assert(!s.hasConflict() && "ClauseCreator::integrate() - precondition violated!");
	Detail::Sink shared( (modeFlags & clause_no_release) == 0 ? clause : 0);
	// determine state of clause
	Result  result;
	Watches w(s, clause->begin(), clause->end(), (modeFlags & clause_known_order) != 0);
	Status  stat( status(s, w, modeFlags) );
	result.status = stat;
	if (stat != status_subsumed && (stat != status_conflicting || (modeFlags & clause_not_conflict) == 0)) {
		ClauseInfo e(t);
		s.heuristic()->newConstraint(s, clause->begin(), clause->size(), t);
		uint32 impSize  = (modeFlags & clause_explicit) != 0 || !s.sharedContext()->allowImplicit(t) ? 1 : 3;
		if (w.simpSize > Clause::MAX_SHORT_LEN && s.sharedContext()->physicalShare(t)) {
			result.local  = Clause::newShared(s, clause, e, w.lits, shared.clause == 0);
			shared.clause = 0;
		}
		else if (w.simpSize > impSize) {
			result.local  = w.simpSize <= Clause::MAX_SHORT_LEN
			              ? Clause::newClause(s, w.lits, w.simpSize, e)
			              : newUnshared(s, clause, w, e);
		}
		else {
			// unary clause or implicitly shared via binary/ternary implication graph;
			// only check for implication/conflict but do not create
			// a local representation for the clause
			s.stats.addLearnt(w.simpSize, e.type());
			modeFlags    |= clause_no_add;
		}
		if ((modeFlags & clause_no_add) == 0) { s.addLearnt(result.local, w.simpSize, e.type()); }
	}
	if ((stat & status_unit) != 0) {
		Antecedent ante = result.local ? Antecedent(result.local) : Antecedent(~w.lits[1], ~w.lits[2]);
		uint32 impLevel = s.level(w.lits[1].var());
		result.status   = s.force(w.lits[0], impLevel, ante) ? stat : status_conflicting;
		if (result.local && (modeFlags & clause_int_lbd) != 0 && result.status != status_conflicting) {
			result.local->lbd(s.updateLearnt(negLit(0), clause->begin(), clause->end(), result.local->lbd(), true));
		}
	}
	return result;
}
ClauseCreator::Result ClauseCreator::integrate(Solver& s, SharedLiterals* clause, uint32 modeFlags) { 
	return integrate(s, clause, modeFlags, clause->type());
}
/////////////////////////////////////////////////////////////////////////////////////////
// Clause
/////////////////////////////////////////////////////////////////////////////////////////
void* Clause::alloc(Solver& s, uint32 lits, bool learnt) {
	if (lits <= Clause::MAX_SHORT_LEN) {
		if (learnt) { s.addLearntBytes(32); }
		return s.allocSmall();
	}
	uint32 extra = std::max((uint32)ClauseHead::HEAD_LITS, lits) - ClauseHead::HEAD_LITS; 
	uint32 bytes = sizeof(Clause) + (extra)*sizeof(Literal);
	if (learnt) { s.addLearntBytes(bytes); }
	return Detail::alloc(bytes);
}

ClauseHead* Clause::newClause(void* mem, Solver& s, const Literal* lits, uint32 size, const ClauseInfo& extra) {
	assert(size >= 2 && mem);
	return new (mem) Clause(s, extra, lits, size, size, false);
}

ClauseHead* Clause::newShared(Solver& s, SharedLiterals* shared_lits, const ClauseInfo& e, const Literal* lits, bool addRef) {
	return mt::SharedLitsClause::newClause(s, shared_lits, e, lits, addRef);
}

ClauseHead* Clause::newContractedClause(Solver& s, const LitVec& lits, const ClauseInfo& e, LitVec::size_type tailStart, bool extend) {
	assert(lits.size() >= 2);
	return new (alloc(s, (uint32)lits.size(), e.learnt())) Clause(s, e, &lits[0], static_cast<uint32>(lits.size()), static_cast<uint32>(tailStart), extend);
}

Clause::Clause(Solver& s, const ClauseInfo& e, const Literal* a_lits, uint32 size, uint32 tail, bool extend) 
	: ClauseHead(e) {
	assert(tail == size || s.isFalse(a_lits[tail]));
	data_.local.init(size);
	if (!isSmall()) {
		// copy literals
		std::memcpy(head_, a_lits, size*sizeof(Literal));
		tail = std::max(tail, (uint32)ClauseHead::HEAD_LITS);
		if (tail < size) {       // contracted clause
			head_[size-1].watch(); // mark last literal of clause
			Literal t = head_[tail];
			if (s.level(t.var()) > 0) {
				data_.local.markContracted();
				if (extend) {
					s.addUndoWatch(s.level(t.var()), this);
				}
			}
			data_.local.setSize(tail);
		}
	}
	else {
		std::memcpy(head_, a_lits, std::min(size, (uint32)ClauseHead::HEAD_LITS)*sizeof(Literal));
		data_.lits[0] = size > ClauseHead::HEAD_LITS   ? a_lits[ClauseHead::HEAD_LITS].asUint()   : negLit(0).asUint();
		data_.lits[1] = size > ClauseHead::HEAD_LITS+1 ? a_lits[ClauseHead::HEAD_LITS+1].asUint() : negLit(0).asUint();
		assert(isSmall() && Clause::size() == size);
	}
	attach(s);
}

Clause::Clause(Solver& s, const Clause& other) : ClauseHead(ClauseInfo()) {
	info_.rep      = other.info_.rep;
	uint32 oSize   = other.size();
	data_.local.init(oSize);
	if      (!isSmall())      { std::memcpy(head_, other.head_, oSize*sizeof(Literal)); }
	else if (other.isSmall()) { std::memcpy(data_.lits, other.data_.lits, (ClauseHead::MAX_SHORT_LEN+1)*sizeof(Literal)); }
	else { // this is small, other is not
		std::memcpy(head_, other.head_, ClauseHead::HEAD_LITS*sizeof(Literal));
		std::memcpy(data_.lits, other.head_+ClauseHead::HEAD_LITS, 2*sizeof(Literal));
	}
	attach(s);
}

Constraint* Clause::cloneAttach(Solver& other) {
	assert(!learnt());
	return new (alloc(other, Clause::size(), false)) Clause(other, *this);
}

void Clause::destroy(Solver* s, bool detachFirst) {
	if (s) { 
		if (detachFirst) { Clause::detach(*s); }
		if (learnt())    { s->freeLearntBytes(computeAllocSize()); }
	}
	void* mem   = static_cast<Constraint*>(this);
	bool  small = isSmall();
	this->~Clause();
	if (!small) { Detail::free(mem); }
	else if (s) { s->freeSmall(mem); }
}

void Clause::detach(Solver& s) {
	if (contracted()) {
		Literal* eoc = longEnd();
		if (s.isFalse(*eoc) && s.level(eoc->var()) != 0) {
			s.removeUndoWatch(s.level(eoc->var()), this);
		}
	}
	ClauseHead::detach(s);
}

uint32 Clause::computeAllocSize() const {
	if (isSmall()) { return 32; }
	uint32 rt = sizeof(Clause) - (ClauseHead::HEAD_LITS*sizeof(Literal));
	uint32 sz = data_.local.size();
	uint32 nw = contracted() + strengthened();
	if (nw != 0u) {
		const Literal* eoc = head_ + sz;
		do { nw -= eoc++->watched(); } while (nw); 
		sz = static_cast<uint32>(eoc - head_);
	}
	return rt + (sz*sizeof(Literal));
}

bool Clause::updateWatch(Solver& s, uint32 pos) {
	uint32 idx = data_.local.idx;
	if (!isSmall()) {
		for (Literal* tailS = head_ + ClauseHead::HEAD_LITS, *end = longEnd();;) {
			for (Literal* it  = tailS + idx; it < end; ++it) {
				if (!s.isFalse(*it)) {
					std::swap(*it, head_[pos]);
					data_.local.idx = static_cast<uint32>(++it - tailS);
					return true;
				}
			}
			if (idx == 0) { break; }
			end = tailS + idx;
			idx = 0;
		}
	}
	else if (!s.isFalse(Literal::fromRep(data_.lits[idx=0])) || !s.isFalse(Literal::fromRep(data_.lits[idx=1]))) {
		std::swap(head_[pos].asUint(), data_.lits[idx]);
		return true;
	}
	return false;
}

void Clause::reason(Solver& s, Literal p, LitVec& out) {
	LitVec::size_type i = out.size();
	out.push_back(~head_[p == head_[0]]);
	if (!isSentinel(head_[2])) {
		out.push_back(~head_[2]);
		LitRange t = tail();
		for (const Literal* r = t.first; r != t.second; ++r) {
			out.push_back(~*r);
		}
		if (contracted()) {
			const Literal* r = t.second;
			do { out.push_back(~*r); } while (!r++->watched());
		}
	}
	if (learnt()) { 
		ClauseHead::bumpActivity();
		setLbd(s.updateLearnt(p, &out[0]+i, &out[0]+out.size(), lbd(), !hasLbd())); 
	}
}

bool Clause::minimize(Solver& s, Literal p, CCMinRecursive* rec) {
	ClauseHead::bumpActivity();
	uint32 other = p == head_[0];
	if (!s.ccMinimize(~head_[other], rec) || !s.ccMinimize(~head_[2], rec)) { return false; }
	LitRange t = tail();
	for (const Literal* r = t.first; r != t.second; ++r) {
		if (!s.ccMinimize(~*r, rec)) { return false; }
	}
	if (contracted()) {
		do {
			if (!s.ccMinimize(~*t.second, rec)) { return false; }
		} while (!t.second++->watched());
	}
	return true;
}

bool Clause::isReverseReason(const Solver& s, Literal p, uint32 maxL, uint32 maxN) {
	uint32 other   = p == head_[0];
	if (!isRevLit(s, head_[other], maxL) || !isRevLit(s, head_[2], maxL)) { return false; }
	uint32 notSeen = !s.seen(head_[other].var()) + !s.seen(head_[2].var());
	LitRange t     = tail();
	for (const Literal* r = t.first; r != t.second && notSeen <= maxN; ++r) {
		if (!isRevLit(s, *r, maxL)) { return false; }
		notSeen += !s.seen(r->var());
	}
	if (contracted()) {
		const Literal* r = t.second;
		do { notSeen += !s.seen(r->var()); } while (notSeen <= maxN && !r++->watched());
	}
	return notSeen <= maxN;
}

bool Clause::simplify(Solver& s, bool reinit) {
	assert(s.decisionLevel() == 0 && s.queueSize() == 0);
	if (ClauseHead::satisfied(s)) {
		detach(s);
		return true;
	}
	LitRange t = tail();
	Literal* it= t.first - !isSmall(), *j;
	// skip free literals
	while (it != t.second && s.value(it->var()) == value_free) { ++it; }
	// copy remaining free literals
	for (j = it; it != t.second; ++it) {
		if      (s.value(it->var()) == value_free) { *j++ = *it; }
		else if (s.isTrue(*it)) { Clause::detach(s); return true;}
	}
	// replace any false lits with sentinels
	for (Literal* r = j; r != t.second; ++r) { *r = negLit(0); }
	if (!isSmall()) {
		uint32 size     = std::max((uint32)ClauseHead::HEAD_LITS, static_cast<uint32>(j-head_));
		data_.local.idx = 0;
		data_.local.setSize(size);
		if (j != t.second && learnt() && !strengthened()) {
			// mark last literal so that we can recompute alloc size later
			t.second[-1].watch();
			data_.local.markStrengthened();
		}
		if (reinit && size > 3) {
			detach(s);
			std::random_shuffle(head_, j, s.strategies().rng);
			attach(s);	
		}
	}
	else if (s.isFalse(head_[2])) {
		head_[2]   = t.first[0];
		t.first[0] = t.first[1];
		t.first[1] = negLit(0);
		--j;
	}
	return j <= t.first && ClauseHead::toImplication(s);
}

uint32 Clause::isOpen(const Solver& s, const TypeSet& x, LitVec& freeLits) {
	if (!x.inSet(ClauseHead::type()) || ClauseHead::satisfied(s)) {
		return 0;
	}
	assert(s.queueSize() == 0 && "Watches might be false!");
	freeLits.push_back(head_[0]);
	freeLits.push_back(head_[1]);
	if (!s.isFalse(head_[2])) freeLits.push_back(head_[2]);
	ValueRep v;
	LitRange t = tail();
	for (Literal* r = t.first; r != t.second; ++r) {
		if ( (v = s.value(r->var())) == value_free) {
			freeLits.push_back(*r);
		}
		else if (v == trueValue(*r)) {
			std::swap(head_[2], *r);
			return 0;
		}
	}
	return ClauseHead::type();
}

void Clause::undoLevel(Solver& s) {
	assert(!isSmall());
	uint32   t = data_.local.size();
	Literal* r = head_+t;
	while (!r->watched() && s.value(r->var()) == value_free) {
		++t;
		++r;
	}
	if (r->watched() || s.level(r->var()) == 0) {
		r->clearWatch();
		t += !isSentinel(*r);
		data_.local.clearContracted();
	}
	else {
		s.addUndoWatch(s.level(r->var()), this);
	}
	data_.local.setSize(t);
}

void Clause::toLits(LitVec& out) const {
	out.insert(out.end(), head_, (head_+ClauseHead::HEAD_LITS)-isSentinel(head_[2]));
	LitRange t = const_cast<Clause&>(*this).tail();
	if (contracted()) { while (!t.second++->watched()) {;} }
	out.insert(out.end(), t.first, t.second);
}

ClauseHead::BoolPair Clause::strengthen(Solver& s, Literal p, bool toShort) {
	LitRange t  = tail();
	Literal* eoh= head_+ClauseHead::HEAD_LITS;
	Literal* eot= t.second;
	Literal* it = std::find(head_, eoh, p);
	BoolPair ret(false, false);
	if (it != eoh) {
		if (it != head_+2) { // update watch
			*it = head_[2];
			s.removeWatch(~p, this);
			Literal* best = it, *n;
			for (n = t.first; n != eot && s.isFalse(*best); ++n) {
				if (!s.isFalse(*n) || s.level(n->var()) > s.level(best->var())) { 
					best = n; 
				}
			}
			std::swap(*it, *best);
			s.addWatch(~*it, ClauseWatch(this));
			it = head_+2;
		}	
		// replace cache literal with literal from tail
		if ((*it  = *t.first) != negLit(0)) {
			eot     = removeFromTail(s, t.first, eot);
		}
		ret.first = true;
	}
	else if ((it = std::find(t.first, eot, p)) != eot) {
		eot       = removeFromTail(s, it, eot);
		ret.first = true;
	}
	else if (contracted()) {
		for (; *it != p && !it->watched(); ++it) { ; }
		ret.first = *it == p;
		eot       = *it == p ? removeFromTail(s, it, eot) : it + 1;
	}
	if (ret.first && ~p == s.sharedContext()->tagLiteral()) {
		clearTagged();
	}
	ret.second = toShort && eot == t.first && toImplication(s);
	return ret;
}

Literal* Clause::removeFromTail(Solver& s, Literal* it, Literal* end) {
	assert(it != end || contracted());
	if (!contracted()) {
		*it  = *--end;
		*end = negLit(0);
		if (!isSmall()) { 
			data_.local.setSize(data_.local.size()-1);
			data_.local.idx = 0;
		}
	}
	else {
		uint32 uLev  = s.level(end->var());
		Literal* j   = it;
		while ( !j->watched() ) { *j++ = *++it; }
		*j           = negLit(0);
		uint32 nLev  = s.level(end->var());
		if (uLev != nLev && s.removeUndoWatch(uLev, this) && nLev != 0) {
			s.addUndoWatch(nLev, this);
		}
		if (j != end) { (j-1)->watch(); }
		else          { data_.local.clearContracted(); }
		end = j;
	}
	if (learnt() && !isSmall() && !strengthened()) {
		end->watch();
		data_.local.markStrengthened();
	}
	return end;
}
uint32 Clause::size() const {
	LitRange t = const_cast<Clause&>(*this).tail();
	return !isSentinel(head_[2])
		? 3u + static_cast<uint32>(t.second - t.first)
		: 2u;
}
/////////////////////////////////////////////////////////////////////////////////////////
// mt::SharedLitsClause
/////////////////////////////////////////////////////////////////////////////////////////
namespace mt {
ClauseHead* SharedLitsClause::newClause(Solver& s, SharedLiterals* shared_lits, const ClauseInfo& e, const Literal* lits, bool addRef) {
	return new (s.allocSmall()) SharedLitsClause(s, shared_lits, lits, e, addRef);
}

SharedLitsClause::SharedLitsClause(Solver& s, SharedLiterals* shared_lits, const Literal* w, const ClauseInfo& e, bool addRef) 
	: ClauseHead(e) {
	static_assert(sizeof(SharedLitsClause) <= 32, "Unsupported Padding");
	data_.shared = addRef ? shared_lits->share() : shared_lits;
	std::memcpy(head_, w, std::min((uint32)ClauseHead::HEAD_LITS, shared_lits->size())*sizeof(Literal));
	attach(s);
	if (learnt()) { s.addLearntBytes(32); }
}

Constraint* SharedLitsClause::cloneAttach(Solver& other) {
	return SharedLitsClause::newClause(other, data_.shared, ClauseInfo(this->type()), head_);
}

bool SharedLitsClause::updateWatch(Solver& s, uint32 pos) {
	Literal  other = head_[1^pos];
	for (const Literal* r = data_.shared->begin(), *end = data_.shared->end(); r != end; ++r) {
		// at this point we know that head_[2] is false so we only need to check 
		// that we do not watch the other watched literal twice!
		if (!s.isFalse(*r) && *r != other) {
			head_[pos] = *r; // replace watch
			// try to replace cache literal
			switch( std::min(static_cast<uint32>(8), static_cast<uint32>(end-r)) ) {
				case 8: if (!s.isFalse(*++r) && *r != other) { head_[2] = *r; return true; }
				case 7: if (!s.isFalse(*++r) && *r != other) { head_[2] = *r; return true; }
				case 6: if (!s.isFalse(*++r) && *r != other) { head_[2] = *r; return true; }
				case 5: if (!s.isFalse(*++r) && *r != other) { head_[2] = *r; return true; }
				case 4: if (!s.isFalse(*++r) && *r != other) { head_[2] = *r; return true; }
				case 3: if (!s.isFalse(*++r) && *r != other) { head_[2] = *r; return true; }
				case 2: if (!s.isFalse(*++r) && *r != other) { head_[2] = *r; return true; }
				default: return true;
			}
		}
	}
	return false;
}

void SharedLitsClause::reason(Solver& s, Literal p, LitVec& out) {
	LitVec::size_type i = out.size();
	for (const Literal* r = data_.shared->begin(), *end = data_.shared->end(); r != end; ++r) {
		assert(s.isFalse(*r) || *r == p);
		if (*r != p) { out.push_back(~*r); }
	}
	if (learnt()) { 
		ClauseHead::bumpActivity();
		setLbd(s.updateLearnt(p, &out[0]+i, &out[0]+out.size(), lbd(), !hasLbd())); 
	}
}

bool SharedLitsClause::minimize(Solver& s, Literal p, CCMinRecursive* rec) {
	ClauseHead::bumpActivity();
	for (const Literal* r = data_.shared->begin(), *end = data_.shared->end(); r != end; ++r) {
		if (*r != p && !s.ccMinimize(~*r, rec)) { return false; }
	}
	return true;
}

bool SharedLitsClause::isReverseReason(const Solver& s, Literal p, uint32 maxL, uint32 maxN) {
	uint32 notSeen = 0;
	for (const Literal* r = data_.shared->begin(), *end = data_.shared->end(); r != end; ++r) {
		if (*r == p) continue;
		if (!isRevLit(s, *r, maxL)) return false;
		if (!s.seen(r->var()) && ++notSeen > maxN) return false;
	}
	return true;
}

bool SharedLitsClause::simplify(Solver& s, bool reinit) {
	if (ClauseHead::satisfied(s)) {
		detach(s);
		return true;
	}
	uint32 optSize = data_.shared->simplify(s);
	if (optSize == 0) {
		detach(s);
		return true;
	}
	else if (optSize <= Clause::MAX_SHORT_LEN) {
		Literal lits[Clause::MAX_SHORT_LEN];
		Literal* j = lits;
		for (const Literal* r = data_.shared->begin(), *e = data_.shared->end(); r != e; ++r) {
			if (!s.isFalse(*r)) *j++ = *r;
		}
		uint32 rep= info_.rep;
		// detach & destroy but do not release memory
		detach(s);
		SharedLitsClause::destroy(0, false);
		// construct short clause in "this"
		ClauseHead* h = Clause::newClause(this, s, lits, static_cast<uint32>(j-lits), ClauseInfo());
		// restore extra data - safe because memory layout has not changed!
		info_.rep = rep;
		return h->simplify(s, reinit);
	}
	else if (s.isFalse(head_[2])) {
		// try to replace cache lit with non-false literal
		for (const Literal* r = data_.shared->begin(), *e = data_.shared->end(); r != e; ++r) {
			if (!s.isFalse(*r) && std::find(head_, head_+2, *r) == head_+2) {
				head_[2] = *r;
				break;
			}
		}
	}
	return false;
}

void SharedLitsClause::destroy(Solver* s, bool detachFirst) {
	if (s && detachFirst) {
		ClauseHead::detach(*s);
	}
	data_.shared->release();
	void* mem = this;
	this->~SharedLitsClause();
	if (s) {
		// return memory to solver
		s->freeLearntBytes(32);
		s->freeSmall(mem);
	}
}

uint32 SharedLitsClause::isOpen(const Solver& s, const TypeSet& x, LitVec& freeLits) {
	if (!x.inSet(ClauseHead::type()) || ClauseHead::satisfied(s)) {
		return 0;
	}
	Literal* head = head_;
	ValueRep v;
	for (const Literal* r = data_.shared->begin(), *end = data_.shared->end(); r != end; ++r) {
		if ( (v = s.value(r->var())) == value_free ) {
			freeLits.push_back(*r);
		}
		else if (v == trueValue(*r)) {
			head[2] = *r; // remember as cache literal
			return 0;
		}
	}
	return ClauseHead::type();
}

void SharedLitsClause::toLits(LitVec& out) const {
	out.insert(out.end(), data_.shared->begin(), data_.shared->end());
}

ClauseHead::BoolPair SharedLitsClause::strengthen(Solver&, Literal, bool) {
	return BoolPair(false, false);
}

uint32 SharedLitsClause::size() const { return data_.shared->size(); }

} // end namespace mt

/////////////////////////////////////////////////////////////////////////////////////////
// LoopFormula
/////////////////////////////////////////////////////////////////////////////////////////
LoopFormula* LoopFormula::newLoopFormula(Solver& s, Literal* bodyLits, uint32 numBodies, uint32 bodyToWatch, uint32 numAtoms, const Activity& act) {
	uint32 bytes = sizeof(LoopFormula) + (numBodies+numAtoms+3) * sizeof(Literal);
	void* mem    = Detail::alloc( bytes );
	s.addLearntBytes(bytes);
	return new (mem) LoopFormula(s, numBodies+numAtoms, bodyLits, numBodies, bodyToWatch, act);
}
LoopFormula::LoopFormula(Solver& s, uint32 size, Literal* bodyLits, uint32 numBodies, uint32 bodyToWatch, const Activity& act)
	: act_(act) {
	end_          = numBodies + 2;
	size_         = end_+1;
	other_        = end_-1;
	lits_[0]      = Literal();  // Starting sentinel
	lits_[end_-1] = Literal();  // Position of active atom
	lits_[end_]   = Literal();  // Ending sentinel - active part
	for (uint32 i = size_; i != size+3; ++i) {
		lits_[i] = Literal();
	}
	// copy bodies: S B1...Bn, watch one
	std::memcpy(lits_+1, bodyLits, numBodies * sizeof(Literal));
	s.addWatch(~lits_[1+bodyToWatch], this, ((1+bodyToWatch)<<1)+1);
	lits_[1+bodyToWatch].watch();
}

void LoopFormula::destroy(Solver* s, bool detach) {
	if (s) {
		if (detach) {
			for (uint32 x = 1; x != end_-1; ++x) {
				if (lits_[x].watched()) {
					s->removeWatch(~lits_[x], this);
					lits_[x].clearWatch();
				}
			}
			if (lits_[end_-1].watched()) {
				lits_[end_-1].clearWatch();
				for (uint32 x = end_+1; x != size_; ++x) {
					s->removeWatch(~lits_[x], this);
					lits_[x].clearWatch();
				}
			}
		}
		if (lits_[0].watched()) {
			while (lits_[size_++].asUint() != 3u) { ; }
		}
		s->freeLearntBytes(sizeof(LoopFormula) + (size_ * sizeof(Literal)));
	}
	void* mem = static_cast<Constraint*>(this);
	this->~LoopFormula();
	Detail::free(mem);
}


void LoopFormula::addAtom(Literal atom, Solver& s) {
	act_.bumpAct();
	uint32 pos = size_++;
	assert(isSentinel(lits_[pos]));
	lits_[pos] = atom;
	lits_[pos].watch();
	s.addWatch( ~lits_[pos], this, (pos<<1)+0 );
	if (isSentinel(lits_[end_-1])) {
		lits_[end_-1] = lits_[pos];
	}
}

void LoopFormula::updateHeuristic(Solver& s) {
	Literal saved = lits_[end_-1];
	for (uint32 x = end_+1; x != size_; ++x) {
		lits_[end_-1] = lits_[x];
		s.heuristic()->newConstraint(s, lits_+1, end_-1, Constraint_t::learnt_loop);
	}
	lits_[end_-1] = saved;
}

bool LoopFormula::watchable(const Solver& s, uint32 idx) {
	assert(!lits_[idx].watched());
	if (idx == end_-1) {
		for (uint32 x = end_+1; x != size_; ++x) {
			if (s.isFalse(lits_[x])) {
				lits_[idx] = lits_[x];
				return false;
			}
		}
	}
	return true;
}

bool LoopFormula::isTrue(const Solver& s, uint32 idx) {
	if (idx != end_-1) return s.isTrue(lits_[idx]);
	for (uint32 x = end_+1; x != size_; ++x) {
		if (!s.isTrue(lits_[x])) {
			lits_[end_-1] = lits_[x];
			return false;
		}
	}
	return true;
}

Constraint::PropResult LoopFormula::propagate(Solver& s, Literal, uint32& data) {
	if (isTrue(s, other_)) {          // ignore clause, as it is 
		return PropResult(true, true);  // already satisfied
	}
	uint32  pos   = data >> 1;
	uint32  idx   = pos;
	if (pos > end_) {
		// p is one of the atoms - move to active part
		lits_[end_-1] = lits_[pos];
		idx           = end_-1;
	}
	int     dir   = ((data & 1) << 1) - 1;
	int     bounds= 0;
	for (;;) {
		for (idx+=dir;s.isFalse(lits_[idx]);idx+=dir) {;} // search non-false literal - sentinels guarantee termination
		if (isSentinel(lits_[idx])) {             // Hit a bound,
			if (++bounds == 2) {                    // both ends seen, clause is unit, false, or sat
				if (other_ == end_-1) {
					uint32 x = end_+1;
					for (; x != size_ && s.force(lits_[x], this);  ++x) { ; }
					return Constraint::PropResult(x == size_, true);  
				}
				else {
					return Constraint::PropResult(s.force(lits_[other_], this), true);  
				}
			}
			idx   = std::min(pos, end_-1);          // halfway through, restart search, but
			dir   *= -1;                            // this time walk in the opposite direction.
			data  ^= 1;                             // Save new direction of watch
		}
		else if (!lits_[idx].watched() && watchable(s, idx)) { // found a new watchable literal
			if (pos > end_) {     // stop watching atoms
				lits_[end_-1].clearWatch();
				for (uint32 x = end_+1; x != size_; ++x) {
					if (x != pos) {
						s.removeWatch(~lits_[x], this);
						lits_[x].clearWatch();
					}
				}
			}
			lits_[pos].clearWatch();
			lits_[idx].watch();
			if (idx == end_-1) {  // start watching atoms
				for (uint32 x = end_+1; x != size_; ++x) {
					s.addWatch(~lits_[x], this, static_cast<uint32>(x << 1) + 0);
					lits_[x].watch();
				}
			}
			else {
				s.addWatch(~lits_[idx], this, static_cast<uint32>(idx << 1) + (dir==1));
			}
			return Constraint::PropResult(true, false);
		} 
		else if (lits_[idx].watched()) {          // Hit the other watched literal
			other_  = idx;                          // Store it in other_
		}
	}
}

// Body: all other bodies + active atom
// Atom: all bodies
void LoopFormula::reason(Solver& s, Literal p, LitVec& lits) {
	uint32 os = lits.size();
	// all relevant bodies
	for (uint32 x = 1; x != end_-1; ++x) {
		if (lits_[x] != p) {
			lits.push_back(~lits_[x]);
		}
	}
	// if p is a body, add active atom
	if (other_ != end_-1) {
		lits.push_back(~lits_[end_-1]);
	}
	act_.setLbd(s.updateLearnt(p, &lits[0]+os, &lits[0]+lits.size(), act_.lbd()));
	act_.bumpAct();
}

bool LoopFormula::minimize(Solver& s, Literal p, CCMinRecursive* rec) {
	act_.bumpAct();
	for (uint32 x = 1; x != end_-1; ++x) {
		if (lits_[x] != p && !s.ccMinimize(~lits_[x], rec)) {
			return false;
		}
	}
	return other_ == end_-1
		|| s.ccMinimize(~lits_[end_-1], rec);
}

uint32 LoopFormula::size() const {
	return size_ - 3;
}

bool LoopFormula::locked(const Solver& s) const {
	if (other_ != end_-1) {
		return s.isTrue(lits_[other_]) && s.reason(lits_[other_]) == this;
	}
	for (uint32 x = end_+1; x != size_; ++x) {
		if (s.isTrue(lits_[x]) && s.reason(lits_[x]) == this) {
			return true;
		}
	}
	return false;
}

uint32 LoopFormula::isOpen(const Solver& s, const TypeSet& x, LitVec& freeLits) {
	if (!x.inSet(Constraint_t::learnt_loop) || (other_ != end_-1 && s.isTrue(lits_[other_]))) {
		return 0;
	}
	for (uint32 x = 1; x != end_-1; ++x) {
		if (s.isTrue(lits_[x])) {
			other_ = x;
			return 0;
		}
		else if (!s.isFalse(lits_[x])) { freeLits.push_back(lits_[x]); }
	}
	for (uint32 x = end_+1; x != size_; ++x) {
		if      (s.value(lits_[x].var()) == value_free) { freeLits.push_back(lits_[x]); }
		else if (s.isTrue(lits_[x]))                    { return 0; }
	}
	return Constraint_t::learnt_loop;
}

bool LoopFormula::simplify(Solver& s, bool) {
	assert(s.decisionLevel() == 0);
	typedef std::pair<uint32, uint32> WatchPos;
	bool      sat = false;          // is the constraint SAT?
	WatchPos  bodyWatches[2];       // old/new position of watched bodies
	uint32    bw  = 0;              // how many bodies are watched?
	uint32    j   = 1, i;
	// 1. simplify the set of bodies:
	// - search for a body that is true -> constraint is SAT
	// - remove all false bodies
	// - find the watched bodies
	for (i = 1; i != end_-1; ++i) {
		assert( !s.isFalse(lits_[i]) || !lits_[i].watched() ); // otherwise should be asserting 
		if (!s.isFalse(lits_[i])) {
			sat |= s.isTrue(lits_[i]);
			if (i != j) { lits_[j] = lits_[i]; }
			if (lits_[j].watched()) { bodyWatches[bw++] = WatchPos(i, j); }
			++j;
		}
	}
	uint32  newEnd    = j + 1;
	uint32  numBodies = j - 1;
	j += 2;
	// 2. simplify the set of atoms:
	// - remove all determined atoms
	// - remove/update watches if necessary
	for (i = end_ + 1; i != size_; ++i) {
		if (s.value(lits_[i].var()) == value_free) {
			if (i != j) { lits_[j] = lits_[i]; }
			if (lits_[j].watched()) {
				if (sat || numBodies <= 2) {
					s.removeWatch(~lits_[j], this);
					lits_[j].clearWatch();
				}
				else if (i != j) {
					GenericWatch* w = s.getWatch(~lits_[j], this);
					assert(w);
					w->data = (j << 1) + 0;
				}
			}
			++j;
		}
		else if (lits_[i].watched()) {
			s.removeWatch(~lits_[i], this);
			lits_[i].clearWatch();
		}
	}
	if (j != size_ && !lits_[0].watched()) {
		lits_[size_-1].asUint() = 3u;
		lits_[0].watch();
	}
	size_         = j;
	end_          = newEnd;
	lits_[end_]   = Literal();
	lits_[end_-1] = lits_[end_+1];
	if (sat || numBodies < 3 || size_ == end_ + 1) {
		for (i = 0; i != bw; ++i) {
			s.removeWatch(~lits_[bodyWatches[i].second], this);
			lits_[bodyWatches[i].second].clearWatch();
		}
		if (sat || size_ == end_+1) { return true; }
		// replace constraint with short clauses
		ClauseCreator creator(&s);
		creator.start();
		for (i = 1; i != end_; ++i) { creator.add(lits_[i]); }
		for (i = end_+1; i != size_; ++i) {
			creator[creator.size()-1] = lits_[i];
			creator.end();
		}
		return true;
	}
	other_ = 1;
	for (i = 0; i != bw; ++i) {
		if (bodyWatches[i].first != bodyWatches[i].second) {
			GenericWatch* w  = s.getWatch(~lits_[bodyWatches[i].second], this);
			assert(w);
			w->data = (bodyWatches[i].second << 1) + (w->data&1);
		}
	}
	return false;
}
}
