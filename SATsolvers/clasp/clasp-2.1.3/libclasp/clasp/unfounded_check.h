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

#ifndef CLASP_UNFOUNDED_CHECK_H_INCLUDED
#define CLASP_UNFOUNDED_CHECK_H_INCLUDED

#ifdef _MSC_VER
#pragma once
#endif
#include <clasp/solver.h>
#include <clasp/literal.h> 
#include <clasp/dependency_graph.h>
#include <clasp/constraint.h>
namespace Clasp {
class LoopFormula;


//! Clasp's default unfounded set checker.
/*!
 * \ingroup constraint
 * Searches for unfounded atoms by checking the positive dependency graph (PDG)
 * Basic Idea:
 *  - For each (non-false) atom a, let source(a) be a body B in body(a) that provides an external support for a
 *    - If no such B exists, a must be false
 *  - If source(a) becomes false and a is not false:
 *    - Let Q = {};
 *    - add a to Q
 *    - For each B' s.th B' is not external to Q
 *      - add { a' | source(a') = B } to Q
 *  - Try to find new sources for all atoms a in Q
 */
class DefaultUnfoundedCheck : public PostPropagator {
public:
	typedef SharedDependencyGraph DependencyGraph;
	typedef DependencyGraph::NodeId NodeId;
	//! Defines the supported reasons for explaining assignments.
	enum ReasonStrategy {
		common_reason,    /*!< one reason for each unfounded set but one clause for each atom */
		distinct_reason,  /*!< distinct reason and clause for each unfounded atom */
		shared_reason,    /*!< one shared loop formula for each unfounded set */
		only_reason,      /*!< store only the reason but don't learn a nogood */
	};
	
	DefaultUnfoundedCheck();
	~DefaultUnfoundedCheck();

	ReasonStrategy reasonStrategy() const { return strategy_; }

	//! Adds the unfounded set checker as a post propagator to the given solver.
	/*!
	 * Once the unfounded set checker is attached, it is owned by
	 * the solver. 
	 * \note Shall be called at most once!
	 */
	void   attachTo(Solver& s, DependencyGraph* graph);
	
	DependencyGraph* graph() const { return graph_; }
	uint32           nodes() const { return static_cast<uint32>(atoms_.size() + bodies_.size()); }

