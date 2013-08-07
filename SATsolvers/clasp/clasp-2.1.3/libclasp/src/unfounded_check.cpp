// 
// Copyright (c) 2010, Benjamin Kaufmann
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
#ifdef _MSC_VER
#pragma warning(disable : 4996) // std::copy was declared deprecated
#endif

#include <clasp/unfounded_check.h>
#include <clasp/clause.h>
#include <algorithm>

namespace Clasp { 
/////////////////////////////////////////////////////////////////////////////////////////
// DefaultUnfoundedCheck - Construction/Destruction
/////////////////////////////////////////////////////////////////////////////////////////
// Choice rules are handled like normal rules with one exception:
//  Since BFA does not apply to choice rules, we manually trigger the removal of source pointers
//  whenever an atom sourced by a choice rule becomes false.
//
// The major problems with card/weight-rules are:
//  1. subgoals can circularly depend on the body
//  2. subgoal false -> body false, does not hold
// 
// Regarding the first point, consider: {b}. a:- 1{a,b}.
// Since b is external to 1{a,b}, the body is a valid source for a. Therefore, 1{a,b} can source a.
// After a's source pointer is set to 1{a,b}, both subgoals of 1{a,b} have a source. Nevertheless,
// we must not count a because it (circularly) depends on the body. I.e. as soon as b 
// becomes false, a is unfounded, because the loop {a} no longer has external bodies.
//
// The second point means that we have to watch *all* subgoals because we 
// may need to trigger source pointer removal whenever one of the subgoals becomes false.
// Consider: {a,b,c}. t :- 2 {b,t,x}. x :- t,c. x :- a.
// Now assume {t,c} is true and a becomes false. In this case, both x and t lose their
// source pointer and we get the (conflicting) unfounded set {x, t}.
// Further assume that after some backtracking we have that both {t,c} and a
// become false. Therefore x is false, too. Since we do not update source pointers on
// conflicts, x and t still have no source. Thus no removal of source pointers is triggered.
// If we would not watch x in 2 {b,t,x}, we could not add t to the todo queue and 
// we would therefore miss the unfounded set {t}.
//
// The implementation for extended bodies works as follows:
// - It distinguishes between internal literals, those that are in the same SCC as B
//   and external literals, those that are not.
// - Let W(l) be the weight of literal l in B and W(WS) be the sum of W(l) for each l in a set WS. 
// - The goal is to maintain a set WS of literals l, s.th l in Body and hasSource(l) AND W(WS) >= bound.
// - Initially WS contains all non-false external literals of B.
// - Whenever one of the internal literals of B becomes sourced, it is added to WS 
//   *only if* W(WS) < bound. In that case, it is guaranteed that the literal
//   currently does not circularly depend on the body.
// - As soon as W(WS) >= bound, we declare the body as valid source for its heads.
// - Whenever one of the literals l in WS loses its source, it is removed from WS.
//   If l is an external literal, new valid external literals are searched and added to WS 
//   until the source condition holds again.
// - If the condition cannot be restored, the body is marked as invalid source.

DefaultUnfoundedCheck::DefaultUnfoundedCheck()
	: solver_(0) 
	, graph_(0)
	, reasons_(0)
	, strategy_(common_reason) {
}
DefaultUnfoundedCheck::~DefaultUnfoundedCheck() { 
	for (ExtVec::size_type i = 0; i != extended_.size(); ++i) {
		::operator delete(extended_[i]);
	}
	delete [] reasons_;
}
/////////////////////////////////////////////////////////////////////////////////////////
// DefaultUnfoundedCheck - Initialization
/////////////////////////////////////////////////////////////////////////////////////////
void DefaultUnfoundedCheck::attachTo(Solver& s, DependencyGraph* graph) {
	assert(solver_ == 0 && graph_ == 0);
	solver_  = &s;
	graph_   = graph;
	strategy_= static_cast<ReasonStrategy>(s.strategies().loopRep);
	s.addPost(this);
}

// inits unfounded set checker with graph, i.e.
// - creates data objects for bodies and atoms
// - adds necessary watches to the solver
// - initializes source pointers
bool DefaultUnfoundedCheck::init(Solver&) {
	assert(solver_ && graph_ && "DefaultUnfoundedCheck::attachTo() not called!");
	AtomVec::size_type startAtom = atoms_.size();
	// set up new atoms
	atoms_.resize(graph_->numAtoms());
	// set up new bodies
	for (uint32 i = (uint32)bodies_.size(); i != graph_->numBodies(); ++i) {
		bodies_.push_back(BodyData());
		BodyNodeP n = graph_->getBody(i);
		if (!n.node->extended()) {
			initBody(n);
		}
		else {
			initExtBody(n);
		}
		// when a body becomes false, it can no longer be used as source
		solver_->addWatch(~n.node->lit, this, (n.id << 1));
	}
	// check for initially unfounded atoms
	propagateSource();
	for (AtomVec::size_type i = startAtom, end = atoms_.size(); i != end; ++i) {
		const DependencyGraph::AtomNode& a = graph_->getAtomNode(NodeId(i));
		if (!atoms_[i].hasSource() && !solver_->force(~a.lit, 0)) {
			return false;
		}
		if (a.inChoice()) {
			addExtWatch(~a.lit, NodeId(i));
		}
	}
	if (strategy_ == only_reason) {
		delete [] reasons_;
		reasons_ = new LitVec[solver_->numVars()];
	}
	return true;
}

// initializes a "normal" body, i.e. a body where lower(B) == size(B)
void DefaultUnfoundedCheck::initBody(const BodyNodeP& n) {
	assert(n.id < bodies_.size());
	BodyData& data = bodies_[n.id];
	// initialize lower to the number of predecessors from same scc that currently
	// have no source. One lower is 0, the body can source successors in its scc
	data.lower_or_ext  = n.node->num_preds();
	initSuccessors(n, data.lower_or_ext);
}

// initializes an "extended" body, i.e. a count/sum
// creates & populates WS and adds watches to all subgoals
void DefaultUnfoundedCheck::initExtBody(const BodyNodeP& n) {
	assert(n.id < bodies_.size() && n.node->extended());
	BodyData& data = bodies_[n.id];
	uint32 preds   = n.node->num_preds();
	ExtData* extra = new (::operator new(sizeof(ExtData) + (ExtData::flagSize(preds)*sizeof(uint32)))) ExtData(n.node->ext_bound(), preds);

	InitExtWatches addWatches = { this, extra };
	graph_->visitBodyLiterals(n, addWatches);
	
	data.lower_or_ext = (uint32)extended_.size();
	extended_.push_back(extra);
	initSuccessors(n, extra->lower);
}

// set n as source for its heads if possible and necessary
void DefaultUnfoundedCheck::initSuccessors(const BodyNodeP& n, weight_t lower) {
	if (!solver_->isFalse(n.node->lit)) {
		for (const NodeId* x = n.node->heads(); x != n.node->heads_end(); ++x) {
			const AtomNodeP& a = graph_->getAtom(*x);
			if (a.node->scc != n.node->scc || lower <= 0) {
				setSource(a, n);
			}
		}
	}
}

// watches needed to implement extended rules
void DefaultUnfoundedCheck::addExtWatch(Literal p, const BodyNodeP& B, uint32 data) {
	ExtWatch w = { B.id, data };
	solver_->addWatch(p, this, (uint32(watches_.size())<<1)+1);
	watches_.push_back(w);
}
void DefaultUnfoundedCheck::addExtWatch(Literal p, uint32 data) {
	ExtWatch w = { idMax, data };
	solver_->addWatch(p, this, (uint32(watches_.size())<<1)+1);
	watches_.push_back(w);
}

/////////////////////////////////////////////////////////////////////////////////////////
// DefaultUnfoundedCheck - Constraint interface
/////////////////////////////////////////////////////////////////////////////////////////
// a (relevant) literal was assigned. Check which node is affected and update it accordingly
Constraint::PropResult DefaultUnfoundedCheck::propagate(Solver& s, Literal, uint32& data) {
	assert(&s == solver_); (void)s;
	uint32 index  = data >> 1;
	uint32 type   = data & 1;
	if (type == 0) { // a body became false
		// update source if necessary
		if (bodies_[index].watches > 0) { invalid_.push_back(index); }
	}
	else if (watches_[index].bodyId == idMax) { // head of a choice rule became false
		// normally head false -> body false and hence the head has its source 
		// autmatically removed when the invalid body is processed;
		// for choice rules we must force source removal explicity
		const ExtWatch w = watches_[index];
		invalidExt_.push_back((w.data << 1) | ExtWatch::watch_choice_false);
	}
	else { // a literal relevant to a body became false
		invalidExt_.push_back((index << 1) | ExtWatch::watch_body_goal_false);
	}
	return PropResult(true, true);  // always keep the watch
}

void DefaultUnfoundedCheck::reason(Solver&, Literal p, LitVec& r) {
	LitVec::const_iterator it, end;
	if (!activeClause_.empty() && activeClause_[0] == p) {
		it  = activeClause_.begin()+1;
		end = activeClause_.end();
	}
	else {
		assert(strategy_ == only_reason && reasons_);
		it  = reasons_[p.var()-1].begin();
		end = reasons_[p.var()-1].end();
	}
	for (; it != end; ++it) r.push_back( ~*it );
}

/////////////////////////////////////////////////////////////////////////////////////////
// DefaultUnfoundedCheck - Base interface
/////////////////////////////////////////////////////////////////////////////////////////
void DefaultUnfoundedCheck::reset() {
	assert(loopAtoms_.empty());
	for (VarVec::size_type i = 0; i != todo_.vec_.size(); ++i) {
		atoms_[todo_.vec_[i]].todo = 0;
	}
	todo_.clear();
	for (VarVec::size_type i = 0; i != unfounded_.vec_.size(); ++i) {
		atoms_[unfounded_.vec_[i]].ufs = 0;
	}
	unfounded_.clear();
	invalidExt_.clear();
	invalid_.clear();
	activeClause_.clear();
}

bool DefaultUnfoundedCheck::propagateFixpoint(Solver&) {
	return solver_->strategies().search != SolverStrategies::no_learning 
		? assertAtom()
		: assertSet();
}

/////////////////////////////////////////////////////////////////////////////////////////
// DefaultUnfoundedCheck - source pointer propagation
/////////////////////////////////////////////////////////////////////////////////////////
// propagates recently set source pointers within one strong component.
void DefaultUnfoundedCheck::propagateSource(bool forceTodo) {
	for (LitVec::size_type i = 0; i < sourceQ_.size(); ++i) {
		NodeId atom = sourceQ_[i];
		if (atoms_[atom].hasSource()) {
			// propagate a newly added source-pointer
			graph_->visitAtomSuccessors(atom, AddSource(this));
		}
		else {
			graph_->visitAtomSuccessors(atom, RemoveSource(this, forceTodo));
		}
	}
	sourceQ_.clear();
}

// replaces current source of atom with n
void DefaultUnfoundedCheck::updateSource(AtomData& atom, const BodyNodeP& n) {
	if (atom.watch() != AtomData::nill_source) {
		--bodies_[atom.watch()].watches;
	}
	atom.setSource(n.id);
	++bodies_[n.id].watches;
}

// an atom in extended body n has a new source, check if n is now a valid source
void DefaultUnfoundedCheck::AddSource::operator()(const BodyNodeP& n, NodeId atomId, uint32 idx) const {
	assert(n.node->extended() && n.node->get_pred(idx) == atomId);
	(void)atomId;
	ExtData* ext = self->extended_[self->bodies_[n.id].lower_or_ext];
	if (ext->lower > 0 || self->bodies_[n.id].watches == 0) {
		// currently not a source - safely add pred to our watch set
		ext->addToWs(idx, n.node->pred_weight(idx, false));
	}
	if (!self->solver_->isFalse(n.node->lit) && ext->lower <= 0) {
		// valid source - propagate to heads
		self->forwardSource(n);
	}
}
// an atom in extended body n has lost its source, check if n is no longer a valid source
void DefaultUnfoundedCheck::RemoveSource::operator()(const BodyNodeP& n, NodeId atomId, uint32 idx) const {
	assert(n.node->extended() && n.node->get_pred(idx) == atomId); (void)atomId;
	ExtData* ext = self->extended_[self->bodies_[n.id].lower_or_ext];
	ext->removeFromWs(idx, n.node->pred_weight(idx, false));
	if (ext->lower > 0 && self->bodies_[n.id].watches > 0) {
		// extended bodies don't always become false if a predecessor becomes false
		// eagerly enqueue all successors watching this body
		self->forwardUnsource(n, true);
	}
}

// n is a valid source again, forward propagate this information to its heads
void DefaultUnfoundedCheck::forwardSource(const BodyNodeP& n) {
	for (const NodeId* x = n.node->heads(); x != n.node->heads_end(); ++x) {
		setSource(graph_->getAtom(*x), n);
	}
}

// n is no longer a valid source, forward propagate this information to its heads
void DefaultUnfoundedCheck::forwardUnsource(const BodyNodeP& n, bool add) {
	for (const NodeId* x = n.node->heads(); x != n.node->heads_end() && graph_->getAtomNode(*x).scc == n.node->scc; ++x) {
		if (atoms_[*x].hasSource() && atoms_[*x].watch() == n.id) {
			atoms_[*x].markSourceInvalid();
			sourceQ_.push_back(*x);
		}
		if (add && atoms_[*x].watch() == n.id) {
			enqueueTodo(*x);
		}
	}
}

// sets body as source for head if necessary.
// PRE: value(body) != value_false
// POST: source(head) != 0
void DefaultUnfoundedCheck::setSource(const AtomNodeP& head, const BodyNodeP& body) {
	assert(!solver_->isFalse(body.node->lit));
	// For normal rules from not false B follows not false head, but
	// for choice rules this is not the case. Therefore, the 
	// check for isFalse(head) is needed so that we do not inadvertantly
	// source a head that is currently false.
	if (!atoms_[head.id].hasSource() && !solver_->isFalse(head.node->lit)) {
		updateSource(atoms_[head.id], body);
		sourceQ_.push_back(head.id);
	}
}

// This function is called for each body that became invalid during propagation.
// Heads having the body as source have their source invalidated and are added
// to the todo queue. Furthermore, source pointer removal is propagated forward
void DefaultUnfoundedCheck::removeSource(NodeId bodyId) {
	const DependencyGraph::BodyNode& body = graph_->getBodyNode(bodyId);
	for (const NodeId* x = body.heads(); x != body.heads_end(); ++x) {
		if (atoms_[*x].watch() == bodyId) {
			if (atoms_[*x].hasSource()) {
				atoms_[*x].markSourceInvalid();
				sourceQ_.push_back(*x);
			}
			enqueueTodo(*x);
		}
	}
	propagateSource();
} 

/////////////////////////////////////////////////////////////////////////////////////////
// DefaultUnfoundedCheck - Finding & propagating unfounded sets
/////////////////////////////////////////////////////////////////////////////////////////
bool DefaultUnfoundedCheck::findUnfoundedSet() {
	// first: remove all sources that were recently falsified
	VarVec::size_type unpick = invalid_.size();
	for (VarVec::size_type i = 0; i != invalidExt_.size(); ++i) {
		uint32        id = invalidExt_[i] >> 1;
		ExtWatch::Type t = static_cast<ExtWatch::Type>(invalidExt_[i] & 1u);
		if (t == ExtWatch::watch_choice_false) {
			if (atoms_[id].hasSource() && !solver_->isFalse(graph_->getBodyNode(atoms_[id].watch()).lit)) {
				atoms_[id].markSourceInvalid();
				sourceQ_.push_back(id);
				propagateSource(true);
			}
		}
		else if (t == ExtWatch::watch_body_goal_false) {
			typedef DependencyGraph::BodyNode BodyNode;
			const ExtWatch& w = watches_[id];
			BodyData&       b = bodies_[w.bodyId];
			ExtData*      ext = extended_[b.lower_or_ext];
			const BodyNode& B = graph_->getBodyNode(w.bodyId);
			NodeId       pred = w.data >> 1;
			ext->removeFromWs(pred, B.pred_weight(pred, test_bit(w.data, 0) != 0));
			if (ext->lower > 0 && b.watches && b.picked == 0 && !solver_->isFalse(B.lit)) {
				invalid_.push_back(w.bodyId);
				b.picked = 1;
			}
		}
	}
	for (VarVec::size_type i = 0; i != invalid_.size(); ++i) {
		removeSource(invalid_[i]);
	}
	for (VarVec::size_type i = unpick; i != invalid_.size(); ++i) {
		bodies_[invalid_[i]].picked = 0;
	}
	invalid_.clear(); invalidExt_.clear();
	assert(sourceQ_.empty() && unfounded_.empty());
	// second: try to re-establish sources.
	while (!todo_.empty()) {
		NodeId head = dequeueTodo();
		if (!atoms_[head].hasSource() && !solver_->isFalse(graph_->getAtomNode(head).lit) && !findSource(head)) {
			return true;  // found an unfounded set - contained in unfounded_
		}
		assert(sourceQ_.empty());
	}
	todo_.clear();
	return false;     // no unfounded sets
}

// searches a new source for the atom node head.
// If a new source is found the function returns true.
// Otherwise the function returns false and unfounded_ contains head
// as well as atoms with no source that circularly depend on head.
bool DefaultUnfoundedCheck::findSource(NodeId head) {
	assert(unfounded_.empty());
	enqueueUnfounded(head);	// unfounded, unless we find a new source
	VarVec noSourceYet;
	bool changed = false;
	const NodeId* bodyIt, *bodyEnd;
	while (!unfounded_.empty()) {
		head = unfounded_.front();
		if (!atoms_[head].hasSource()) { // no source
			unfounded_.pop_front();        // note: current atom is still marked 
			AtomNodeP headNode(graph_->getAtom(head));
			for (bodyIt = headNode.node->bodies(), bodyEnd = headNode.node->bodies_end(); bodyIt != bodyEnd; ++bodyIt) {
				BodyNodeP bodyNode(graph_->getBody(*bodyIt));
				if (!solver_->isFalse(bodyNode.node->lit)) {
					if (bodyNode.node->scc != headNode.node->scc || isValidSource(bodyNode)) {
						atoms_[head].ufs = 0;          // found a new source,
						setSource(headNode, bodyNode); // set the new source
						propagateSource();             // and propagate it forward
						changed = true;                // may source atoms in noSourceYet!
						break;
					}
					else { addUnsourced(bodyNode); }
				}
			}
			if (!atoms_[head].hasSource()) {
				noSourceYet.push_back(head);// no source found
			}
		}
		else {  // head has a source
			dequeueUnfounded();
		}
	} // while unfounded_.emtpy() == false
	unfounded_.clear();
	if (changed) {
		// remove all atoms that have a source as they are not unfounded
		VarVec::iterator it;
		for (it = noSourceYet.begin(); it != noSourceYet.end(); ++it) {
			if ( atoms_[*it].hasSource() )   { atoms_[*it].ufs = 0; }
			else                             { unfounded_.push_back(*it); }
		}
	}
	else {
		// all atoms in noSourceYet are unfounded!
		noSourceYet.swap(unfounded_.vec_);
	}
	return unfounded_.empty();
}

// checks whether the body can source its heads
bool DefaultUnfoundedCheck::isValidSource(const BodyNodeP& n) {
	if (!n.node->extended()) {
		return bodies_[n.id].lower_or_ext == 0;
	}
	ExtData* ext = extended_[bodies_[n.id].lower_or_ext];
	if (ext->lower > 0) {
		// Since n is currently not a source, 
		// we here know that no literal with a source can depend on this body.
		// Hence, we can safely add all those literals to WS.
		
		// We check all internal literals here because there may be atoms
		// that were sourced *after* we established the watch set.
		const uint32 inc = n.node->pred_inc();
		const NodeId* x  = n.node->preds();
		uint32       p   = 0;
		for (; *x != idMax; x += inc, ++p) {
			if (atoms_[*x].hasSource() && !ext->inWs(p) && !solver_->isFalse(graph_->getAtomNode(*x).lit)) {
				ext->addToWs(p, n.node->pred_weight(p, false));
			}
		}
		// We check all external literals here because we do not update
		// the body on backtracking. Therefore some external literals that were false
		// may now be true/free.
		for (++x; *x != idMax; x += inc, ++p) {
			if (!solver_->isFalse(Literal::fromRep(*x)) && !ext->inWs(p)) {
				ext->addToWs(p, n.node->pred_weight(p, true));
			}
		}
	}
	return ext->lower <= 0;
}

// enqueues all predecessors of this body that currently lack a source
// PRE: isValidSource(n) == false
void DefaultUnfoundedCheck::addUnsourced(const BodyNodeP& n) {
	const uint32 inc = n.node->pred_inc();
	for (const NodeId* x = n.node->preds(); *x != idMax; x += inc) {
		if (!atoms_[*x].hasSource() && !solver_->isFalse(graph_->getAtomNode(*x).lit)) {
			enqueueUnfounded(*x);
		}
	}
}

// asserts all atoms of the unfounded set, then propagates
bool DefaultUnfoundedCheck::assertSet() {
	activeClause_.clear();
	while (findUnfoundedSet()) {
		while (!unfounded_.empty() && solver_->force(~graph_->getAtomNode(unfounded_.front()).lit, 0)) {
			dequeueUnfounded();
		}
		if (!unfounded_.empty() || !solver_->propagateUntil(this)) {
			return false;
		}
	}
	return true;
}

// as long as an unfounded set U is not empty,
// - asserts the first non-false atom
// - propagates
// - removes the atom from U
bool DefaultUnfoundedCheck::assertAtom() {
	while(findUnfoundedSet()) {
		activeClause_.clear();
		while (!unfounded_.empty()) {
			Literal a = graph_->getAtomNode(unfounded_.front()).lit;
			if (!solver_->isFalse(a) && !(assertAtom(a) && solver_->propagateUntil(this))) {
				break;
			}
			assert(solver_->isFalse(a));
			dequeueUnfounded();
		}
		if (!loopAtoms_.empty()) {
			createLoopFormula();
		}
		if (!unfounded_.empty()) { 
			return false; 
		}
		unfounded_.clear(); // clear underlying vector
	}
	return true;
}

// asserts an unfounded atom using the selected reason strategy
bool DefaultUnfoundedCheck::assertAtom(Literal a) {
	if (solver_->isTrue(a) || strategy_ == distinct_reason || activeClause_.empty()) {
		// Conflict, first atom of unfounded set, or distinct reason for each atom requested -
		// compute reason for a being unfounded.
		// We must flush any not yet created loop formula here - the
		// atoms in loopAtoms_ depend on the current reason which is about to be replaced. 
		if (!loopAtoms_.empty()) { 
			createLoopFormula(); 
		}
		activeClause_.assign(1, ~a);
		computeReason();
	}
	activeClause_[0] = ~a;
	bool noClause = solver_->isTrue(a) || strategy_ == only_reason || (strategy_ == shared_reason && activeClause_.size() > 3);
	if (noClause) {
		if (!solver_->force(~a, this)){ return false; }
		if (strategy_ == only_reason) { reasons_[a.var()-1].assign(activeClause_.begin()+1, activeClause_.end()); }
		else                          { loopAtoms_.push_back(~a); }
		return true;
	}
	else { // learn nogood and assert ~a
		return ClauseCreator::create(*solver_, activeClause_, info_).ok();
	}
}
void DefaultUnfoundedCheck::createLoopFormula() {
	assert(activeClause_.size() > 3);
	if (loopAtoms_.size() == 1) {
		activeClause_[0] = loopAtoms_[0];
		Constraint* ante = ClauseCreator::create(*solver_, activeClause_, info_).local;
		assert(ante != 0 && solver_->isTrue(loopAtoms_[0]) && solver_->reason(loopAtoms_[0]) == this);
		solver_->setReason(loopAtoms_[0], ante);
	}
	else {
		Activity act(info_.activity(), info_.lbd());
		LoopFormula* lf = LoopFormula::newLoopFormula(*solver_, &activeClause_[1], (uint32)activeClause_.size() - 1, (uint32)0, (uint32)loopAtoms_.size(), act); 
		solver_->addLearnt(lf, lf->size(), Constraint_t::learnt_loop);
		for (VarVec::size_type i = 0; i < loopAtoms_.size(); ++i) {
			assert(solver_->isTrue(loopAtoms_[i]) && solver_->reason(loopAtoms_[i]) == this);
			solver_->setReason(loopAtoms_[i], lf);
			lf->addAtom(loopAtoms_[i], *solver_);
		}
		lf->updateHeuristic(*solver_);
	}
	loopAtoms_.clear();
}

// computes the reason why a set of atoms is unfounded
void DefaultUnfoundedCheck::computeReason() {
	assert(reasonExt_.empty());
	uint32 ufsScc = graph_->getAtomNode(unfounded_.front()).scc;
	for (VarVec::size_type i = unfounded_.front_; i != unfounded_.vec_.size(); ++i) {
		const DependencyGraph::AtomNode& atom = graph_->getAtomNode(unfounded_.vec_[i]);
		if (!solver_->isFalse(atom.lit)) {
			assert(atom.scc == ufsScc);
			for (const NodeId* x = atom.bodies(); x != atom.bodies_end(); ++x) {
				addIfReason(graph_->getBody(*x), ufsScc);
			}
		}
	}
	for (VarVec::size_type i = 0; i != reasonExt_.size(); ++i) { bodies_[reasonExt_[i]].picked = 0; }
	reasonExt_.clear();
	info_     = ClauseInfo(Constraint_t::learnt_loop);
	uint32 dl = solver_->finalizeConflictClause(activeClause_, info_);
	assert(dl == solver_->decisionLevel() && "Loop nogood must contain a literal from current DL!");
	double ratio = activeClause_.size()/double(solver_->decisionLevel()+1);
	if (ratio > 10 && activeClause_.size() > 100 && !solver_->isFalse(activeClause_[0])) {
		activeClause_.resize(1);
		// replace reason with current decision sequence
		// start with last decision so that watches are correctly initialized
		for (uint32 x = dl; x; --x) {
			activeClause_.push_back(~solver_->decision(x));
		}
 	}
}

// check if n is part of the reason for the current unfounded set
void DefaultUnfoundedCheck::addIfReason(const BodyNodeP& n, uint32 uScc) {
	if (solver_->isFalse(n.node->lit)) {
		if (n.node->scc != uScc) {
			addReasonLit(n.node->lit);
		}
		else if (!solver_->seen(n.node->lit)) {
			if (!n.node->extended()) {
				// body is only a reason if it does not depend on the atoms from the unfounded set
				for (const NodeId* x = n.node->preds(); *x != idMax; ++x) {
					if (atoms_[*x].ufs && !solver_->isFalse(graph_->getAtomNode(*x).lit)) {
						return;
					}
				}
				addReasonLit(n.node->lit);
			}
			else if (bodies_[n.id].picked == 0) {
				bodies_[n.id].picked = 1;
				reasonExt_.push_back(n.id);
				// Check if the body depends on the atoms from the unfounded set. I.e.
				// would the body still be false if all but its unfounded literals would be true?
				ExtData* ext     = extended_[bodies_[n.id].lower_or_ext];
				weight_t temp    = ext->lower;
				const NodeId* x  = n.node->preds();
				const uint32 inc = n.node->pred_inc();
				uint32       p   = 0;
				for (; *x != idMax; x += inc, ++p) {
					if (!ext->inWs(p) && (atoms_[*x].ufs == 0 || solver_->isFalse(graph_->getAtomNode(*x).lit))) {
						if ( (temp -= n.node->pred_weight(p, false)) <= 0 ) {
							addReasonLit(n.node->lit);
							return;
						}
					}
				}
				for (++x; *x != idMax; x += inc, ++p) {
					if (!ext->inWs(p) && (temp -= n.node->pred_weight(p, true)) <= 0) {
						addReasonLit(n.node->lit);
						return;
					}
				}
			}
		}
	}
	else if (n.node->scc == uScc && n.node->extended() && bodies_[n.id].picked == 0) {
		bodies_[n.id].picked = 1;
		reasonExt_.push_back(n.id);
		// body is neither false nor a valid source - add all false lits to reason set
		AddReasonLit addFalseLits = { this };
		graph_->visitBodyLiterals(n, addFalseLits);
	}
}

void DefaultUnfoundedCheck::addReasonLit(Literal p) {
	if (!solver_->seen(p)) {
		solver_->markSeen(p);
		solver_->markLevel(solver_->level(p.var()));
		activeClause_.push_back(p);
		if (solver_->level(p.var()) > solver_->level(activeClause_[1].var())) {
			std::swap(activeClause_[1], activeClause_.back());
		}
	}
}

}
