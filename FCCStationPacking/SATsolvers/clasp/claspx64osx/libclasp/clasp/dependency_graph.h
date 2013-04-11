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

#ifndef CLASP_DEPENDENCY_GRAPH_H_INCLUDED
#define CLASP_DEPENDENCY_GRAPH_H_INCLUDED

#ifdef _MSC_VER
#pragma once
#endif

#include <clasp/program_builder.h>

namespace Clasp {

//! (Positive) Body-Atom-Dependency Graph.
/*!
 * \ingroup shared
 *
 * Represents the PBADG of a logic program. Once initialized, the
 * PBDAG is static and read-only and thus can be shared between multiple solvers.
 *
 * \note Initialization is *not* thread-safe, i.e. must be done only once by one thread.
 */
class SharedDependencyGraph {
public:
	SharedDependencyGraph();
	~SharedDependencyGraph();
	typedef uint32 NodeId;

	//! Base type for nodes.
	struct Node {
		Node(Literal l = Literal(0, false), uint32 sc = PrgNode::noScc, uint32 t = type_normal) 
			: lit(l), scc(sc), type(t), adj_(0), sep_(0) {}
		enum { type_normal = 0, type_in_choice = 1, type_ext = 2, type_ext_weight = 3 };
		Literal lit;      // literal of this node
		uint32  scc : 30; // scc of this node
		uint32  type:  2; // type of body or atom
		NodeId* adj_;     // list of adjacent nodes
		NodeId* sep_;     // separates successor/predecessor nodes
	};
	//! A node and its id.
	template <class NT>
	struct NodePair {
		NodePair(const NT* n, NodeId i) : node(n), id(i) {}
		const NT* node;
		NodeId    id;
	};
	//! An atom node.
	/*!
	 * The PBDAG stores a node of type AtomNode for each non-trivially connected atom.
   * The predecessors of an AtomNode are the bodies that define the atom. Its successors
   * are those bodies from the same SCC that contain the atom positively.
   */
	struct AtomNode : public Node {
		AtomNode() {}
		//! Contained in extended bodies?
		bool          extended()   const { return (type & type_ext) != 0; }
		//! Contained in the head of a choice rule?
		bool          inChoice()   const { return (type & type_in_choice) != 0; }
		//! Bodies (i.e. predecessors): bodies from other SCCs precede those from same SCC.
		const NodeId* bodies()     const { return adj_; }
		const NodeId* bodies_end() const { return sep_; }
		
		//! Successors from same SCC [B1,...Bn, idMax].
		/*!
		 * \note If extended() is true, the atom is adjacent to some extended body.
		 * In that case, the returned list looks like this:
		 * [Bn1, ..., Bnj, idMax, Bext1, pos1, ..., Bextn, posn, idMax], where
		 * each Bni is a normal body, each Bexti is an extended body and posi is the
		 * position of this atom in Bexti. 
		 */
		const NodeId* succs() const { return sep_; }
	};
	
	//! A body node.
	/*!
	 * The PBDAG stores a node of type BodyNode for each body that defines 
	 * a non-trivially connected atom.
   * The predecessors of a BodyNode are the bodies subgoals.
	 * Its successors are the heads that are defined by the body.
	 * \note Normal bodies only store the positive subgoals from the same SCC, while
	 * extended rule bodies store all subgoals. In the latter case, the positive subgoals from
	 * the same SCC are stored as AtomNodes. All other subgoals are stored as literals.
   */
	struct BodyNode : public Node {
		explicit BodyNode(PrgBodyNode* b) : Node(b->literal(), b->scc()) {
			if (b->scc() == PrgNode::noScc || b->type() ==  PrgBodyNode::NORMAL_BODY) {
				type = Node::type_normal;
			}
			else {
				assert(b->type() == PrgBodyNode::COUNT_BODY || b->type() == PrgBodyNode::SUM_BODY);
				type = Node::type_ext + (b->type() == PrgBodyNode::SUM_BODY);
			}
		}
		//! Heads (i.e. successors): atoms from same SCC precede those from other SCCs.
		const NodeId* heads()     const { return adj_ + extended(); }
		const NodeId* heads_end() const { return sep_; }
		
