//
// Copyright (c) 2006, 2007, 2012 Benjamin Kaufmann
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

#if defined(_MSC_VER)
#pragma warning (disable : 4146) // unary minus operator applied to unsigned type, result still unsigned
#pragma warning (disable : 4996) // 'std::_Fill_n' was declared deprecated
#endif

#include <clasp/program_builder.h>
#include <clasp/preprocessor.h>
#include <clasp/solver.h>
#include <clasp/clause.h>
#include <clasp/weight_constraint.h>
#include <clasp/minimize_constraint.h>
#include <clasp/dependency_graph.h>
#include <sstream>
#include <numeric>

#if defined(_MSC_VER)
#	if !defined(__FUNCTION__)
#	define MY_FUNCNAME __FILE__
#	else
# define MY_FUNCNAME __FUNCTION__
# endif
#elif defined(__GNUC__)
#define MY_FUNCNAME __PRETTY_FUNCTION__
#else
#define MY_FUNCNAME __FILE__
#endif

std::string precondition_error(const char* ex, const char* func, unsigned line) {
	std::stringstream err;
	err << func << "@" << line << ": precondition violated: " << ex;
	return err.str();
}

#define check_precondition(x, EX) \
	(void)( (!!(x)) || (throw EX(precondition_error((#x), MY_FUNCNAME, __LINE__)), 0))

namespace Clasp {
/////////////////////////////////////////////////////////////////////////////////////////
// class PrgAtomNode
//
// Objects of this class represent atoms in the atom-body-dependency graph.
/////////////////////////////////////////////////////////////////////////////////////////
// Creates the atom-oriented nogoods for this atom in form of clauses.
// Adds the support clause [~h B1...Bn] (where each Bi is a defining body of h)
// representing the tableau-rules BTA and FFA.
// Furthermore, adds the clause [h ~Bj] representing tableau-rules FTA and BFA
// if Bj defines h via a "normal" edge. 
bool PrgAtomNode::toConstraint(SharedContext& ctx, ClauseCreator& gc, ProgramBuilder& prg) {
	if (value() != value_free && !ctx.addUnary(trueLit())) {
		return false;
	}
	if (!hasVar()) { return true; }
	ClauseCreator bc(ctx.master());
	Literal a = literal();
	gc.start().add(~a);
	prg.vars_.mark( ~a );
	bool sat = false;
	bool nant= !negDep.empty();
	// consider only bodies which are part of the simplified program, i.e.
	// are associated with a variable in the solver.
	HeadVec::iterator j = preds.begin();
	for (HeadVec::iterator it = preds.begin(); it != preds.end(); ++it) {
		PrgBodyNode* bn = prg.bodies_[it->node()];
		Literal B = bn->literal();
		sat |= prg.vars_.marked( ~B );
		if (bn->hasVar()) {
			*j++ = *it;
			nant = nant || it->choice();
			if (!prg.vars_.marked(B)) {
				prg.vars_.mark( B );
				gc.add(B);
			}
			if (it->normal() && a != B) {
				bc.start().add(a);
				if ( (a != ~B && !bc.add(~B).end()) || (a == ~B && !bc.end()) ) {
					return false;
				}
			}
		}
	}
	preds.erase(j, preds.end());
	prg.vars_.unmark( var() );
	for (HeadVec::const_iterator it = preds.begin(); it != preds.end(); ++it) {
		prg.vars_.unmark( prg.bodies_[it->node()]->var() );
	}
	if (nant) { prg.ctx_->setNant(var(), true); }
	return sat || gc.end();
}
// Some bodies of this node have changed during preprocessing,
// e.g. they are false or were replaced with different but equivalent bodies.
// Update all back-links.
PrgAtomNode::SimpRes PrgAtomNode::simplifyBodies(Var atomId, ProgramBuilder& prg, bool strong) {
	HeadVec::iterator j = preds.begin();
	Var eq;
	uint32 diffLits = 0;
	for (HeadVec::iterator it = preds.begin(), end = preds.end(); it != end; ++it) {
		if ( (eq = prg.getEqBody(it->node())) != it->node() ) { *it = HeadEdge(eq, it->type()); }
		PrgBodyNode* x = prg.bodies_[it->node()];
		if (!x->ignore() && x->value() != value_false && (!strong || x->hasHead(atomId))) {
			if (!it->normal()) {
				for (HeadVec::iterator k = it; ++k != end; ) {
					if ( (eq = prg.getEqBody(k->node())) != k->node() ) { *k = HeadEdge(eq, k->type()); }
					if (k->node() == it->node() && k->type() < it->type()) {
						*it = *k;
					}
				}
			}
			*j++ = *it;
			if (strong && (!prg.vars_.marked(x->literal()) || !it->normal())) {
				++diffLits;
				if (it->normal()) { prg.vars_.mark(x->literal()); }
			}
			x->setIgnore(true);
		}
	}
	preds.erase(j, preds.end());
	for (HeadVec::iterator it = preds.begin(), end = preds.end(); it != end; ++it) {
		prg.bodies_[it->node()]->setIgnore(false);
		if (strong) { prg.vars_.unmark(prg.bodies_[it->node()]->var()); }
	}
	if (!strong) diffLits = (uint32)preds.size();
	if (preds.empty()) {
		setIgnore(true);
		return SimpRes(setValue(value_false), 0);
	}
	return SimpRes(true, diffLits);
}
/////////////////////////////////////////////////////////////////////////////////////////
// class PrgBodyNode
//
// Objects of this class represent bodies in the atom-body-dependency graph.
/////////////////////////////////////////////////////////////////////////////////////////
PrgBodyNode* PrgBodyNode::create(uint32 id, const PrgRule& rule, const PrgRule::RData& rInfo, ProgramBuilder& prg) {
	void* mem = ::operator new(sizeof(PrgBodyNode)+ (rule.body.size()*sizeof(Literal)));
	return new (mem)PrgBodyNode(id, rule, rInfo, prg);
}
PrgBodyNode::PrgBodyNode(uint32 id, const PrgRule& rule, const PrgRule::RData& rInfo, ProgramBuilder& prg) {
	size_     = (uint32)rule.body.size();
	posSize_  = rInfo.posSize;
	type_     = NORMAL_BODY;
	if      (rule.type() == CONSTRAINTRULE) { type_ = COUNT_BODY; }
	else if (rule.type() == WEIGHTRULE)     { type_ = SUM_BODY; }
	bool w = false;
	if (extended()) { 
		extra_.ext = Extended::createExt(this, rule.bound(), w = (rule.type() == WEIGHTRULE));
	}
	uint32 spw= 0;  // sum of positive weights
	uint32 snw= 0;  // sum of negative weights
	uint32 p = 0, n = 0;
	// store B+ followed by B-
	for (LitVec::size_type i = 0, end = rule.body.size(); i != end; ++i) {
		Literal      x = rule.body[i].first;	
		PrgAtomNode* a = prg.resize(x.var());
		prg.ruleState_.popFromRule(x.var()); // clear flags that were set in PrgRule during rule simplification
		if (!x.sign()) {  // B+ atom
			goals_[p] = x;
			a->posDep.push_back(id);
			if (w) { 
				check_precondition(rule.body[i].second>0, std::logic_error);
				spw += (extra_.ext->weights[p] = rule.body[i].second);
			}
			++p;
		}
		else {                  // B- atom
			goals_[posSize_+n]  = x;
			if (prg.eqOpts_.iters != 0) { a->negDep.push_back(id); }
			if (w) { 
				check_precondition(rule.body[i].second>0, std::logic_error);
				snw += (extra_.ext->weights[posSize_+n] = rule.body[i].second);
			}
			++n;
		}
	}
	if (extended()) { 
		if (!w)       { spw = posSize(); snw = negSize(); }
		extra_.ext->sumWeights  = spw + snw; 
		extra_.ext->unsupp      = static_cast<weight_t>(this->bound() - snw);
	}
	else {
		extra_.unsupp = posSize();
	}
}
PrgBodyNode::~PrgBodyNode() {
	if (extended()) { extra_.ext->destroy(); }
}

void PrgBodyNode::destroy() {
	this->~PrgBodyNode();
	::operator delete(this);
}

PrgBodyNode::Extended* PrgBodyNode::Extended::createExt(PrgBodyNode* self, uint32 b, bool w) {
	void* m = ::operator new(sizeof(Extended) + ((w?self->size():0)*sizeof(weight_t)));
	Extended* r = new (m) Extended;
	r->bound = b;
	return r;
}

void PrgBodyNode::Extended::destroy() {
	::operator delete(this);
}

uint32 PrgBodyNode::reinitDeps(uint32 id, ProgramBuilder& prg) {
	uint32 hash = 0;
	for (uint32 i = 0; i != posSize(); ++i) {
		hash += hashId(pos(i));
		prg.resize(pos(i))->posDep.push_back(id);
	}
	for (uint32 i = 0; i != negSize(); ++i) {
		hash += hashId(-neg(i));
		prg.resize(neg(i))->negDep.push_back(id);
	}
	for (uint32 i = 0; i != heads_.size(); ++i) {
		prg.resize(heads_[i].node())->preds.push_back(HeadEdge(id, heads_[i].type()));
	}
	return hash;
}

void PrgBodyNode::sortBody() {
	if (!hasWeights()) {
		std::sort(goals_, goals_+posSize_);
		std::sort(goals_+posSize_, goals_+size());
	}
	else {
		WeightLitVec temp;
		temp.reserve(size());
		for (uint32 i = 0; i != size(); ++i) {
			temp.push_back(WeightLiteral(goal(i), weight(i)));
		}
		std::sort(temp.begin(), temp.begin()+posSize());
		std::sort(temp.begin()+posSize(), temp.end());
		for (uint32 i = 0; i != size(); ++i) {
			goals_[i]              = temp[i].first;
			extra_.ext->weights[i] = temp[i].second;
		}
	}
}


// Lower bound of this body, i.e. number of literals that must be true
// in order to make the body true.
weight_t PrgBodyNode::bound() const {
	return !extended()
		? (weight_t)size()
		: (weight_t)std::max(extra_.ext->bound, weight_t(0));
}

// Sum of weights of the literals in the body.
// Note: if type != WEIGHTRULE, the size of the body is returned
weight_t PrgBodyNode::sumWeights() const {
	return !extended()
		? (weight_t)size()
		: std::max(extra_.ext->sumWeights, weight_t(0));
}

// Returns the weight of the idx'th subgoal in B+/B-
// Note: if type != WEIGHTRULE, the returned weight is always 1
weight_t PrgBodyNode::weight(uint32 idx, bool pos) const {
	return !hasWeights()
		? 1
		: extra_.ext->weights[ (!pos * posSize()) + idx ];
}

// The atom v, which must be a positive subgoal of this body, is supported,
// check if this body is now also supported.
bool PrgBodyNode::onPosPredSupported(Var v) {
	if (!extended()) {
		return --extra_.unsupp <= 0;
	}
	else if (!hasWeights()) {
		return --extra_.ext->unsupp <= 0;
	}
	else {
		return (extra_.ext->unsupp -= extra_.ext->weights[std::distance(goals_, std::find(goals_, goals_+posSize_, posLit(v)))])
			<= 0;
	}
}

// Creates the body-oriented nogoods for this body
bool PrgBodyNode::toConstraint(SharedContext& ctx, ClauseCreator& c, const ProgramBuilder& prg) {
	if (value() != value_free && !ctx.addUnary(trueLit())){ return false; }
	if (!hasVar() || ignore())                            { return true; } // body is not relevant
	const AtomList& atoms = prg.atoms_;
	if (!extended()) { return addPredecessorClauses(ctx, c, atoms); }
	WeightLitVec lits;
	for (uint32 i = 0, end = size_; i != end; ++i) {
		assert(goals_[i].var() != 0);
		Literal eq = goals_[i].sign() ? ~atoms[goals_[i].var()]->literal() : atoms[goals_[i].var()]->literal();
		lits.push_back( WeightLiteral(eq, weight(i)) );
	}
	return WeightConstraint::newWeightConstraint(ctx, literal(), lits, bound());
}

// Adds clauses for the tableau-rules FFB and BTB as well as FTB, BFB.
// FFB and BTB:
// - a binary clause [~b s] for every positive subgoal of b
// - a binary clause [~b ~n] for every negative subgoal of b
// FTB and BFB:
// - a clause [b ~s1...~sn n1..nn] where si is a positive and ni a negative subgoal
bool PrgBodyNode::addPredecessorClauses(SharedContext& ctx, ClauseCreator& gc, const AtomList& prgAtoms) {
	const Literal negBody = ~literal();
	ClauseCreator bc(ctx.master());
	gc.start().add(literal());
	bool sat = false;
	for (Literal* it = goals_, *end = goals_+size_; it != end; ++it) {
		assert(it->var() != 0);
		Literal aEq = it->sign() ? ~prgAtoms[it->var()]->literal() : prgAtoms[it->var()]->literal();
		if (negBody != ~aEq) {
			bc.start().add(negBody);
			if ( (negBody != aEq && !bc.add(aEq).end()) || (negBody == aEq && !bc.end()) ) {
				return false;
			}
		} // else: SAT-clause - ~b b
		sat |= aEq == literal();
		if (~aEq != literal()) {
			gc.add( ~aEq );
		}
	}
	return sat || gc.end();
}

// Remove/merge duplicate literals.
// If body contains p and ~p and both are needed, set body to false.
// Remove false/true literals
bool PrgBodyNode::simplifyBody(ProgramBuilder& prg, uint32 bodyId, std::pair<uint32, uint32>& hashes, Preprocessor& pre, bool strong) {
	hashes.first = hashes.second = 0;
	bool ok  = sumWeights() >= bound();
	uint32 j = 0;
	Var eq, comp;
	for (uint32 i = 0, end = size_; i != end && ok; ++i) {
		Var      a = goals_[i].var();
		hashes.first += hashId(goals_[i].sign()?-a:a);
		if ((eq = prg.getEqAtom(a)) != a) {
			if (goals_[i].sign() || (comp = pre.replaceComp(a)) == varMax) {
				a         = eq;
				goals_[i] = Literal(a, goals_[i].sign());	
			}
			else {
				a         = comp;
				goals_[i] = Literal(comp, true);
				prg.atoms_[comp]->negDep.push_back(bodyId);
				VarVec& eqPos = prg.atoms_[eq]->posDep;
				VarVec::iterator it = std::find(eqPos.begin(), eqPos.end(), bodyId);
				if (it != eqPos.end()) eqPos.erase(it);
			}
		}
		Literal p; 
		ValueRep v = prg.atoms_[a]->value();
		bool mark  = false, rem = true;
		if (prg.atoms_[a]->hasVar() || strong) {
			p = goals_[i].sign() ? ~prg.atoms_[a]->literal() : prg.atoms_[a]->literal();
			v = prg.atoms_[a]->hasVar() ? v : value_false;
			mark = true;
		}
		if (v == value_weak_true && goals_[i].sign()) {
			v = value_true;
		}
		if (v != value_free && v != value_weak_true) {  // truth value is known
			mark = false;                                 // subgoal will be removed
			if (v == falseValue(goals_[i])) {
				// drop rule if normal/choice
				// or if we can no longer reach the lower bound of card/weight rule
				ok = extended() && (extra_.ext->sumWeights -= weight(i)) >= bound();
				pre.setSimplifyHeads(bodyId);
			}
			else if (extended()) {
				// subgoal is true: decrease necessary lower bound
				weight_t w = weight(i);
				extra_.ext->sumWeights -= w;
				extra_.ext->bound      -= w;
			}
		}
		else if (!mark || !prg.vars_.marked(p)) {
			if (mark) prg.vars_.mark(p);
			goals_[j] = goals_[i];
			rem       = false;
			if (hasWeights()) {
				extra_.ext->weights[j] = extra_.ext->weights[i];
			}
			++j;
		}
		else if (extended()) { // body contains p more than once
			// ignore lit if normal/choice, merge if card/weight
			uint32 x = findLit(p, prg.atoms_);
			assert(x != static_cast<uint32>(-1) && "WeightBody - Literal is missing!");
			if (!hasWeights()) {
				Extended* newExt   = Extended::createExt(this, 0, true);
				std::fill_n(newExt->weights, (LitVec::size_type)size(), 1);
				newExt->sumWeights = extra_.ext->sumWeights;
				newExt->bound      = extra_.ext->bound;
				newExt->unsupp     = extra_.ext->unsupp;
				extra_.ext->destroy();
				extra_.ext = newExt;
				type_ = SUM_BODY;
			}
			extra_.ext->weights[x] += extra_.ext->weights[i];
		}
		if (mark && prg.vars_.marked(~p)) {     // body contains p and ~p
			ok = extended() && (extra_.ext->sumWeights - std::min(weight(i), findWeight(~p, prg.atoms_))) >= extra_.ext->bound;
		}
		if (rem) {
			VarVec& deps = goals_[i].sign() ? prg.atoms_[a]->negDep : prg.atoms_[a]->posDep;
			VarVec::iterator it = std::find(deps.begin(), deps.end(), bodyId);
			if (it != deps.end()) {
				*it = deps.back();
				deps.pop_back();
			}
		}
	}
	// unmark atoms, compute new hash value,
	// and restore pos | neg partition in case
	// we changed some positive goals to negative ones 
	size_    = j;
	uint32 p = 0, n = j;
	for (uint32 a, h; p < n;) {
		if      (!goals_[p].sign())      { h = hashId( (a = goals_[p++].var()));  }
		else if (goals_[n-1].sign())     { h = hashId(-(a = goals_[--n].var()));  }
		else /* restore pos|neg order */ {
			std::swap(goals_[p], goals_[n-1]);
			if (hasWeights()) {
				std::swap(extra_.ext->weights[p], extra_.ext->weights[n-1]);
			}
			continue;
		}
		prg.vars_.unmark( prg.atoms_[a]->var() );
		hashes.second += h;
	}
	posSize_ = p;
	assert(sumWeights() >= bound() || !ok);
	if (!ok) {          // body is false...
		setIgnore(true);  // ...and therefore can be ignored
		for (VarVec::size_type i = 0; i != heads_.size(); ++i) {
			pre.setSimplifyBodies(heads_[i].node());
		}
		heads_.clear();
		return setValue(value_false);
	}
	else if (bound() == 0) { // body is satisfied
		size_ = posSize_ = hashes.second = 0;
		if (extended()) {
			extra_.ext->destroy();
			extra_.unsupp = 0;
			type_ = NORMAL_BODY;
		}
		return setValue(value_true);
	}
	else if (extended() && (bound() == sumWeights() || size_ == 1)) { // body is normal
		weight_t unsupp = extra_.ext->unsupp;
		extra_.ext->destroy();
		extra_.unsupp = unsupp;
		type_ = NORMAL_BODY;
	}
	return true;
}

// (Re)establishes set property.
// Removes duplicate edges and removes superseded edges
void PrgBodyNode::buildHeadSet() {
	if (heads_.size() > 1) {
		std::sort(heads_.begin(), heads_.end());
		HeadVec::iterator j = heads_.begin();
		Var atomId = 0;
		for (HeadVec::iterator it = heads_.begin(), end = heads_.end(); it != end; ++it) {
			assert(it->normal() || it->choice());
			if (it->node() != atomId) {
				atomId = it->node();
				*j++   = *it;
			}
		}
		heads_.erase(j, heads_.end());
	}
}

void PrgBodyNode::removeHead(Var atomId) {
	HeadVec::iterator it, end;
	for (it = heads_.begin(), end = heads_.end(); it != end; ++it) {
		if (it->node() == atomId) {
			HeadVec::iterator j;
			for (j = it++; it != end; ++it) { if (it->node() != atomId) { *j++ = *it; } }
			heads_.erase(j, heads_.end());
			break;
		}
	}
}

uint32 PrgBodyNode::replace(PrgBodyNode& other, uint32 otherId, Preprocessor& pre) {
	uint32 ret = 0;
	for (HeadVec::iterator it = heads_.begin(), end = heads_.end(); it != end; ++it) {
		ret += it->normal();
		other.heads_.push_back(*it);
		// mark atom so that the link between atom and *this can be replaced
		// with a link to other.
		pre.setSimplifyBodies(it->node());
	}
	// mark for head-simplification - will remove possible duplicates
	pre.setSimplifyHeads(otherId); 
	// from now on, ignore *this
	heads_.clear();
	clearLiteral(true);
	setEq(otherId); // also sets ignore
	return ret;
}

// remove duplicate, equivalent and superfluous atoms from the head
bool PrgBodyNode::simplifyHeads(ProgramBuilder& prg, Preprocessor& pre, bool strong) {
	// 1. mark the body literals so that we can easily detect superfluous atoms
	// and selfblocking situations.
	RuleState& rs = prg.ruleState_;
	for (uint32 i = 0; i != size_; ++i) { rs.addToBody( goals_[i] ); }
	
	// 2. Now check for duplicate/superfluous heads and selfblocking situations
	std::sort(heads_.begin(), heads_.end()); // head-set changed, reestablish ordering
	bool ok = true;
	Weights w(*this);
	HeadVec::iterator j = heads_.begin();
	Var lastId = 0;
	for (HeadVec::iterator it = heads_.begin(), end = heads_.end(); it != end; ++it) {
		assert(it->normal() || it->choice());
		Var id = it->node();
		if (id != lastId && (!strong || prg.atoms_[id]->hasVar()) && !rs.inHead(id)) {
			lastId = id;
			// Note: equivalent atoms don't have vars.
			if (!rs.superfluousHead(it->choice(), sumWeights(), bound(), id, w)) {
				*j++ = *it;
				rs.addToHead(id);
				if (ok && rs.selfblocker(it->choice(), sumWeights(), bound(), id, w)) {
					ok = false;
				}
			}
			else { pre.setSimplifyBodies(id); }
		}
		else {
			// ignore any duplicates/irrelevant edges
			pre.setSimplifyBodies(id);
		}
	}
	heads_.erase(j, heads_.end());
	for (uint32 i = 0; i != size_; ++i) { rs.popFromRule(goals_[i].var()); }
	if (!ok) {
		for (VarVec::size_type i = 0; i != heads_.size(); ++i) {
			pre.setSimplifyBodies(heads_[i].node());
			rs.popFromRule(heads_[i].node());
		}
		heads_.clear();
		return setValue(value_false);
	}
	for (HeadVec::iterator it = heads_.begin(), end = heads_.end();  it != end; ++it) {
		rs.popFromRule(it->node());
	}
	return true;
}

uint32 PrgBodyNode::findLit(Literal p, const AtomList& prgAtoms) const {
	for (uint32 i = 0; i != size(); ++i) {
		Literal x = prgAtoms[goals_[i].var()]->literal();
		if (goals_[i].sign()) x = ~x;
		if (x == p) return i;
	}
	return static_cast<uint32>(-1);
}

weight_t PrgBodyNode::findWeight(Literal p, const AtomList& prgAtoms) const {
	if (!hasWeights()) return 1;
	uint32 i = findLit(p, prgAtoms);
	assert(i != static_cast<uint32>(-1) && "WeightBody - Literal is missing!");
	return extra_.ext->weights[i];
}

bool PrgBodyNode::backpropagate(ProgramBuilder& prg, LitVec& comp) {
	assert(value() != value_free);
	if (ignore()) return true;
	if (!extended() && size() > 0) {
		if (size() == 1 || value() != value_false) {
			ValueRep v = value();
			for (uint32 i = 0; i != size(); ++i) {
				if (i == posSize()) v = value()==value_false?value_weak_true:value_false;
				Var         id = goals_[i].var();
				PrgAtomNode* a = prg.atoms_[id];
				if (a->value() != v) {
					if (!a->setValue(v)) return false;
					comp.push_back(Literal(id, v==value_false));
				}
			}
		}
	}
	else if (type_ == COUNT_BODY && value() == value_false && extra_.ext->bound == 1) {
		ValueRep v = value_false;
		for (uint32 i = 0; i != size(); ++i) {
			if (i == posSize()) v = value_weak_true;
			Var         id = goals_[i].var();
			PrgAtomNode* a = prg.atoms_[id];
			if (a->value() != v) {
				if (!a->setValue(v)) return false;
				comp.push_back(Literal(id, v==value_false));
			}
		}
	}
	else if (type_ == SUM_BODY) {
		weight_t bound = value()==value_false?extra_.ext->bound:extra_.ext->sumWeights;
		ValueRep v     = value();
		for (uint32 i = 0; i != size(); ++i) {
			if (i == posSize()) v = value()==value_false?value_weak_true:value_false;
			if ((bound - weight(i)) <= 0) {
				Var         id = goals_[i].var();
				PrgAtomNode* a = prg.atoms_[id];
				if (a->value() != v) {
					if (!a->setValue(v)) return false;
					comp.push_back(Literal(id, v==value_false));
				}
			}
		}
	}
	return true;
}

bool PrgBodyNode::propagateFalse(uint32 myId, AtomList& prgAtoms) {
	if (!setValue(value_false)) return false;
	// since the body is false, it cannot define its heads
	for (HeadVec::iterator it = heads_.begin(), end = heads_.end(); it != end; ++it) {
		PrgAtomNode* a = prgAtoms[it->node()];
		for (HeadVec::iterator aIt = a->preds.begin(); aIt != a->preds.end(); ++aIt) {
			if (aIt->node() == myId) {
				HeadVec::iterator j;
				for (j = aIt++; aIt != a->preds.end(); ++aIt) {
					if (aIt->node() != myId) {
						*j++ = *aIt;
					}
				}
				a->preds.erase(j, a->preds.end());
				break;
			}
		}
	}
	heads_.clear();
	return true;
}
/////////////////////////////////////////////////////////////////////////////////////////
// SCC/cycle checking
/////////////////////////////////////////////////////////////////////////////////////////
class ProgramBuilder::CycleChecker {
public:
	CycleChecker(const AtomList& prgAtoms, const BodyList& prgBodies, bool check, uint32 startScc)
		: atoms_(prgAtoms)
		, bodies_(prgBodies)
		, count_(0)
		, sccs_(startScc)
		, check_(check) {
	}
	void visit(PrgBodyNode* b) { if (check_) visitDfs(b, true); }
	void visit(PrgAtomNode* a) { if (check_) visitDfs(a, false); }
	uint32 sccs()              const { return sccs_; }
	const AtomList& sccAtoms() const { return sccAtoms_; }
private:
	CycleChecker& operator=(const CycleChecker&);
	void visitDfs(PrgNode* n, bool body);
	typedef VarVec::iterator VarVecIter;
	struct Call {
		Call(PrgNode* n, bool body, Var next, uint32 min = 0)
			: node_( n )
			, min_(min)
			, next_(next)
			, body_(body) {}
		PrgNode*  node()  const { return node_; }
		bool      body()  const { return body_ != 0; }
		uint32    min()   const { return min_; }
		uint32    next()  const { return next_; }
		void      setMin(uint32 m)  { min_ = m; }
	private:
		PrgNode*  node_;    // node that is visited
		uint32    min_;     // min "discovering time"
		uint32    next_:31; // next successor
		uint32    body_:1;  // is node_ a body?
	};
	PrgNode* packNode(const Call& c) const { return reinterpret_cast<PrgNode*>(uintp(c.node())+uintp(!c.body())); }
	PrgNode* extractNode(PrgNode* n) const { return reinterpret_cast<PrgNode*>(uintp(n)&~uintp(1)); }
	bool     isAtomNode(PrgNode* n)  const { return (uintp(n) & 1) != 0; }
	typedef PodVector<Call>::type CallStack;
	NodeList        nodeStack_; // Nodes in the order they are visited
	CallStack       callStack_; // explict "runtime" stack - avoid recursion and stack overflows (mainly a problem on Windows)
	AtomList        sccAtoms_;  // non-trivially connected atoms
	const AtomList& atoms_;     // atoms of the program
	const BodyList& bodies_;    // bodies of the program
	uint32          count_;     // dfs counter
	uint32          sccs_;      // current scc number
	bool            check_;     // scc-check enabled?
};
// Tarjan's scc algorithm
// Uses callStack instead of native runtime stack in order to avoid stack overflows on
// large sccs.
void ProgramBuilder::CycleChecker::visitDfs(PrgNode* node, bool body) {
	if (!node->hasVar() || node->ignore() || node->visited()) return;
	callStack_.push_back( Call(node, body, 0) );
	const uint32 maxVertex  = (uint32(1)<<30)-1;
START:
	while (!callStack_.empty()) {
		Call c = callStack_.back();
		callStack_.pop_back();
		node = c.node();
		body = c.body();
		if (!node->visited()) {
			nodeStack_.push_back(packNode(c));
			c.setMin(count_++);
			node->setDfsIdx(c.min());
			node->setVisited(true);
		}
		// visit successors
		if (body) {
			PrgBodyNode* b = static_cast<PrgBodyNode*>(node);
			for (HeadVec::const_iterator it = b->heads_begin() + c.next(), end = b->heads_end(); it != end; ++it) {
				PrgAtomNode* a = atoms_[it->node()];
				if (a->hasVar() && !a->ignore()) {
					if (!a->visited()) {
						callStack_.push_back(Call(b, true, static_cast<uint32>(it-b->heads_begin()), c.min()));
						callStack_.push_back(Call(a, false, 0));
						goto START;
					}
					if (a->dfsIdx() < c.min()) {
						c.setMin(a->dfsIdx());
					}
				}
			}
		}
		else if (!body) {
			PrgAtomNode* a = static_cast<PrgAtomNode*>(node);
			VarVec::size_type end = a->posDep.size();
			assert(c.next() <= end);
			for (VarVec::size_type it = c.next(); it != end; ++it) {
				PrgBodyNode* bn = bodies_[a->posDep[it]];
				if (bn->hasVar() && !bn->ignore()) {
					if (!bn->visited()) {
						callStack_.push_back(Call(a, false, (uint32)it, c.min()));
						callStack_.push_back(Call(bn, true, 0));
						goto START;
					}
					if (bn->dfsIdx() < c.min()) {
						c.setMin(bn->dfsIdx());
					}
				}
			}
		}
		if (c.min() < node->dfsIdx()) {
			node->setDfsIdx( c.min() );
		}
		else if (node == extractNode(nodeStack_.back())) {
			// node is trivially-connected; all such nodes are in the same Pseudo-SCC
			node->setScc(PrgNode::noScc);
			node->setDfsIdx(maxVertex);
			nodeStack_.pop_back();
		}
		else { // non-trivial SCC
			PrgNode* succVertex;
			do {
				succVertex = nodeStack_.back();
				nodeStack_.pop_back();
				if (isAtomNode(succVertex)) {
					succVertex = extractNode(succVertex);
					sccAtoms_.push_back((PrgAtomNode*)succVertex);
				}
				succVertex->setScc(sccs_);
				succVertex->setDfsIdx(maxVertex);
			} while (succVertex != node);
			++sccs_;
		}
	}
}
/////////////////////////////////////////////////////////////////////////////////////////
// class VarList
/////////////////////////////////////////////////////////////////////////////////////////
void VarList::addTo(SharedContext& ctx, Var startVar) {
	ctx.reserveVars((uint32)vars_.size());
	for (Var i = startVar; i != (Var)vars_.size(); ++i) {
		ctx.addVar( type(i), hasFlag(i, eq_f) );
	}
}
/////////////////////////////////////////////////////////////////////////////////////////
// class PreproStats
/////////////////////////////////////////////////////////////////////////////////////////
void PreproStats::accu(const PreproStats& o) {
	bodies    += o.bodies;
	atoms     += o.atoms;
	ufsNodes  += o.ufsNodes;
	for (int i = 0; i != sizeof(rules)/sizeof(rules[0]); ++i) {
		rules[i] += o.rules[i];
	}
	for (int i = 0; i != sizeof(eqs)/sizeof(eqs[0]); ++i) {
		eqs[i] += o.eqs[i];
	}
	if (sccs == PrgNode::noScc || o.sccs == PrgNode::noScc) {
		sccs = o.sccs;
	}
	else {
		sccs += o.sccs;
	}
	if (o.trStats) {
		if (!trStats) trStats = new TrStats();
		trStats->auxAtoms += o.trStats->auxAtoms;
		for (int i = 0; i != sizeof(trStats->rules)/sizeof(trStats->rules[0]); ++i) {
			trStats->rules[i] += o.trStats->rules[i];
		}
	}
}
/////////////////////////////////////////////////////////////////////////////////////////
// class ProgramBuilder
/////////////////////////////////////////////////////////////////////////////////////////
ProgramBuilder::ProgramBuilder() 
	: minimize_(0), incData_(0), graph_(0), frozen_(true) { 
}
ProgramBuilder::~ProgramBuilder() { disposeProgram(true); }
ProgramBuilder::Incremental::Incremental() : startAtom_(1), startVar_(1), startAux_(1), startScc_(0) {}

void ProgramBuilder::disposeProgram(bool force) {
	// remove rules
	std::for_each( bodies_.begin(), bodies_.end(), DestroyObject() );
	BodyList().swap(bodies_);
	bodyIndex_.clear();
	MinimizeRule* r = minimize_;
	while (r) {
		MinimizeRule* t = r;
		r = r->next_;
		delete t;
	}
	minimize_ = 0;
	for (RuleList::size_type i = 0; i != extended_.size(); ++i) {
		delete extended_[i];
	}
	extended_.clear();
	VarVec().swap(initialSupp_);
	rule_.clear();
	if (force) {
		std::for_each( atoms_.begin(), atoms_.end(), DeleteObject() );
		AtomList().swap(atoms_);
		graph_ = 0;
		delete incData_;
		LitVec().swap(compute_);
		vars_.clear();
		ruleState_.clear();
	}
	else {
		// clean up atoms
		// remove rule associations
		uint32 startAux = incData_ ? incData_->startAux_ : (uint32)atoms_.size();
		assert(startAux <= atoms_.size());
		for (VarVec::size_type i = 0; i != startAux; ++i) {
			if (atoms_[i]->eq() && getEqAtom(i) >= startAux) {
				// atom i is equivalent to some aux atom 
				// make i the new root
				PrgAtomNode* eq = atoms_[getEqAtom(i)];
				assert(!eq->eq());
				eq->posDep.clear(); eq->negDep.clear(); eq->preds.clear();
				atoms_[i]->assign(eq);
				eq->setEq(i);
			}
			VarVec().swap(atoms_[i]->posDep);
			VarVec().swap(atoms_[i]->negDep);
			HeadVec().swap(atoms_[i]->preds);
		}
		// delete any introduced aux atoms
		// this is safe because aux atoms are never part of the input program
		// it is necessary in order to free their ids, i.e. the id of an aux atom
		// from step I might be needed for a program atom in step I+1
		for (VarVec::size_type i = startAux; i != atoms_.size(); ++i) {
			delete atoms_[i];
		}
		atoms_.erase(atoms_.begin()+startAux, atoms_.end());
	}
	stats.reset();
}

ProgramBuilder& ProgramBuilder::startProgram(SharedContext& ctx, const EqOptions& eqOpts) {
	disposeProgram(true);
	// atom 0 is always false
	atoms_.push_back( new PrgAtomNode() );
	incData_  = 0;
	eqOpts_   = eqOpts;
	ctx_      = &ctx;
	ctx_->symTab().clear();
	ctx_->symTab().startInit();
	graph_    = 0;
	frozen_   = false;
	if (eqOpts_.noSCC) {
		stats.sccs = PrgNode::noScc;
	}
	return *this;
}

ProgramBuilder& ProgramBuilder::updateProgram() {
	check_precondition(frozen_ || !incData_, std::logic_error);
	check_precondition(!atoms_.empty() && "startProgram() not called!", std::logic_error);
	// delete bodies and clean up atoms
	disposeProgram(false);
	if (!incData_)  { incData_ = new Incremental(); }
	uint32 maxVar = ctx_->master() ? ctx_->master()->numVars() : 1;
	while ((uint32)vars_.size() <= maxVar) {
		vars_.add(Var_t::atom_var);
	}
	incData_->startVar_ = (uint32)vars_.size();
	incData_->startAtom_= (uint32)atoms_.size();
	incData_->startAux_ = (uint32)atoms_.size();
	incData_->unfreeze_.clear();
	for (VarVec::iterator it = incData_->freeze_.begin(), end = incData_->freeze_.end(); it != end; ++it) {
		atoms_[*it]->setIgnore(false);
	}
	frozen_   = false;
	ctx_->symTab().startInit();
	return *this;
}

bool ProgramBuilder::endProgram() {
	return frozen_ || (prepareProgram() && transformProgram(!eqOpts_.noSCC));
}

bool ProgramBuilder::cloneProgram(SharedContext& ctx) {
	assert(frozen_);
	if (&ctx == ctx_) {
		return true;
	}
	if (&ctx.symTab() != &ctx_->symTab()) {
		ctx_->symTab().copyTo(ctx.symTab());
	}
	vars_.addTo(ctx, incData_ ? incData_->startVar_ : 1);
	SharedContext* t = ctx_;
	ctx_   = &ctx;
	bool r = transformProgram(false);
	ctx_   = t;
	return r;
}

void ProgramBuilder::addMinimize(MinimizeBuilder& m) {
	check_precondition(frozen_, std::length_error);
	if (minimize_) {
		WeightLitVec lits;
		for (MinimizeRule* r = minimize_; r; r = r->next_) {
			for (WeightLitVec::iterator it = r->lits_.begin(); it != r->lits_.end(); ++it) {
				PrgAtomNode* h    = atoms_[it->first.var()];
				lits.push_back(WeightLiteral(it->first.sign() ? ~h->literal() : h->literal(), it->second));
			}
			m.addRule(lits);
			lits.clear();
		}
	}
}

void ProgramBuilder::writeProgram(std::ostream& os) {
	const char* const delimiter = "0";
	// first write all minimize rules - revert order!
	PodVector<MinimizeRule*>::type mr;
	for (MinimizeRule* r = minimize_; r; r = r->next_) {
		mr.push_back(r);
	}
	for (PodVector<MinimizeRule*>::type::reverse_iterator rit = mr.rbegin(); rit != mr.rend(); ++rit) {
		os << OPTIMIZERULE << " " << 0 << " ";
		std::stringstream pBody, nBody, weights;
		VarVec::size_type nbs = 0, pbs =0;
		MinimizeRule* r = *rit;
		for (WeightLitVec::iterator it = r->lits_.begin(); it != r->lits_.end(); ++it) {
			if (atoms_[it->first.var()]->hasVar()) {
				it->first.sign() ? ++nbs : ++pbs;
				std::stringstream& body = it->first.sign() ? nBody : pBody;
				body << it->first.var() << " ";
				weights << it->second << " ";
			}
		}
		os << pbs+nbs << " " << nbs << " "
			 << nBody.str() << pBody.str() << weights.str() << "\n";
	}
	uint32 falseAtom = 0;
	for (uint32 i = 1; i < atoms_.size(); ++i) {
		if (atoms_[i]->value() == value_false) {
			falseAtom = i;
			break;
		}
	}
	// write all bodies together with their heads
	for (BodyList::iterator it = bodies_.begin(); it != bodies_.end(); ++it) {
		PrgBodyNode* b = *it;
		if (b->hasVar()) {
			if (b->value() == value_false && !b->hasHeads() && !b->ignore()) {
				// This body is an integrity constraint.
				// Handle by writing falseAtom :- Body.
				// where falseAtom is set to false in the compute statement
				if (falseAtom == 0) {
					atoms_.push_back( new PrgAtomNode() );
					falseAtom = (Var) atoms_.size() - 1;
					setCompute(falseAtom, false);
				}
				b->addHead(falseAtom, BASICRULE);
			}
			writeRule(b, falseAtom, os);
		}
	}
	// write eq-atoms, symbol-table and compute statement
	std::stringstream bp, bm, symTab;
	Literal comp;
	SymbolTable::const_iterator sym = ctx_->symTab().begin();
	for (AtomList::size_type i = 1; i < atoms_.size(); ++i) {
		// write the equivalent atoms
		if (atoms_[i]->eq()) {
			os << "1 " << i << " 1 0 " << getEqAtom(Var(i)) << " \n";
		}
		if ( atoms_[i]->value() != value_free ) {
			std::stringstream& str = atoms_[i]->value() == value_false ? bm : bp;
			str << i << "\n";
		}
		if (sym != ctx_->symTab().end() && Var(i) == sym->first) {
			if (sym->second.lit != negLit(sentVar) && !sym->second.name.empty()) {
				symTab << i << " " << sym->second.name.c_str() << "\n";
			}
			++sym;
		}
	}
	os << delimiter << "\n";
	os << symTab.str();
	os << delimiter << "\n";
	os << "B+\n" << bp.str() << "0\n"
		 << "B-\n" << bm.str() << "0\n1\n";
}

/////////////////////////////////////////////////////////////////////////////////////////
// Program mutating functions
/////////////////////////////////////////////////////////////////////////////////////////
#define check_not_frozen() check_precondition(!frozen_ && "Can't update frozen program!", std::logic_error)
Var ProgramBuilder::newAtom() {
	check_not_frozen();
	atoms_.push_back( new PrgAtomNode() );
	return (Var) atoms_.size() - 1;
}

ProgramBuilder& ProgramBuilder::setAtomName(Var atomId, const char* name) {
	check_not_frozen();
	check_precondition(atomId >= startAtom(), RedefinitionError);
	resize(atomId);
	ctx_->symTab().addUnique(atomId, name);
	return *this;
}

ProgramBuilder& ProgramBuilder::setCompute(Var atomId, bool pos) {
	PrgAtomNode* a = resize(atomId);
	ValueRep v     = pos ? value_weak_true : value_false;
	compute_.push_back(Literal(atomId, v==value_false));
	if (!a->setValue(v)) {
		setConflict();
	}
	return *this;
}

ProgramBuilder& ProgramBuilder::freeze(Var atomId) {
	check_not_frozen();
	check_precondition(incData_ && "ProgramBuilder::updateProgram() not called!", std::logic_error);
	PrgAtomNode* a = resize(atomId);
	if (atomId >= startAtom() && !a->frozen() && a->preds.empty()) {
		incData_->freeze_.push_back(atomId);
		a->setFrozen(true);
	}
	// else: atom is defined or from a previous step - ignore!
	return *this;
}

ProgramBuilder& ProgramBuilder::unfreeze(Var atomId) {
	check_not_frozen();
	check_precondition(incData_ && "ProgramBuilder::updateProgram() not called!", std::logic_error);
	PrgAtomNode* atom = resize(atomId);
	if (atomId >= startAtom() || atom->frozen()) {
		incData_->unfreeze_.push_back(atomId);
	}
	// else: atom is from a previous step - ignore!
	return *this;
}

ProgramBuilder& ProgramBuilder::addRule(PrgRule& r) {
	// simplify rule, mark literals
	if (startAtom() > 1) {
		updateRule(r);
	}
	PrgRule::RData rd = r.simplify(ruleState_);
	if (r.type() != ENDRULE) {     // rule is relevant
		check_not_frozen();
		upRules(r.type(), 1);
		if (handleNatively(r, rd)) { // and can be handled natively
			addRuleImpl(r, rd);
		}
		else {
			// rule is to be replaced with a set of normal rules
			clearRuleState(r);
			if (!transformNoAux(r, rd)) {
				// Since rule transformation needs aux atoms, we must
				// defer the transformation until all rules were added
				// because only then we can safely assign new unique consecutive atom ids.
				extended_.push_back(new PrgRule());
				extended_.back()->swap(r);
			}
			else {
				PrgRuleTransform rt;
				incTr(r.type(), rt.transformNoAux(*this, r));
			}
		}
	}
	else { // rule is not relevant - don't forget to clear rule state
		clearRuleState(r);
	}
	return *this;
}
#undef check_not_frozen
/////////////////////////////////////////////////////////////////////////////////////////
// Query functions
/////////////////////////////////////////////////////////////////////////////////////////
Literal ProgramBuilder::getLiteral(Var atomId) const {
	check_precondition(atomId < atoms_.size(), std::logic_error);
	return getAtom(getEqAtom(atomId))->literal();
}
void ProgramBuilder::getAssumptions(LitVec& out) const {
	check_precondition(frozen_, std::logic_error);
	if (incData_) {
		for (VarVec::const_iterator it = incData_->freeze_.begin(), end = incData_->freeze_.end(); it != end; ++it) {
			out.push_back( ~getLiteral(*it) );
		}
	}
}
/////////////////////////////////////////////////////////////////////////////////////////
// Program definition - private
/////////////////////////////////////////////////////////////////////////////////////////
bool ProgramBuilder::inCompute(Literal x) const {
	return std::find(compute_.begin(), compute_.end(), x) != compute_.end();
}
void ProgramBuilder::addRuleImpl(PrgRule& r, const PrgRule::RData& rd) {
	if (r.type() != OPTIMIZERULE) {
		Body b = findOrCreateBody(r, rd);
		if (b.first->value() == value_false || rd.value == value_false) {
			// a false body can't define any atoms
			b.first->propagateFalse(b.second, atoms_);
		}
		for (VarVec::const_iterator it = r.heads.begin(), end = r.heads.end(); it != end; ++it) {
			if (b.first->value() != value_false) {
				PrgAtomNode* a = resize(*it);
				check_precondition(*it >= startAtom() || a->frozen() || inCompute(negLit(*it)), RedefinitionError);
				if (r.body.empty())                  { a->setIgnore(true); }
				if (a->frozen() && a->preds.empty()) { unfreeze(*it); }
				// Note: b->heads may now contain duplicates. They are removed in PrgBodyNode::buildHeadSet.
				HeadType t = b.first->addHead(*it, r.type());
				// Similarly, duplicates in atoms_[*it]->preds_ are removed in PrgAtomNode::simplifyBodies.
				a->preds.push_back(HeadEdge(b.second, t));
			}
			ruleState_.popFromRule(*it);  // clear flag of head atoms
		}
		if (rd.value != value_free) { b.first->setValue(rd.value); }
	}
	else {
		check_precondition(r.heads.empty(), std::logic_error);
		ProgramBuilder::MinimizeRule* mr = new ProgramBuilder::MinimizeRule;
		for (WeightLitVec::const_iterator it = r.body.begin(), bEnd = r.body.end(); it != bEnd; ++it) {
			resize(it->first.var());
			ruleState_.popFromRule(it->first.var());
		}
		mr->lits_ = r.body;
		mr->next_ = minimize_;
		minimize_ = mr;
	}
}

void ProgramBuilder::updateRule(PrgRule& r) {
	for (WeightLitVec::iterator it = r.body.begin(), end = r.body.end(); it != end; ++it) {
		Var id = it->first.var();
		if (id < atoms_.size() && getEqAtom(id) != id) {
			it->first = Literal(getEqAtom(id), it->first.sign());
		}
	}
}

bool ProgramBuilder::handleNatively(const PrgRule& r, const PrgRule::RData& rd) const {
	ExtendedRuleMode m = eqOpts_.erMode;
	if (m == mode_native || m == mode_transform_integ || r.type() == BASICRULE || r.type() == OPTIMIZERULE) {
		return true;
	}
	else if (m == mode_transform) {
		return false;
	}
	else if (m == mode_transform_dynamic) {
		return (r.type() != CONSTRAINTRULE && r.type() != WEIGHTRULE)
			|| transformNoAux(r, rd) == false;
	}
	else if (m == mode_transform_choice) {
		return r.type() != CHOICERULE;
	}
	else if (m == mode_transform_card)   {
		return r.type() != CONSTRAINTRULE;
	}
	else if (m == mode_transform_weight) {
		return r.type() != CONSTRAINTRULE && r.type() != WEIGHTRULE;
	}
	assert(false && "unhandled extended rule mode");
	return true;
}

ProgramBuilder::Body ProgramBuilder::findOrCreateBody(PrgRule& r, const PrgRule::RData& rd) {
	ProgramBuilder::BodyRange eqRange = bodyIndex_.equal_range(rd.hash);
	for (bool sorted = false; eqRange.first != eqRange.second; ++eqRange.first) {
		PrgBodyNode& o = *bodies_[eqRange.first->second];
		if (o.type() == o.bodyType(r) && r.body.size() == o.size() && rd.posSize == o.posSize() && r.bound() == o.bound()) {
			// bodies are structurally equivalent - check if they contain the same literals
			// Note: at this point all literals of the given rule are marked
			if (!allLitsMarked(o)) {
				continue;
			}
			// weight-rules must be checked in N*logN because
			// previous "mark-and-test"-approach did not test for equal weights
			if (r.type() == WEIGHTRULE && !eqWeights(o, r.body, sorted)) {
				continue;
			}
			// found an equivalent body - clear flags, i.e. unmark all body literals of this rule
			for (WeightLitVec::const_iterator it = r.body.begin(), bEnd = r.body.end(); it != bEnd; ++it) {
				ruleState_.popFromRule(it->first.var());
			}
			return Body(&o, eqRange.first->second);
		}
	}
	// no corresponding body exists, create a new object
	// Note: the flags are cleared in the ctor of PrgBodyNode
	uint32 bodyId   = (uint32)bodies_.size();
	PrgBodyNode* b  = PrgBodyNode::create(bodyId, r, rd, *this);
	bodyIndex_.insert(BodyIndex::value_type(rd.hash, bodyId));
	bodies_.push_back(b);
	if (b->isSupported()) {
		initialSupp_.push_back(bodyId);
	}
	return Body(b, bodyId);
}

void ProgramBuilder::clearRuleState(const PrgRule& r) {
	for (VarVec::const_iterator it = r.heads.begin(), end = r.heads.end();  it != end; ++it) {
		// clear flag only if a node was added for the head!
		if ((*it) < ruleState_.size()) {
			ruleState_.popFromRule(*it);
		}
	}
	for (WeightLitVec::const_iterator it = r.body.begin(), bEnd = r.body.end(); it != bEnd; ++it) {
		ruleState_.popFromRule(it->first.var());
	}
}

bool ProgramBuilder::transformNoAux(const PrgRule& r, const PrgRule::RData&) const {
	return r.type() != CHOICERULE && (r.bound() == 1 || (r.body.size() <= 6 && choose((uint32)r.body.size(), r.bound()) <= 15));
}

void ProgramBuilder::transformExtended() {
	uint32 a   = numAtoms();
	if (incData_) {
		// remember starting position of aux atoms so
		// that we can remove them on next incremental step
		incData_->startAux_ = (uint32)atoms_.size();
	}
	PrgRuleTransform tm;
	for (RuleList::size_type i = 0; i != extended_.size(); ++i) {
		incTr(extended_[i]->type(), tm.transform(*this, *extended_[i]));
		delete extended_[i];
	}
	extended_.clear();
	incTrAux(numAtoms() - a);
}

void ProgramBuilder::transformIntegrity(uint32 maxAux) {
	if (stats.rules[CONSTRAINTRULE] == 0) {
		return;
	}
	BodyList integrity;
	uint32 A = static_cast<uint32>(atoms_.size());
	uint32 B = static_cast<uint32>(bodies_.size());
	for (uint32 i = 0, end = B; i != end; ++i) {
		PrgBodyNode* b = bodies_[i];
		if (!b->ignore() && b->type() == PrgBodyNode::COUNT_BODY && b->value() == value_false) {
			integrity.push_back(b);
		}
	}
	if (!integrity.empty() && (integrity.size() == 1 || (atoms_.size()/double(bodies_.size()) > 0.5 && integrity.size() / double(bodies_.size()) < 0.01))) {
		bodyIndex_.clear();
		frozen_ = false;
		uint32 F= 1;
		while (atoms_[F]->value() != value_false) { ++F; }
		assert(F < atoms_.size());
		for (BodyList::size_type i = 0; i != integrity.size(); ++i) {
			PrgBodyNode* b = integrity[i];
			uint32 est = b->bound()*( b->sumWeights()-b->bound() );
			if (est > maxAux) {
				break;
			} 
			maxAux -= est;
			startRule(CONSTRAINTRULE, b->bound());
			addHead(F);
			for (uint32 g = 0; g != b->size(); ++g) {
				addToBody(b->goal(g).var(), !b->goal(g).sign());
			}
			extended_.push_back(new PrgRule());
			extended_.back()->swap(rule_);
			transformExtended();
			for (; B != bodies_.size(); ++B) {
				PrgBodyNode* nb = bodies_[B];
				assert(!nb->hasVar());
				if (nb->heads_begin()->node() == 1) {
					nb->setValue(value_false);
					nb->setLiteral(b->literal());
				}
				else {
					PrgAtomNode* a = nb->size() == 1 ? atoms_[nb->goal(0).var()] : 0;
					if (!a || !a->hasVar()) {
						nb->setLiteral(posLit(vars_.add(Var_t::body_var)));
					}
					else {
						nb->setLiteral(nb->posSize() ? a->literal() : ~a->literal());
					}
					a = atoms_[nb->heads_begin()->node()];
					if (!a->hasVar()) {
						if (a->preds.size() == 1) {
							a->setLiteral(nb->literal());
							vars_.setAtomBody(nb->var());
						}
						else { a->setLiteral(posLit(vars_.add(Var_t::atom_var))); }
					}
				}
			}
			b->setIgnore(true);
		}
		for (uint32 i = A; i != atoms_.size(); ++i) {
			PrgAtomNode* a = atoms_[i];
			if (!a->hasVar()) {
				uint32 numB = static_cast<uint32>(a->preds.size());
				if      (numB == 0) { a->setValue(value_false); }
				else if (numB == 1) { a->setLiteral(bodies_[a->preds[0].node()]->literal()); vars_.setAtomBody(a->var()); }
				else /*  numB > 1 */{ a->setLiteral(posLit(vars_.add(Var_t::atom_var))); }
			}
		}
		frozen_ = true;
	}
}

// checks whether all literals in body are currently marked in the
// ProgramBuilder's ruleState-Object. Used for comparing bodies
// in a "mark-and-test"-approach
bool ProgramBuilder::allLitsMarked(const PrgBodyNode& body) {
	for (uint32 i = 0, end = body.size(); i != end; ++i) {
		if (!ruleState_.inBody(body.goal(i))) { return false; }				
	}
	return true;
}

// checks whether body and wl are equal w.r.t literals and weights
bool ProgramBuilder::eqWeights(const PrgBodyNode& body, WeightLitVec& lits, bool& sorted)  const {
	compose_2_2<std::less<Literal>, select1st<WeightLiteral>, select1st<WeightLiteral> > cmp;
	if (!sorted) {
		std::stable_sort(lits.begin(), lits.end(), cmp);
		sorted = true;
	}
	for (uint32 i = 0, end = body.size(); i != end; ++i) {
		WeightLiteral wl(body.goal(i), body.weight(i));
		WeightLitVec::iterator it = std::lower_bound(lits.begin(), lits.end(), wl, cmp);
		if (it == lits.end() || *it != wl) {
			return false;
		}
	}
	return body.size() == (uint32)lits.size();
}

namespace {
	struct LessBody {
		bool operator()(PrgBodyNode* lhs, PrgBodyNode* rhs) const {
			if (lhs->type() == rhs->type()) {
				if (lhs->size() == rhs->size()) {
					if (lhs->posSize() == rhs->posSize()) {
						for (uint32 i = 0; i != lhs->size(); ++i) {
							if (lhs->goal(i) != rhs->goal(i)) {
								return lhs->goal(i) < rhs->goal(i);
							}
						}
						return false;
					}
					return lhs->posSize() < rhs->posSize();
				}
				return lhs->size() < rhs->size();
			}
			return lhs->type() < rhs->type();
		}
	};
}

void ProgramBuilder::normalize() {
	bodyIndex_.clear();
	initialSupp_.clear();
	for (VarVec::size_type i = 0; i != atoms_.size(); ++i) {
		atoms_[i]->posDep.clear();
		atoms_[i]->negDep.clear();
		atoms_[i]->preds.clear();
	}
	for (VarVec::size_type i = 0; i != bodies_.size(); ++i) {
		bodies_[i]->sortBody();
	}
	std::sort(bodies_.begin(), bodies_.end(), LessBody());
	for (VarVec::size_type i = 0; i != bodies_.size(); ++i) {
		uint32 id= uint32(i);
		uint32 h = bodies_[i]->reinitDeps(id, *this);
		bodyIndex_.insert(BodyIndex::value_type(h, id));
		if (bodies_[i]->isSupported()) {
			initialSupp_.push_back(id);
		}
	}	
}

bool ProgramBuilder::prepareProgram() {
	assert(!frozen_);
	transformExtended();
	if (eqOpts_.normalize) { normalize(); }
	stats.atoms = numAtoms() - (startAtom()-1);
	stats.bodies= numBodies();
	updateFrozenAtoms();
	frozen_ = true;
	if (atoms_[0]->value() == value_true) { return false; }
	Preprocessor p;
	p.enableBackprop(eqOpts_.backprop);
	if (!p.preprocess(*this, eqOpts_.iters != 0 ? Preprocessor::full_eq : Preprocessor::no_eq, eqOpts_.iters, eqOpts_.dfOrder)) {
		return false;
	}
	if (eqOpts_.erMode == mode_transform_integ || eqOpts_.erMode == mode_transform_dynamic) {
		transformIntegrity(std::min(uint32(15000), uint32(numAtoms())<<1));
	}
	vars_.addTo(*ctx_, incData_ ? incData_->startVar_ : 1);
	ctx_->symTab().endInit();
	bodyIndex_.clear();
	stats.atoms = numAtoms() - (startAtom()-1);
	return true;
}

bool ProgramBuilder::transformProgram(bool checkSccs) {
	if (checkSccs && graph_.get() == 0) {
		graph_ = new SharedDependencyGraph();
	}
	uint32 scc = 0;
	if (incData_) {
		for (VarVec::const_iterator it = incData_->unfreeze_.begin(), end = incData_->unfreeze_.end(); it != end; ++it) {
			atoms_.push_back(atoms_[*it]);
		}
		scc = incData_->startScc_;
	}
	CycleChecker c(atoms_, bodies_, checkSccs, scc);
	ctx_->startAddConstraints();
	SatPreprocessor* p = 0;
	if (ctx_->satPrepro.get() && ctx_->satPrepro->limit(std::max(stats.atoms, stats.bodies))) {
		p = ctx_->satPrepro.release();
	}
	bool ret = addConstraints(c);
	if (p) ctx_->satPrepro.reset(p);
	if (checkSccs) stats.sccs = (c.sccs()-scc);
	if (ret && !c.sccAtoms().empty()) {
		uint32 oldNodes = graph_->nodes();
		graph_->addSccs(*ctx_, c.sccAtoms(), atoms_, bodies_);
		stats.ufsNodes  = graph_->nodes()-oldNodes;
	}
	if (incData_) {
		atoms_.resize( atoms_.size() - incData_->unfreeze_.size() );
		incData_->startScc_ = c.sccs();
	}
	return ret;
}

void ProgramBuilder::updateFrozenAtoms() {
	const Solver& solver = *ctx_->master();
	if (incData_ != 0) {
		// update truth values of atoms from previous iterations
		for (uint32 i = 1; i != incData_->startAtom_; ++i) {
			ValueRep v;
			if (atoms_[i]->hasVar() && (v = solver.value(atoms_[i]->var())) != value_free) {
				if (v == trueValue(atoms_[i]->literal())) {
					// set to strong true only if we are sure that atom has a valid support
					v = atoms_[i]->frozen() || atoms_[i]->value() != value_true ? value_weak_true : value_true;
				}
				else {
					v = value_false;
				}
				atoms_[i]->setValue(v);
			}
		}
		// remove protection of frozen atoms
		VarVec::iterator j = incData_->unfreeze_.begin();
		for (VarVec::iterator it = incData_->unfreeze_.begin(), end = incData_->unfreeze_.end(); it != end; ++it) {
			PrgAtomNode* a = atoms_[*it];
			if (a->frozen() && *it < startAtom()) {
				a->resetSccFlags();
				*j++ = *it;
			}
			a->setFrozen(false);
		}
		incData_->unfreeze_.erase(j, incData_->unfreeze_.end());
		// add protection for atoms still frozen
		j = incData_->freeze_.begin();
		Body emptyBody(static_cast<PrgBodyNode*>(0),0);
		for (VarVec::iterator it = j, end = incData_->freeze_.end(); it != end; ++it) {
			PrgAtomNode* a = atoms_[*it];
			if (a->frozen()) {
				assert(a->preds.empty() && "Can't freeze defined atom!");
				if (emptyBody.first == 0) { 
					rule_.clear(); rule_.setType(CHOICERULE);
					PrgRule::RData rd = {0,0,0,0};
					emptyBody = findOrCreateBody(rule_, rd); 
				}
				// Make atom a choice.
				// This way, no special handling during preprocessing/nogood creation is necessary
				HeadType t = emptyBody.first->addHead(*it, CHOICERULE);
				a->preds.push_back(HeadEdge(emptyBody.second, t));
				a->setIgnore(true);
				*j++ = *it;
			}
		}
		incData_->freeze_.erase(j, incData_->freeze_.end());
	}
}

bool ProgramBuilder::mergeEqAtoms(Var a, Var root) {
	PrgAtomNode* at = atoms_[a];
	root            = getEqAtom(root);
	PrgAtomNode* r  = atoms_[root];
	assert(!at->eq() && !r->eq());
	if (at->ignore()) {
		r->setIgnore(true);
	}
	if (!at->mergeValue(r)) {
		setConflict();
		return false;
	}
	assert(at->value() == r->value() || (r->value() == value_true && at->value() == value_weak_true));
	at->setEq(root);
	incEqs(Var_t::atom_var);
	return true;
}

/////////////////////////////////////////////////////////////////////////////////////////
// program creation - clark's completion
//
// Adds (completion) nogoods and initiates scc checking.
/////////////////////////////////////////////////////////////////////////////////////////
bool ProgramBuilder::addConstraints(CycleChecker& c) {
	ClauseCreator gc(ctx_->master());
	for (BodyList::const_iterator it = bodies_.begin(); it != bodies_.end(); ++it) {
		if ( !(*it)->toConstraint(*ctx_, gc, *this) ) { return false; }
		c.visit(*it);
	}
	const bool freezeAtoms = incData_ && ctx_->satPrepro.get() != 0;
	uint32 start           = startAtom();
	check_precondition(ctx_->symTab().curBegin() == ctx_->symTab().end() || start <= ctx_->symTab().curBegin()->first,
		std::logic_error);
	SymbolTable::const_iterator sym = ctx_->symTab().lower_bound(ctx_->symTab().curBegin(), start);
	for (AtomList::const_iterator it = atoms_.begin()+start; it != atoms_.end(); ++it) {
		if ( !(*it)->toConstraint(*ctx_, gc, *this) ) { return false; }
		c.visit(*it);
		if (sym != ctx_->symTab().end() && uint32(it-atoms_.begin()) == sym->first) {
			sym->second.lit = atoms_[getEqAtom(uint32(it-atoms_.begin()))]->literal();
			++sym;
		}
		if (freezeAtoms && (*it)->hasVar()) {
			ctx_->setFrozen((*it)->var(), true);
		}
	}
	return true;
}

// exclude vars contained in minimize statements from var elimination
void ProgramBuilder::freezeMinimize() {
	if (!minimize_) return;
	for (MinimizeRule* r = minimize_; r; r = r->next_) {
		for (WeightLitVec::iterator it = r->lits_.begin(); it != r->lits_.end(); ++it) {
			PrgAtomNode* h    = atoms_[it->first.var()];
			if (h->hasVar()) {
				ctx_->setFrozen(h->var(), true);
			}
		}
	}
}
/////////////////////////////////////////////////////////////////////////////////////////
// misc/helper functions
/////////////////////////////////////////////////////////////////////////////////////////
void ProgramBuilder::setConflict() {
	atoms_[0]->setValue(value_true);
}

PrgAtomNode* ProgramBuilder::resize(Var atomId) {
	assert(atomId != varMax && atomId > 0);
	while (atoms_.size() <= AtomList::size_type(atomId)) {
		atoms_.push_back( new PrgAtomNode() );
	}
	return atoms_[atomId];
}

void ProgramBuilder::writeRule(PrgBodyNode* b, Var falseAtom, std::ostream& os) {
	VarVec::size_type nbs = 0, pbs = 0;
	std::stringstream body;
	std::stringstream extended;
	for (uint32 p = 0; p < b->negSize(); ++p) {
		if (atoms_[b->neg(p)]->hasVar() ) {
			body << b->neg(p) << " ";
			++nbs;
			if (b->type() == PrgBodyNode::SUM_BODY) {
				extended << b->weight(p, false) << " ";
			}
		}
	}
	for (uint32 p = 0; p < b->posSize(); ++p) {
		if (atoms_[b->pos(p)]->hasVar() ) {
			body << b->pos(p) << " ";
			++pbs;
			if (b->type() == PrgBodyNode::SUM_BODY) {
				extended << b->weight(p, true) << " ";
			}
		}
	}
	body << extended.str();
	extended.str("");
	int choiceHeads = 0;
	for (HeadVec::const_iterator it = b->heads_begin(); it != b->heads_end(); ++it) {
		Var h = it->node();
		if (atoms_[h]->hasVar() || h == falseAtom) {
			if (it->normal()) {
				RuleType rt = BASICRULE;
				if      (b->type() == PrgBodyNode::COUNT_BODY) { rt = CONSTRAINTRULE; }
				else if (b->type() == PrgBodyNode::SUM_BODY)   { rt = WEIGHTRULE; }
				os << rt << " " << h << " ";
				if (rt == WEIGHTRULE) {
					os << b->bound() << " ";
				}
				os << pbs + nbs << " " << nbs << " ";
				if (rt == CONSTRAINTRULE) {
					os << b->bound() << " ";
				}
				os << body.str() << "\n";
			}
			else if (it->choice()) {
				++choiceHeads;
				extended << it->node() << " ";
			}
			else { assert(!"UNKNOWN RULE TYPE!"); }
		}
	}
	if (choiceHeads > 0) {
		os << CHOICERULE << " "
		   << choiceHeads << " " << extended.str()
			 << pbs + nbs << " " << nbs << " "
			 << body.str() << "\n";
	}
}
}
