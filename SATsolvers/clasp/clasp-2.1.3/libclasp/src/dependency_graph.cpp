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
#ifdef _MSC_VER
#pragma warning(disable : 4996) // std::copy was declared deprecated
#endif

#include <clasp/dependency_graph.h>
#include <clasp/solver.h>

namespace Clasp {

/////////////////////////////////////////////////////////////////////////////////////////
// class SharedDependencyGraph
/////////////////////////////////////////////////////////////////////////////////////////
SharedDependencyGraph::SharedDependencyGraph() {
}
SharedDependencyGraph::~SharedDependencyGraph() {
	for (AtomVec::size_type i = 0; i != atoms_.size(); ++i) {
		delete [] atoms_[i].adj_;
	}
	for (AtomVec::size_type i = 0; i != bodies_.size(); ++i) {
		delete [] bodies_[i].adj_;
	}
}

bool SharedDependencyGraph::relevantPrgAtom(const Solver& s, PrgAtomNode* a) const { 
	return !s.isFalse(a->literal()) && a->scc() != PrgNode::noScc;
}
bool SharedDependencyGraph::relevantPrgBody(const Solver& s, PrgBodyNode* b) const { 
	return !s.isFalse(b->literal()); 
}

// Creates a positive-body-atom-dependency graph (PBADG)
// The PBADG contains a node for each atom A of a non-trivial SCC and
// a node for each body B, s.th. there is a non-trivially connected atom A with
// B in body(A).
// Pre : a->ignore  = 0 for all new and relevant atoms a
// Pre : b->visited = 1 for all new and relevant bodies b
// Post: a->ignore  = 1 for all atoms that were added to the PBADG
// Post: b->visited = 0 for all bodies that were added to the PBADG
void SharedDependencyGraph::addSccs(SharedContext& ctx, const AtomList& sccAtoms, const AtomList& prgAtoms, const BodyList& prgBodies) {
	// Pass 1: Create graph atom nodes and estimate number of bodies
	NodeId atomId = static_cast<NodeId>(atoms_.size());
	atoms_.reserve(atoms_.size() + sccAtoms.size());
	AtomList::size_type numBodies = 0;
	for (AtomList::size_type i = 0, end = sccAtoms.size(); i != end; ++i) {
		PrgAtomNode* a = sccAtoms[i];
		if (relevantPrgAtom(*ctx.master(), a)) {
			// initialize graph atom node
			atoms_.push_back(AtomNode());
			AtomNode& ua = atoms_.back();
			ua.lit       = a->literal();
			ua.scc       = a->scc();
			numBodies   += a->preds.size();
			// store link between program node and graph node for later lookup
			a->setUfsNode(atomId, true);
			// atom is defined by more than just a bunch of clauses
			ctx.setFrozen(a->var(), true);
			++atomId;
		}
	}
	// Pass 2: Init atom nodes and create body nodes
	VarVec preds, succs, succsExt;
	NodeId* temp;
	bodies_.reserve(bodies_.size() + numBodies/2);
	for (AtomList::size_type i = 0, end = sccAtoms.size(); i != end; ++i) {
		PrgAtomNode* a = sccAtoms[i];
		if (relevantPrgAtom(*ctx.master(), a)) { 
			AtomNode& ua = atoms_[a->ufsNode()];
			for (HeadVec::const_iterator it = a->preds.begin(), endIt = a->preds.end(); it != endIt; ++it) {
				PrgBodyNode* prgBody = prgBodies[it->node()];
				if (relevantPrgBody(*ctx.master(), prgBody)) {
					NodeId bId = addBody(ctx, prgBody, prgAtoms);
					VarVec::iterator insPos = prgBody->scc() == a->scc() ? preds.end() : preds.begin();
					preds.insert(insPos, bId);
					if (it->choice()) {
						ua.type |= Node::type_in_choice;
					}
				}
			}
			for (VarVec::const_iterator it = a->posDep.begin(), endIt = a->posDep.end(); it != endIt; ++it) {
				PrgBodyNode* prgBody = prgBodies[*it];
				if (prgBody->scc() == a->scc() && relevantPrgBody(*ctx.master(), prgBody)) {
					NodeId bodyId = addBody(ctx, prgBody, prgAtoms);
					if (!bodies_[bodyId].extended()) {
						succs.push_back(bodyId);
					}
					else {
						succsExt.push_back(bodyId);
						succsExt.push_back(bodies_[bodyId].get_pred_idx(a->ufsNode()));
						assert(bodies_[bodyId].get_pred(succsExt.back()) == a->ufsNode());
						ua.type |= Node::type_ext;
					}
				}
			}
			succs.push_back(idMax);
			if (!succsExt.empty()) {
				succsExt.push_back(idMax);
			}
			ua.adj_   = new NodeId[preds.size()+succs.size()+succsExt.size()];
			ua.sep_   = std::copy(preds.begin(), preds.end(), ua.adj_);
			temp      = std::copy(succs.begin(), succs.end(), ua.sep_);
			std::copy(succsExt.begin(), succsExt.end(), temp);
			preds.clear(); succs.clear(); succsExt.clear();
		}
	}	
}

uint32 SharedDependencyGraph::addBody(SharedContext& ctx, PrgBodyNode* b, const AtomList& prgAtoms) {
	if (b->visited()) {       // first time we see this body - 
		b->setVisited(false);   // mark as visited and create node
		b->setUfsNode( (NodeId)bodies_.size(), false );
		bodies_.push_back(BodyNode(b));
		BodyNode* bn = &bodies_.back();
		// Init node
		VarVec preds, succs;
		if (bn->scc != PrgNode::noScc) {
			const bool weights = b->type() == PrgBodyNode::SUM_BODY;
			for (uint32 p = 0; p != b->posSize(); ++p) {
				PrgAtomNode* pred = prgAtoms[b->pos(p)];
				if (relevantPrgAtom(*ctx.master(), pred) && pred->scc() == bn->scc) {
					preds.push_back( pred->ufsNode() );
					if (weights) {
						preds.push_back( b->weight(p, true) );
					}
				}
			}
			preds.push_back(idMax);
			if (bn->extended()) {
				for (uint32 n = 0; n != b->size(); ++n) {
					PrgAtomNode* pred = prgAtoms[b->goal(n).var()];
					Literal lit       = b->goal(n).sign() ? ~pred->literal() : pred->literal();
					if ( (b->goal(n).sign() || pred->scc() != bn->scc) && !ctx.master()->isFalse(lit) ) {
						preds.push_back(lit.asUint());
						if (weights) {
							preds.push_back(b->weight(n));
						}
					}
				}
				preds.push_back(idMax);
			}
		}
		for (HeadVec::const_iterator it = b->heads_begin(), end = b->heads_end(); it != end; ++it) {
			PrgAtomNode* a = prgAtoms[it->node()];
			if (relevantPrgAtom(*ctx.master(), a)) {
				NodeId id = a->ufsNode();
				VarVec::iterator insPos = bn->scc == a->scc() ? succs.begin() : succs.end();
				succs.insert(insPos, id);
			}
		}
		bn->adj_   = new NodeId[succs.size() + bn->extended() + preds.size()];
		if (bn->extended()) {
			*bn->adj_= b->bound();
		}
		bn->sep_   = std::copy(succs.begin(), succs.end(), bn->adj_ + bn->extended());
		std::copy(preds.begin(), preds.end(), bn->sep_);
		ctx.setFrozen(b->var(), true);
	}
	return b->ufsNode();
}

}