		//! Predecessors from same SCC [a1,...an, idMax].
		/*!
		 * \note If extended() is true, the body stores all its subgoals and preds looks
		 * like this: [a1, [w1], ..., aj, [wj], idMax, l1, [w1], ..., lk, [wk], idMax], where
		 * each ai is an atom from the same SCC, each li is a literal of a subgoal from 
		 * other SCCs and wi is an optional weight (only for weight rules).
		 */
		const NodeId* preds() const { assert(scc != PrgNode::noScc); return sep_; }
		//! Returns idx of atomId in preds.
		uint32 get_pred_idx(NodeId atomId) const { 
			const uint32 inc = pred_inc();
			      uint32 idx = 0;
			for (const NodeId* x = preds(); *x != idMax; x += inc, ++idx) {
				if (*x == atomId) return idx;
			}
			return idMax;
		}
		NodeId get_pred(uint32 idx) const { return *(preds() + (idx*pred_inc())); }
		//! Increment to jump from one pred to the next.
		uint32     pred_inc() const { return 1 + (type == type_ext_weight); }
		//! Weight of ith subgoal.
		/*!
		 * \pre i in [0, num_preds())
		 */
		uint32  pred_weight(uint32 i, bool ext) const { 
			return (type != type_ext_weight) 
				? 1 
				: *(preds() + (i*pred_inc()) + (1+ext));
		}
		//! Number of predecessors (counting external subgoals).
		uint32    num_preds() const {
			if (scc == PrgNode::noScc) return 0;
			uint32 p        = 0;
			const NodeId* x = preds();
			const uint32 inc= pred_inc();
			for (; *x != idMax; x += inc) { ++p; }
			x += extended();
			for (; *x != idMax; x += inc) { ++p; }
			return p;
		}
		//! Is the body an extended body?
		bool       extended() const { return type != Node::type_normal; }
		//! Bound of extended body.
		weight_t  ext_bound() const { return *adj_; }
	};
	//! Adds SCCs to the graph.
	/*!
	 * \param ctx       The shared context object containing variables and the master solver.
	 * \param sccAtoms  Atoms of the logic program that are strongly connected.
	                    Must be a subset of prgAtoms; preferably ordered by SCC.
	 * \param prgAtoms  All atoms of the logic program.
	 * \param prgBodies All bodies of the logic program.
	 */
	void addSccs(SharedContext& ctx, const AtomList& sccAtoms, const AtomList& prgAtoms, const BodyList& prgBodies);

	//! Number of atoms in graph.
	uint32 numAtoms() const { return (uint32)atoms_.size();  }
	//! Number of bodies in graph.
	uint32 numBodies()const { return (uint32)bodies_.size(); }
	//! Sum of atoms and bodies.
	uint32    nodes() const { return numAtoms()+numBodies(); }

	//! Returns AtomNode and id of given atom.
	NodePair<AtomNode> getAtom(NodeId atomId) const {
		assert(atomId < atoms_.size());
		return NodePair<AtomNode>(&atoms_[atomId], atomId);
	}
	//! Returns AtomNode of atom with given id.
	const AtomNode& getAtomNode(NodeId atomId) const {
		assert(atomId < atoms_.size());
		return atoms_[atomId];
	}

	//! Returns BodyNode and id of given body.
	NodePair<BodyNode> getBody(NodeId bodyId) const {
		assert(bodyId < bodies_.size());
		return NodePair<BodyNode>(&bodies_[bodyId], bodyId);
	}
	
	//! Returns BodyNode of body with given id.
	const BodyNode& getBodyNode(NodeId bodyId) const {
		assert(bodyId < bodies_.size());
		return bodies_[bodyId];
	}

	//! Calls the given function object p once for each body containing the given atom.
	template <class P>
	void visitAtomSuccessors(NodeId atomId, const P& p) const {
		assert(atomId < atoms_.size());
		const NodeId* s;
		for (s = atoms_[atomId].succs(); *s != idMax; ++s) {
			p(getBody(*s), atomId);
		}
		if (atoms_[atomId].extended()) {
			for (++s; *s != idMax; s += 2) {
				p(getBody(*s), atomId, *(s+1));
			}
		}
	}
	//! Calls the given function object p once for each body-literal.
	template <class P>
	void visitBodyLiterals(const NodePair<BodyNode>& n, const P& p) {
		const NodeId* x  = n.node->preds();
		const uint32 inc = n.node->pred_inc();
		uint32       i   = 0;
		for (; *x != idMax; x += inc, ++i) { p(getAtomNode(*x).lit, n, i, false); }
		x += n.node->extended();
		for (; *x != idMax; x += inc, ++i) { p(Literal::fromRep(*x), n, i, true); }
	}

	// ONLY FOR TESTING!
	NodePair<AtomNode> getAtomByLit(Literal p) const {
		for (AtomVec::size_type i = 0; i != atoms_.size(); ++i) {
			if (atoms_[i].lit == p) return NodePair<AtomNode>(&atoms_[i], static_cast<NodeId>(i));
		}
		return NodePair<AtomNode>(0, idMax);
	}
private:
	SharedDependencyGraph(const SharedDependencyGraph&);
	SharedDependencyGraph& operator=(const SharedDependencyGraph&);
	inline bool relevantPrgAtom(const Solver& s, PrgAtomNode* a) const;
	inline bool relevantPrgBody(const Solver& s, PrgBodyNode* b) const;
	NodeId      addBody(SharedContext& ctx, PrgBodyNode*, const AtomList& progAtoms);
	typedef PodVector<AtomNode>::type AtomVec;
	typedef PodVector<BodyNode>::type BodyVec;
	AtomVec atoms_;
	BodyVec bodies_;
};

}
#endif
