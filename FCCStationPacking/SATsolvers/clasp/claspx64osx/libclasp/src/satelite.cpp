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
#include <clasp/satelite.h>
#include <clasp/clause.h>

#ifdef _MSC_VER
#pragma warning (disable : 4200) // nonstandard extension used : zero-sized array
#endif
namespace Clasp { namespace SatElite {

inline uint64 abstractLit(Literal p) {
	assert(p.var() > 0); 
	return uint64(1) << ((p.var()-1) & 63); 
}

// A clause class optimized for the SatElite preprocessor.
// Uses 1-literal watching to implement forward-subsumption
class Clause {
public:
	static Clause* newClause(const LitVec& lits) {
		void* mem = ::operator new( sizeof(Clause) + (lits.size()*sizeof(Literal)) );
		return new (mem) Clause(lits);
	}
	bool simplify(Solver& s) {
		uint32 i;
		for (i = 0; i != size_ && s.value(lits_[i].var()) == value_free; ++i) {;}
		if      (i == size_)          { return false; }
		else if (s.isTrue(lits_[i]))  { return true;  }
		uint32 j = i++;
		for (; i != size_; ++i) {
			if (s.isTrue(lits_[i]))   { return true; }
			if (!s.isFalse(lits_[i])) { lits_[j++] = lits_[i]; }
		}
		size_ = j;
		return false;
	}
	void destroy() {
		void* mem = this;
		this->~Clause();
		::operator delete(mem);
	}
	uint32          size()                const { return size_; }
	const Literal&  operator[](uint32 x)  const { return lits_[x]; }
	bool            inSQ()                const { return inSQ_ != 0; }
	uint64          abstraction()         const { return data_.abstr; }
	Clause*         next()                const { return data_.next;  }
	bool            blocked()             const { return inSQ_   != 0;  }
	bool            marked()              const { return marked_ != 0; }
	Literal&        operator[](uint32 x)        { return lits_[x]; }    
	void            setInSQ(bool b)             { inSQ_   = (uint32)b; }
	void            setMarked(bool b)           { marked_ = (uint32)b; }
	uint64&         abstraction()               { return data_.abstr; }
	void            strengthen(Literal p)       {
		uint64 a = 0;
		uint32 i, end;
		for (i   = 0; lits_[i] != p; ++i) { a |= abstractLit(lits_[i]); }
		for (end = size_-1; i < end; ++i) { lits_[i] = lits_[i+1]; a |= abstractLit(lits_[i]); }
		--size_;
		data_.abstr = a;
	}
	Clause*         linkRemoved(bool blocked, Clause* next) {
		inSQ_      = blocked;
		data_.next = next;
		return this;
	}
private:
	Clause(const LitVec& lits) : size_((uint32)lits.size()), inSQ_(0), marked_(0) {
		std::memcpy(lits_, &lits[0], lits.size()*sizeof(Literal));
	}
	union {
		uint64  abstr;      // abstraction - to speed up backward subsumption check
		Clause* next;       // next removed clause
	}       data_;
	uint32  size_   : 30; // size of the clause
	uint32  inSQ_   : 1;  // in subsumption-queue?
	uint32  marked_ : 1;  // a marker flag
	Literal lits_[0];     // literals of the clause: [lits_[0], lits_[size_])
};
/////////////////////////////////////////////////////////////////////////////////////////
// SatElite preprocessing
//
/////////////////////////////////////////////////////////////////////////////////////////
SatElite::SatElite(SharedContext* ctx) 
	: occurs_(0)
	, elimTop_(0)
	, elimHeap_(LessOccCost(occurs_))
	, qFront_(0)
	, facts_(0) {
	if (ctx) setContext(*ctx);
}

SatElite::~SatElite() {
	cleanUp();
	for (Clause* r = elimTop_; r;) {
		Clause* t = r;
		 r = r->next();
		 t->destroy();
	}
	elimTop_ = 0;
}

void SatElite::cleanUp() {
	delete [] occurs_;  occurs_ = 0; 
	for (ClauseList::size_type i = 0; i != clauses_.size(); ++i) {
		if (clauses_[i]) { clauses_[i]->destroy(); }
	}
	ClauseList().swap(clauses_);
	ClauseList().swap(resCands_);
	VarVec().swap(posT_);
	VarVec().swap(negT_);
	LitVec().swap(resolvent_);
	VarVec().swap(queue_);
	elimHeap_.clear();
	qFront_ = facts_ = 0;
}

bool SatElite::addClause(const LitVec& clause) {
	assert(ctx_ && ctx_->master());
	if (clause.empty()) {
		return false;
	}
	else if (clause.size() == 1) {
		return ctx_->master()->force(clause[0], 0) && ctx_->master()->propagate();
	}
	else {
		Clause* c = Clause::newClause( clause );
		clauses_.push_back( c );
	}
	return true;
}

Clause* SatElite::popSubQueue() {
	if (Clause* c = clauses_[ queue_[qFront_++] ]) {
		c->setInSQ(false);
		return c;
	}
	return 0;
}

void SatElite::addToSubQueue(uint32 clauseId) {
	assert(clauses_[clauseId] != 0);
	if (!clauses_[clauseId]->inSQ()) {
		queue_.push_back(clauseId);
		clauses_[clauseId]->setInSQ(true);
	}
}

void SatElite::attach(uint32 clauseId, bool initialClause) {
	Clause& c = *clauses_[clauseId];
	c.abstraction() = 0;
	for (uint32 i = 0; i != c.size(); ++i) {
		Var v = c[i].var();
		occurs_[v].add(clauseId, c[i].sign());
		occurs_[v].unmark();
		c.abstraction() |= abstractLit(c[i]);
		if (elimHeap_.is_in_queue(v)) {
			elimHeap_.decrease(v);
		}
		else if (initialClause) {
			updateHeap(v);
		}
	}
	occurs_[c[0].var()].addWatch(clauseId);
	addToSubQueue(clauseId);
	stats.clAdded += !initialClause;
}

void SatElite::detach(uint32 id) {
	Clause& c  = *clauses_[id];
	occurs_[c[0].var()].removeWatch(id);
	for (uint32 i = 0; i != c.size(); ++i) {
		Var v = c[i].var();
		occurs_[v].remove(id, c[i].sign(), false);
		updateHeap(v);
	}
	clauses_[id] = 0;
	c.destroy();
	++stats.clRemoved;
}

void SatElite::bceVeRemove(uint32 id, bool freeId, Var ev, bool blocked) {
	Clause& c  = *clauses_[id];
	occurs_[c[0].var()].removeWatch(id);
	uint32 pos = 0;
	for (uint32 i = 0; i != c.size(); ++i) {
		Var v = c[i].var();
		if (v != ev) {
			occurs_[v].remove(id, c[i].sign(), freeId);
			updateHeap(v);
		}
		else {
			occurs_[ev].remove(id, c[i].sign(), false);
			pos = i;
		}
	}
	clauses_[id] = 0;
	std::swap(c[0], c[pos]);
	elimTop_   = c.linkRemoved(blocked, elimTop_);
	++stats.clRemoved;
}

bool SatElite::preprocess(bool enumModels) {
	struct Scope { SatElite* self; char type; ~Scope() { 
		self->cleanUp();
		self->reportProgress(Event(self, type, 100,100));
	}} sc = {this, '*'};
	Solver* s = ctx_->master();
	// skip preprocessing if other constraints are UNSAT
	if (!s->propagate()) return false;
	// preprocess only if not too many vars are frozen
	double frozen = 0.0;
	double maxF   = std::min(1.0, options.maxFrozen/100.0);
	if (maxF != 1.0 && (frozen = ctx_->stats().vars_frozen) != 0.0) {
		for (LitVec::const_iterator it = s->trail().begin(), end = s->trail().end(); it != end; ++it) {
			frozen -= (ctx_->frozen(it->var()));
		}
		frozen = std::min(1.0,  frozen / double(s->numFreeVars()));
	}
	if (!SatElite::limit((uint32)clauses_.size()) && frozen <= maxF) {
		// 0. allocate & init state
		occurs_   = new OccurList[ctx_->numVars()+1];
		qFront_   = 0;
		if (enumModels) {
			options.elimPure = 0;
			options.bce      = 0;
		}
		occurs_[0].bce = (options.bce > 1);
		// 1. remove SAT-clauses, strengthen clauses w.r.t false literals, init occur-lists
		facts_ = s->numAssignedVars();
		ClauseList::size_type j = 0; 
		for (ClauseList::size_type i = 0; i != clauses_.size(); ++i) {
			Clause* c = clauses_[i]; assert(c);
			if      (c->simplify(*s)) { c->destroy(); clauses_[i] = 0;}
			else if (c->size() < 2)         {
				Literal unit = c->size() == 1 ? (*c)[0] : negLit(0);
				c->destroy(); clauses_[i] = 0;
				if (!(s->force(unit, 0)&&s->propagate()) || !propagateFacts()) {
					for (++i; i != clauses_.size(); ++i) {
						clauses_[i]->destroy();
					}
					clauses_.erase(clauses_.begin()+j, clauses_.end());
					return false;
				}
			}
			else                            {
				clauses_[j] = c;
				attach(uint32(j), true);
				++j;
			}
		}
		clauses_.erase(clauses_.begin()+j, clauses_.end());
		assert(facts_ == s->numAssignedVars());
		// simplify other constraints w.r.t new derived top-level facts
		if (!s->simplify()) return false;
		// 2. remove subsumed clauses, eliminate vars by clause distribution
		timeout_ = options.maxTime != UINT32_MAX ? time(0) + options.maxTime : std::numeric_limits<std::time_t>::max();
		for (uint32 i = 0, end = options.maxIters; queue_.size()+elimHeap_.size() > 0; ++i) {
			if (!backwardSubsume())          { return false; }
			if (timeout() || i == end)       { break; }
			if (!eliminateVars())            { return false; }
		}
		assert( facts_ == s->numAssignedVars() );
	}
	sc.type = '*';
	// simplify other constraints w.r.t new derived top-level facts
	if (!s->simplify()) return false;
	// 3. Transfer simplified clausal problem to solver
	ClauseCreator nc(s);
	for (ClauseList::size_type i = 0; i != clauses_.size(); ++i) {
		if (clauses_[i]) {
			Clause& c = *clauses_[i];
			nc.start();
			for (uint32 x = 0; x != c.size(); ++x) {  nc.add(c[x]);  }
			c.destroy();
			nc.end();
		}
	}
	clauses_.clear();
	return true;  
}

// (Destructive) unit propagation on clauses.
// Removes satisfied clauses and shortens clauses w.r.t. false literals.
// Pre:   Assignment is propagated w.r.t other non-clause constraints
// Post:  Assignment is fully propagated and no clause contains an assigned literal
bool SatElite::propagateFacts() {
	Solver* s = ctx_->master();
	assert(s->queueSize() == 0);
	while (facts_ != s->numAssignedVars()) {
		Literal l     = s->trail()[facts_++];
		OccurList& ov = occurs_[l.var()];
		ClRange cls   = occurs_[l.var()].clauseRange();
		for (ClIter x = cls.first; x != cls.second; ++x) {
			if      (clauses_[x->var()] == 0)        { continue; }
			else if (x->sign() == l.sign())          { detach(x->var()); }
			else if (!strengthenClause(x->var(), ~l)){ return false; }
		}
		ov.clear();
		ov.mark(!l.sign());
	}
	assert(s->queueSize() == 0);
	return true;
}

// Backward subsumption and self-subsumption resolution until fixpoint
bool SatElite::backwardSubsume() {
	if (!propagateFacts()) return false;
	while (qFront_ != queue_.size()) {
		if ((qFront_ & 8191) == 0) {
			if (timeout()) break;
			if (queue_.size() > 1000) reportProgress(Event(this, 'S', qFront_, queue_.size()));
		}
		if (peekSubQueue() == 0) { ++qFront_; continue; }
		Clause& c = *popSubQueue();
		// Try to minimize effort by testing against the var in c that occurs least often;
		Literal best  = c[0];
		for (uint32 i = 1; i < c.size(); ++i) {
			if (occurs_[c[i].var()].numOcc() < occurs_[best.var()].numOcc()) {
				best  = c[i];
			}
		}
		// Test against all clauses containing best
		ClWList& cls  = occurs_[best.var()].refs;
		Literal res   = negLit(0);
		uint32  j     = 0;
		// must use index access because cls might change!
		for (uint32 i = 0, end = cls.left_size(); i != end; ++i) {
			Literal cl      = cls.left(i);
			uint32 otherId  = cl.var();
			Clause* other   = clauses_[otherId];
			if (other && other!= &c && (res = subsumes(c, *other, best.sign()==cl.sign()?posLit(0):best)) != negLit(0)) {
				if (res == posLit(0)) {
					// other is subsumed - remove it
					detach(otherId);
					other = 0;
				}
				else {
					// self-subsumption resolution; other is subsumed by c\{res} U {~res}
					// remove ~res from other, add it to subQ so that we can check if it now subsumes c
					res = ~res;
					occurs_[res.var()].remove(otherId, res.sign(), res.var() != best.var());
					updateHeap(res.var());
					if (!strengthenClause(otherId, res))              { return false; }
					if (res.var() == best.var() || clauses_[otherId] == 0)  { other = 0; }
				}
			}
			if (other && j++ != i)  { cls.left(j-1) = cl; }
		}
		cls.shrink_left(cls.left_begin()+j);
		occurs_[best.var()].dirty = 0;
		assert(occurs_[best.var()].numOcc() == (uint32)cls.left_size());
		if (!propagateFacts()) return false;
	}   
	queue_.clear();
	qFront_ = 0;
	return true;
}

// checks if 'c' subsumes 'other', and at the same time, if it can be used to 
// simplify 'other' by subsumption resolution.
// Return:
//  - negLit(0) - No subsumption or simplification
//  - posLit(0) - 'c' subsumes 'other'
//  - l         - The literal l can be deleted from 'other'
Literal SatElite::subsumes(const Clause& c, const Clause& other, Literal res) const {
	if (other.size() < c.size() || (c.abstraction() & ~other.abstraction()) != 0) {
		return negLit(0);
	}
	if (c.size() < 10 || other.size() < 10) {
		for (uint32 i = 0; i != c.size(); ++i) {
			for (uint32 j = 0; j != other.size(); ++j) {
				if (c[i].var() == other[j].var()) {
					if (c[i].sign() == other[j].sign())     { goto found; }
					else if (res != posLit(0) && res!=c[i]) { return negLit(0); }
					res = c[i];
					goto found;
				}
			}
			return negLit(0); 
		found:;
		}
	}
	else {
		markAll(&other[0], other.size());
		for (uint32 i = 0; i != c.size(); ++i) {
			if (occurs_[c[i].var()].litMark == 0) { res = negLit(0); break; }
			if (occurs_[c[i].var()].marked(!c[i].sign())) {
				if (res != posLit(0)&&res!=c[i]) { res = negLit(0); break; }
				res = c[i];
			}
		}
		unmarkAll(&other[0], other.size());
	}
	return res;
}

uint32 SatElite::findUnmarkedLit(const Clause& c, uint32 x) const {
	for (; x != c.size() && occurs_[c[x].var()].marked(c[x].sign()); ++x)
		;
	return x;
}

// checks if 'cl' is subsumed by one of the existing clauses and at the same time
// strengthens 'cl' if possible.
// Return:
//  - true  - 'cl' is subsumed
//  - false - 'cl' is not subsumed but may itself subsume other clauses
// Pre: All literals of l are marked, i.e. 
// for each literal l in cl, occurs_[l.var()].marked(l.sign()) == true
bool SatElite::subsumed(LitVec& cl) {
	Literal l;
	uint32 x = 0;
	uint32 str = 0;
	LitVec::size_type j = 0;
	for (LitVec::size_type i = 0; i != cl.size(); ++i) {
		l = cl[i];
		if (occurs_[l.var()].litMark == 0) { --str; continue; }
		ClWList& cls   = occurs_[l.var()].refs; // right: all clauses watching either l or ~l
		WIter wj       = cls.right_begin();
		for (WIter w = wj, end = cls.right_end(); w != end; ++w) {
			Clause& c = *clauses_[*w];
			if (c[0] == l)  {
				if ( (x = findUnmarkedLit(c, 1)) == c.size() ) {
					while (w != end) { *wj++ = *w++; }
					cls.shrink_right( wj );
					return true;
				}
				c[0] = c[x];
				c[x] = l;
				occurs_[c[0].var()].addWatch(*w);
				if (occurs_[c[0].var()].litMark != 0 && findUnmarkedLit(c, x+1) == c.size()) {
					occurs_[c[0].var()].unmark();  // no longer part of cl
					++str;
				}
			}
			else if ( findUnmarkedLit(c, 1) == c.size() ) {
				occurs_[l.var()].unmark(); // no longer part of cl
				while (w != end) { *wj++ = *w++; }
				cls.shrink_right( wj );
				goto removeLit;
			}
			else { *wj++ = *w; }  
		}
		cls.shrink_right(wj);
		if (j++ != i) { cl[j-1] = cl[i]; }
removeLit:;
	}
	cl.erase(cl.begin()+j, cl.end());
	if (str > 0) {
		for (LitVec::size_type i = 0; i != cl.size();) {
			if (occurs_[cl[i].var()].litMark == 0) {
				cl[i] = cl.back();
				cl.pop_back();
				if (--str == 0) break;
			}
			else { ++i; }
		}
	}
	return false;
}

// Pre: c contains l
// Pre: c was already removed from l's occur-list
bool SatElite::strengthenClause(uint32 clauseId, Literal l) {
	Clause& c = *clauses_[clauseId];
	if (c[0] == l) {
		occurs_[c[0].var()].removeWatch(clauseId);
		// Note: Clause::strengthen shifts literals after l to the left. Thus
		// c[1] will be c[0] after strengthen
		occurs_[c[1].var()].addWatch(clauseId);
	}
	++stats.litsRemoved;
	c.strengthen(l);
	if (c.size() == 1) {
		Literal unit = c[0];
		detach(clauseId);
		return ctx_->master()->force(unit, 0) && ctx_->master()->propagate();
	}
	addToSubQueue(clauseId);
	return true;
}

// Split occurrences of v into pos and neg and 
// mark all clauses containing v
SatElite::ClRange SatElite::splitOcc(Var v, bool mark) {
	ClRange cls      = occurs_[v].clauseRange();
	occurs_[v].dirty = 0;
	posT_.clear(); negT_.clear();
	ClIter j = cls.first;
	for (ClIter x = j; x != cls.second; ++x) {
		if (Clause* c = clauses_[x->var()]) {
			assert(c->marked() == false);
			c->setMarked(mark);
			(x->sign() ? negT_ : posT_).push_back(x->var());
			if (j != x) *j = *x;
			++j;
		}
	}
	occurs_[v].refs.shrink_left(j);
	return occurs_[v].clauseRange();
}

void SatElite::markAll(const Literal* lits, uint32 size) const {
	for (uint32 i = 0; i != size; ++i) {
		occurs_[lits[i].var()].mark(lits[i].sign());
	}
}
void SatElite::unmarkAll(const Literal* lits, uint32 size) const {
	for (uint32 i = 0; i != size; ++i) {
		occurs_[lits[i].var()].unmark();
	}
}

// Run variable and/or blocked clause elimination on var v.
// If the number of non-trivial resolvents is <= maxCnt, 
// v is eliminated by clause distribution. If bce is enabled,
// clauses blocked on a literal of v are removed.
bool SatElite::bceVe(Var v, uint32 maxCnt) {
	Solver* s = ctx_->master();
	if (s->value(v) != value_free) return true;
	assert(!ctx_->frozen(v) && !ctx_->eliminated(v));
	resCands_.clear();
	// distribute clauses on v 
	// check if number of clauses decreases if we'd eliminate v
	uint32  bce    = options.bce;
	ClRange cls    = splitOcc(v, bce > 1);
	uint32  cnt    = 0;
	uint32  markMax= ((uint32)negT_.size() * (bce>1));
	uint32  blocked= 0;
	bool stop      = false;
	Clause* lhs, *rhs;
	for (VarVec::const_iterator i = posT_.begin(); i != posT_.end() && !stop; ++i) {
		lhs         = clauses_[*i];
		markAll(&(*lhs)[0], lhs->size());
		lhs->setMarked(bce != 0);
		for (VarVec::const_iterator j = negT_.begin(); j != negT_.end(); ++j) {
			if (!trivialResolvent(*(rhs = clauses_[*j]), v)) {
				markMax -= rhs->marked();
				rhs->setMarked(false); // not blocked on v
				lhs->setMarked(false); // not blocked on v
				if (++cnt <= maxCnt) {
					resCands_.push_back(lhs);
					resCands_.push_back(rhs);
				}
				else if (!markMax) {
					stop = (bce == 0);
					break;
				}
			}
		}
		unmarkAll(&(*lhs)[0], lhs->size());
		if (lhs->marked()) {
			posT_[blocked++] = *i;
		}
	}
	if (cnt <= maxCnt) {
		// eliminate v by clause distribution
		ctx_->eliminate(v);  // mark var as eliminated
		// remove old clauses, store them in the elimination table so that
		// (partial) models can be extended.
		for (ClIter it = cls.first; it != cls.second; ++it) {
			// reuse first cnt ids for resolvents
			if (clauses_[it->var()]) {
				bool freeId = (cnt && cnt--);
				bceVeRemove(it->var(), freeId, v, false);
			}
		}
		// add non trivial resolvents
		assert( resCands_.size() % 2 == 0 );
		ClIter it = cls.first;
		for (VarVec::size_type i = 0; i != resCands_.size(); i+=2, ++it) {
			if (!addResolvent(it->var(), *resCands_[i], *resCands_[i+1])) {
				return false;
			}
		}
		assert(occurs_[v].numOcc() == 0);
		// release memory
		occurs_[v].clear();
	}
	else if ( (blocked + markMax) > 0 ) {
		// remove blocked clauses
		for (uint32 i = 0; i != blocked; ++i) {
			bceVeRemove(posT_[i], false, v, true);
		}
		for (VarVec::const_iterator it = negT_.begin(); markMax; ++it) {
			if ( (rhs = clauses_[*it])->marked() ) {
				bceVeRemove(*it, false, v, true);
				--markMax;
			}
		}
	}
	return options.maxIters != UINT32_MAX || backwardSubsume();
}

bool SatElite::bce() {
	uint32 ops = 0;
	for (ClWList& bce= occurs_[0].refs; bce.right_size() != 0; ++ops) {
		Var v          = *(bce.right_end()-1);
		bce.pop_right();
		occurs_[v].bce=0; 
		if ((ops & 1023) == 0)   {
			if (timeout())         { bce.clear(); return true; }
			if ((ops & 8191) == 0) { reportProgress(Event(this, 'B', ops, 1+bce.size())); }
		}
		if (!cutoff(v) && !bceVe(v, 0)) { return false; }
	}
	return true;
}

bool SatElite::eliminateVars() {
	Var     v          = 0;
	uint32  occ        = 0;
	if (!bce()) return false;
	for (uint32 ops = 0; !elimHeap_.empty(); ++ops) {
		v   = elimHeap_.top();  elimHeap_.pop();
		occ = occurs_[v].numOcc();
		if ((ops & 1023) == 0)   {
			if (timeout())         { elimHeap_.clear(); return true; }
			if ((ops & 8191) == 0) { reportProgress(Event(this, 'E', ops, 1+elimHeap_.size())); }
		}
		if (!cutoff(v) && !bceVe(v, occ)) {
			return false;
		}
	}
	return options.maxIters != UINT32_MAX || bce();
}

// returns true if the result of resolving c1 (implicitly given) and c2 on v yields a tautologous clause
bool SatElite::trivialResolvent(const Clause& c2, Var v) const {
	for (uint32 i = 0, end = c2.size(); i != end; ++i) {
		Literal x = c2[i];
		if (occurs_[x.var()].marked(!x.sign()) && x.var() != v) {
			return true;
		}		
	}
	return false;
}

// Pre: lhs and rhs can be resolved on lhs[0].var()
// Pre: trivialResolvent(lhs, rhs, lhs[0].var()) == false
bool SatElite::addResolvent(uint32 id, const Clause& lhs, const Clause& rhs) {
	resolvent_.clear();
	Solver* s = ctx_->master();
	assert(lhs[0] == ~rhs[0]);
	uint32 i, end;
	Literal l;
	for (i = 1, end = lhs.size(); i != end; ++i) {
		l = lhs[i];
		if (!s->isFalse(l)) {
			if (s->isTrue(l)) goto unmark;
			occurs_[l.var()].mark(l.sign());
			resolvent_.push_back(l);
		}
	}
	for (i = 1, end = rhs.size(); i != end; ++i) {
		l = rhs[i];
		if (!s->isFalse(l) && !occurs_[l.var()].marked(l.sign())) {
			if (s->isTrue(l)) goto unmark;
			occurs_[l.var()].mark(l.sign());
			resolvent_.push_back(l);
		}
	}
	if (!subsumed(resolvent_))  {
		if (resolvent_.empty())   { return false; }
		if (resolvent_.size()==1) { 
			occurs_[resolvent_[0].var()].unmark();
			return s->force(resolvent_[0], 0) && s->propagate() && propagateFacts();
		}
		clauses_[id]  = Clause::newClause(resolvent_);
		attach(id, false);
		return true;
	}
unmark:
	if (!resolvent_.empty()) { 
		unmarkAll(&resolvent_[0], resolvent_.size()); 
	}
	return true;
}

// extends the model given in assign by the vars that were eliminated
void SatElite::extendModel(Assignment& assign, LitVec& unconstr) {
	if (!elimTop_) return;
	// compute values of eliminated vars / blocked literals by "unit propagating"
	// eliminated/blocked clauses in reverse order
	uint32 uv = 0;
	uint32 us = unconstr.size();
	uint32 uo = us;
	if (us) {
		// flip last unconstraint variable to get "next" model
		unconstr.back() = ~unconstr.back();
	}
	Clause* r    = elimTop_;
	do {
		Literal x  = (*r)[0];
		Var last   = x.var();
		bool check = true;
		if (!r->blocked()) {
			// solver set all eliminated vars to true, thus before we can compute the
			// implied values we first need to set them back to free
			assign.clearValue(last);
		}
		if (uv != us && unconstr[uv].var() == last) {
			// last is unconstraint w.r.t the current model -
			// set remembered value
			check    = false;
			assign.setValue(last, trueValue(unconstr[uv]));
			assign.setReason(last, 0);
			++uv;
		}
		do {
			Clause& c = *r;
			if (assign.value(x.var()) != trueValue(x) && check) {
				for (uint32 i = 1, end = c.size(); i != end; ++i) {
					if (assign.value(c[i].var()) != falseValue(c[i])) {
						x = c[i];
						break;
					}
				}
				if (x == c[0]) {
					// all lits != x are false
					// clause is unit or conflicting
					assert(c.blocked() || assign.value(x.var()) != falseValue(x));
					assign.clearValue(x.var());
					assign.setValue(x.var(), trueValue(x));
					assign.setReason(x.var(), Antecedent(x));
					check = false;
				}
			}
			r = r->next();
		} while (r && (x = (*r)[0]).var() == last);
		if (assign.value(last) == value_free) {
			// last seems unconstraint w.r.t the model. Assume last to true; remember it
			// so that we can also enumerate the model containing ~last.
			assign.setValue(last, value_true);
			assign.setReason(last, 0);
			unconstr.push_back(posLit(last));	
		}
	} while (r);
	// check whether newly added unconstraint vars are really unconstraint w.r.t the model
	// or if they are implied by some blocked clause.
	LitVec::iterator j = unconstr.begin()+uo;
	for (LitVec::iterator it = j, end = unconstr.end(); it != end; ++it) {
		if (!it->sign() && assign.reason(it->var()).isNull()) {
			*j++ = *it;
		}
	}
	unconstr.erase(j, unconstr.end());
	// pop all vars that were enumerated in both phases
	while (!unconstr.empty() && unconstr.back().sign()) {
		unconstr.pop_back();
	}
}
}}