	// base interface
	bool   init(Solver&);
	void   reset();
	bool   propagateFixpoint(Solver& s);
	bool   propagate(Solver& s) { return DefaultUnfoundedCheck::propagateFixpoint(s); }
	uint32 priority() const     { return uint32(priority_single_high); }
private:
	DefaultUnfoundedCheck(const DefaultUnfoundedCheck&);
	DefaultUnfoundedCheck& operator=(const DefaultUnfoundedCheck&);
	typedef DependencyGraph::NodePair<DependencyGraph::BodyNode> BodyNodeP;
	typedef DependencyGraph::NodePair<DependencyGraph::AtomNode> AtomNodeP;
	// data for each body
	struct BodyData {
		BodyData() : watches(0), picked(0) {}
		uint32 watches : 31; // how many atoms watch this body as source?
		uint32 picked  :  1; // flag used in computeReason()
		uint32 lower_or_ext; // unsourced preds or index of extended body
	};
	// data for extended bodies
	struct ExtData {
		ExtData(weight_t bound, uint32 preds) : lower(bound) {
			for (uint32 i = 0; i != flagSize(preds); ++i) { flags[i] = 0; }
		}
		bool addToWs(uint32 idx, weight_t w) {
			const uint32 fIdx = (idx / 32);
			const uint32 m    = (1u << (idx & 31));
			assert((flags[fIdx] & m) == 0);
			flags[fIdx]      |= m;
			return (lower -= w) <= 0;
		}
		bool inWs(uint32 idx) const {
			const uint32 fIdx = (idx / 32);
			const uint32 m    = (1u << (idx & 31));
			return (flags[fIdx] & m) != 0;
		}
		void removeFromWs(uint32 idx, weight_t w) {
			if (inWs(idx)) {
				lower += w;
				flags[(idx / 32)] &= ~(uint32(1) << (idx & 31));
			}
		}
		static   uint32 flagSize(uint32 preds) { return (preds+31)/32; }
		weight_t lower;
		uint32   flags[0];
	};
	// data for each atom
	struct AtomData {
		AtomData() : source(nill_source), todo(0), ufs(0), validS(0) {}
		// returns the body that is currently watched as possible source
		NodeId watch()     const { return source; }
		// returns true if atom has currently a source, i.e. a body that can still define it
		bool   hasSource() const { return validS; }
		// mark source as invalid but keep the watch
		void   markSourceInvalid() { validS = 0; }
		// restore validity of source
		void   resurrectSource()   { validS = 1; }
		// sets b as source for this atom
		void   setSource(NodeId b) {
			source = b;
			validS = 1;
		}
		static const uint32 nill_source = (uint32(1) << 29)-1;
		uint32 source : 29; // id of body currently watched as source
		uint32 todo   :  1; // in todo-queue?
		uint32 ufs    :  1; // in ufs-queue?
		uint32 validS :  1; // is source valid?
	};
	// Watch-structure used to update extended rules affected by literal assignments
	struct ExtWatch {
		enum Type { watch_choice_false = 0, watch_body_goal_false = 1 };
		NodeId bodyId;
		uint32 data;
	};
	struct IdQueue {
		IdQueue() : front_(0) {}
		void push_back(NodeId id) { vec_.push_back(id); }
		void pop_front()          { ++front_; }
		void pop_back()           { vec_.pop_back(); }
		void clear()              { front_ = 0; vec_.clear(); }
		bool empty() const        { return front_ >= vec_.size(); }
		NodeId front() const      { return vec_[front_]; }
		VarVec::size_type front_;
		VarVec vec_;
	};
	// -------------------------------------------------------------------------------------------  
	// constraint interface
	PropResult propagate(Solver& s, Literal p, uint32&);
	void reason(Solver& s, Literal, LitVec&);
	// -------------------------------------------------------------------------------------------
	// initialization
	void   initBody(const BodyNodeP& n);
	void   initExtBody(const BodyNodeP& n);
	void   initSuccessors(const BodyNodeP& n, weight_t lower);
	void   addExtWatch(Literal p, const BodyNodeP& n, uint32 data);
	void   addExtWatch(Literal p, uint32 data);
	struct InitExtWatches {
		void operator()(Literal p, const BodyNodeP& B, uint32 idx, bool ext) const { 
			self->addExtWatch(~p, B, (idx<<1)+ext); 
			if (ext && !self->solver_->isFalse(p)) {
				extra->addToWs(idx, B.node->pred_weight(idx, true));
			}
		}
		DefaultUnfoundedCheck* self;
		ExtData*               extra;
	};
	// -------------------------------------------------------------------------------------------  
	// propagating source pointers
	void propagateSource(bool forceTodo=false);
	struct AddSource { // an atom in a body has a new source, check if body is now a valid source
		explicit AddSource(DefaultUnfoundedCheck* u) : self(u) {}
		// normal body
		void operator()(const BodyNodeP& n, NodeId) const {
			if (--self->bodies_[n.id].lower_or_ext == 0 && !self->solver_->isFalse(n.node->lit)) { self->forwardSource(n); }
		}
		// extended body
		void operator()(const BodyNodeP& n, NodeId atomId, uint32 idx) const;
		DefaultUnfoundedCheck* self;
	};
	struct RemoveSource {// an atom in a body has lost its source, check if body is no longer a valid source 
		explicit RemoveSource(DefaultUnfoundedCheck* u, bool add) : self(u), addTodo(add) {}
		// normal body
		void operator()(const BodyNodeP& n, NodeId) const { 
			if (++self->bodies_[n.id].lower_or_ext == 1 && self->bodies_[n.id].watches != 0) { 
				self->forwardUnsource(n, addTodo); 
			}
		}
		// extended body
		void operator()(const BodyNodeP& n, NodeId atomId, uint32 idx) const;
		DefaultUnfoundedCheck* self;
		bool                   addTodo;
	};
	void setSource(const AtomNodeP& n, const BodyNodeP& b);
	void removeSource(NodeId bodyId);
	void forwardSource(const BodyNodeP& n);
	void forwardUnsource(const BodyNodeP& n, bool add);
	void updateSource(AtomData& atom, const BodyNodeP& n);
	// -------------------------------------------------------------------------------------------  
	// finding & propagating unfounded sets
	bool findUnfoundedSet();
	bool findSource(NodeId atom);
	bool isValidSource(const BodyNodeP&);
	void addUnsourced(const BodyNodeP&);
	bool assertAtom();
	bool assertSet(); 
	bool assertAtom(Literal a);
	void computeReason();
	void addIfReason(const BodyNodeP&, uint32 uScc);
	void addReasonLit(Literal);
	struct AddReasonLit {
		void operator()(Literal p, const BodyNodeP&, NodeId, bool) const { if (self->solver_->isFalse(p)) self->addReasonLit(p); }
		DefaultUnfoundedCheck* self;
	};
	void createLoopFormula();
	// -------------------------------------------------------------------------------------------  
	// some helpers
	void enqueueTodo(NodeId atomId) {
		if (atoms_[atomId].todo == 0) {
			atoms_[atomId].todo = 1;
			todo_.push_back(atomId);
		}
	}
	NodeId dequeueTodo() {
		NodeId id = todo_.front();
		todo_.pop_front();
		atoms_[id].todo = 0;
		return id;
	}
	// Add atom a to the list of atoms for which a source pointer is needed
	// Pre: a->hasSource() == false
	void enqueueUnfounded(NodeId atomId) {
		if (atoms_[atomId].ufs == 0) {
			unfounded_.push_back(atomId);
			atoms_[atomId].ufs = 1;
		}
	}
	NodeId dequeueUnfounded() {
		NodeId id = unfounded_.front();
		unfounded_.pop_front();
		atoms_[id].ufs = 0;
		return id;
	} 
	// -------------------------------------------------------------------------------------------  
	typedef PodVector<AtomData>::type AtomVec;
	typedef PodVector<BodyData>::type BodyVec;
	typedef PodVector<ExtData*>::type ExtVec;
	typedef PodVector<ExtWatch>::type WatchVec;
	// -------------------------------------------------------------------------------------------  
	IdQueue          todo_;        // ids of atoms that recently lost their source
	IdQueue          unfounded_;   // ids of atoms that are unfounded wrt the current assignment (limited to one scc)
	AtomVec          atoms_;       // data for each atom       
	BodyVec          bodies_;      // data for each body
	ExtVec           extended_;    // data for each extended body
	WatchVec         watches_;     // watches for handling choice-, cardinality- and weight rules
	VarVec           sourceQ_;     // source-pointer propagation queue
	VarVec           invalid_;     // ids of bodies that became invalid during unit propagation - they can no longer be sources
	VarVec           invalidExt_;  // ids of extended watches that fired during unit propagation
	VarVec           reasonExt_;   // temporary vector for handling extended rules in computeReason()
	LitVec           loopAtoms_;   // only used if strategy_ == shared_reason
	Solver*          solver_;      // my solver
	DependencyGraph* graph_;       // PBADG
	LitVec*          reasons_;     // only used if strategy_ == only_reason. reasons_[v] reason why v is unfounded
	LitVec           activeClause_;// activeClause_[0] is the current unfounded atom
	ClauseInfo       info_;        // info on active clause
	ReasonStrategy   strategy_;    // what kind of reasons to compute?
};

}
#endif
