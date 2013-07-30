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
#include <clasp/cb_enumerator.h>
#include <clasp/solver.h>
#include <clasp/clause.h>
#include <clasp/util/mutex.h>
#include <stdio.h> // sprintf
#ifdef _MSC_VER
#pragma warning (disable : 4996) // sprintf may be unfase
#endif
namespace Clasp {
/////////////////////////////////////////////////////////////////////////////////////////
// CBConsequences::GlobalConstraint
/////////////////////////////////////////////////////////////////////////////////////////
class CBConsequences::GlobalConstraint {
public:
	explicit GlobalConstraint(bool shared) : current_(0), shared_(shared) {}
	~GlobalConstraint() {
		if (current_) current_->release();
	}
	ClauseHead* set(Solver& s, LitVec& c) {
		ClauseCreator::Result ret;
		uint32 flags = ClauseCreator::clause_explicit|ClauseCreator::clause_no_add;
		if (shared_) {
			ret = ClauseCreator::integrate(s, createShared(c), flags);
		}
		else {
			ret = ClauseCreator::create(s, c, flags, ClauseInfo(Constraint_t::learnt_other));
		}
		return ret.local;
	}
	SharedLiterals* getShared() { 
		std::lock_guard<tbb::spin_mutex> lock(mutex_);
		return current_ ? current_->share() : 0;
	}
	bool shared() const { return shared_; }
private:
	SharedLiterals* createShared(const LitVec& c) {
		SharedLiterals* newShared = SharedLiterals::newShareable(c, Constraint_t::learnt_other, 2);
		SharedLiterals* oldShared = current_;
		{ std::lock_guard<tbb::spin_mutex> lock(mutex_);
			current_ = newShared;
		}
		if (oldShared) oldShared->release();
		return newShared;
	}
	tbb::spin_mutex mutex_;
	SharedLiterals* current_;
	bool            shared_;
};
/////////////////////////////////////////////////////////////////////////////////////////
// CBConsequences::LocalConstraint
/////////////////////////////////////////////////////////////////////////////////////////
class CBConsequences::LocalConstraint : public EnumeratorConstraint {
public:
	typedef PodVector<Constraint*>::type  ConstraintDB;
	Constraint* cloneAttach(Solver& other);
	void destroy(Solver* s, bool detach);
	void add(Solver& s, Constraint* c);
	bool integrate(Solver& s, SharedLiterals* lits);
	ConstraintDB locked;
};
/////////////////////////////////////////////////////////////////////////////////////////
// CBConsequences
/////////////////////////////////////////////////////////////////////////////////////////
CBConsequences::CBConsequences(Consequences_t type) 
	: Enumerator(0)
	, current_(0)
	, type_(type) {
	current_ = 0;
}

CBConsequences::~CBConsequences() {
	delete current_;
}

Enumerator::EnumeratorConstraint* CBConsequences::doInit(SharedContext& ctx, uint32 t, bool start) {
	delete current_;
	current_ = 0;
	if (start) {
		if (ctx.symTab().type() == SymbolTable::map_direct) {
			// create indirect from direct mapping
			Var end = ctx.symTab().size();
			ctx.symTab().startInit();
			char buf[1024];
			for (Var v = 1; v < end; ++v) {
				sprintf(buf, "%u", v);
				ctx.symTab().addUnique(v, buf).lit = posLit(v);
			}
			ctx.symTab().endInit();
		}
		const SymbolTable& index = ctx.symTab();
		for (SymbolTable::const_iterator it = index.curBegin(); it != index.end(); ++it) {
			if (!it->second.name.empty()) { 
				ctx.setFrozen(it->second.lit.var(), true);
				if (type_ == cautious_consequences) {
					it->second.lit.watch();  
				}
			}
		} 
		return 0;
	}
	else {
		current_ = new GlobalConstraint(t > 1);
	}
	return new LocalConstraint();
}

bool CBConsequences::ignoreSymmetric() const { return true; }

bool CBConsequences::updateModel(Solver& s) {
	C_.clear();
	type_ == brave_consequences
			? updateBraveModel(s)
			: updateCautiousModel(s);
	for (LitVec::size_type i = 0; i != C_.size(); ++i) {
		s.clearSeen(C_[i].var());
	}
	return current_->shared();
}

void CBConsequences::add(Solver& s, Literal p) {
	assert(s.isTrue(p));
	if (!s.seen(p.var())) {
		C_.push_back(~p); // invert literal: store nogood as clause
		if (s.level(p.var()) > s.level(C_[0].var())) {
			std::swap(C_[0], C_.back());
		}
		s.markSeen(p);
	}
}

void CBConsequences::updateBraveModel(Solver& s) {
	const SymbolTable& index = s.sharedContext()->symTab();
	for (SymbolTable::const_iterator it = index.begin(); it != index.end(); ++it) {
		if (!it->second.name.empty()) {
			Literal& p = it->second.lit;
			if (s.isTrue(p))  { p.watch(); }
			if (!p.watched()) { add(s, ~p); }
		}
	}
}

void CBConsequences::updateCautiousModel(Solver& s) {
	const SymbolTable& index = s.sharedContext()->symTab();
	for (SymbolTable::const_iterator it = index.begin(); it != index.end(); ++it) {
		Literal& p = it->second.lit;
		if (p.watched()) { 
			if      (s.isFalse(p))  { p.clearWatch(); }
			else if (s.isTrue(p))   { add(s, p); }
		}
	}
}

// integrate current global constraint into s
bool CBConsequences::updateConstraint(Solver& s, bool) {
	assert(s.getEnumerationConstraint() && "CBConsequences solver not attached!");
	SharedLiterals* x = current_->getShared();
	return static_cast<LocalConstraint*>(s.getEnumerationConstraint())->integrate(s, x);		
}

Constraint* CBConsequences::addNewConstraint(Solver& s) {
	// create and set new global constraint
	ClauseHead* ret = current_->set(s, C_);
	if (ret) {
		// add new local constraint to the given solver
		LocalConstraint* con = static_cast<LocalConstraint*>(s.getEnumerationConstraint());
		assert(con && "CBConsequences solver not attached!");
		con->add(s, ret);
	}
	return ret;
}

bool CBConsequences::backtrack(Solver& s) {
	if (C_.empty()) {
		// no more consequences possible
		C_.push_back(negLit(0));
	}
	// C_ stores the violated nogood, ie. the new integrity constraint.
	// C_[0] is the literal assigned on the highest DL and hence the
	// decision level on which we must analyze the conflict.
	uint32 newDl = s.level(C_[0].var());
	if (getHighestActiveLevel() < newDl) {
		// C_ is not the most important nogood, ie. there is some other
		// nogood that is violated below C_. 
		newDl = getHighestActiveLevel() - 1;
	}
	s.undoUntil(newDl, true);
	addNewConstraint(s);
	return !s.hasConflict() || s.resolveConflict();
}
/////////////////////////////////////////////////////////////////////////////////////////
// class CBConsequences::LocalConstraint
/////////////////////////////////////////////////////////////////////////////////////////
Constraint* CBConsequences::LocalConstraint::cloneAttach(Solver& other) {
	LocalConstraint* ret = new LocalConstraint();
	ret->attach(other);
	return ret;
}
void CBConsequences::LocalConstraint::destroy(Solver* s, bool detach) {
	assert(!s || s->decisionLevel() == s->rootLevel());
	if (!locked.empty()) {
		static_cast<ClauseHead*>(locked.back())->destroy(s, true);
		locked.pop_back();
	}
	for (ConstraintDB::size_type i = 0; i != locked.size(); ++i) {
		static_cast<ClauseHead*>(locked[i])->destroy(s, false);
	}
	locked.clear();
	EnumeratorConstraint::destroy(s, detach);
}
void CBConsequences::LocalConstraint::add(Solver& s, Constraint* c) {
	if (!locked.empty()) {
		static_cast<ClauseHead*>(locked.back())->detach(s);
		ConstraintDB::size_type j = 0; 
		for (ConstraintDB::size_type i = 0; i != locked.size(); ++i) {
			ClauseHead* h = (ClauseHead*)locked[i];
			if (h->locked(s)) locked[j++] = h;
			else h->destroy(&s, false);
		}
		locked.erase(locked.begin()+j, locked.end());
	}
	locked.push_back(c);
}
bool CBConsequences::LocalConstraint::integrate(Solver& s, SharedLiterals* clause) {
	if (!clause) return true;
	ClauseCreator::Result ret(0, ClauseCreator::status_conflicting);
	if (clause->size() > 0) {
		ret = ClauseCreator::integrate(s, clause, ClauseCreator::clause_explicit|ClauseCreator::clause_no_add);
		if (ret.local) { add(s, ret.local); }
	}
	else {
		s.setStopConflict();
	}
	return ret.ok();
}

}
