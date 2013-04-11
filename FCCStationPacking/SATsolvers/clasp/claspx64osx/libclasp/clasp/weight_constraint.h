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
#ifndef CLASP_SMODELS_CONSTRAINTS_H_INCLUDED
#define CLASP_SMODELS_CONSTRAINTS_H_INCLUDED

#ifdef _MSC_VER
#pragma warning (disable : 4200) // nonstandard extension used : zero-sized array
#pragma once
#endif

#include <clasp/constraint.h>

namespace Clasp { namespace mt {

struct SharedWeightLits;

} // namespace mt

class WeightConstraint;	

//! Class storing a set of literals optionally associated with weights.
class WeightLits {
public:
	//! Number of literals in this object.
	uint32      size()           const { return size_;  }
	//! Returns the i'th literal of this object.
	Literal     lit(uint32 i)    const { return lits_[(i<<hasW_)]; }
	//! Returns the i'th variable of this object.
	Var         var(uint32 i)    const { return lits_[(i<<hasW_)].var(); }
	//! Returns true if literals in this object have weights.
	bool        hasWeights()     const { return hasW_ != 0; }
	//! Returns the weight of the i'th literal or 1 if hasWeights() is false.
	weight_t    weight(uint32 i) const { return hasW_ == 0 ? weight_t(1) : (weight_t)lits_[(i<<1)+1].asUint(); }
	//! Returns true if this object can be shared between solvers.
	bool        sharable()       const { return shared_ == 1; }
	//! Returns true if this object is currently not shared.
	bool        unique()         const;
	//! Destroys this object (or releases a reference to it if it is shared).
	void        destroy();
	//! Clones this object (or adds a reference to it if it is shared).
	WeightLits* clone();
private:
	WeightLits() {}
	WeightLits(uint32 s, bool shared, bool w) : size_(s), shared_(shared), hasW_(w) { }
	WeightLits(const WeightLits&);
	WeightLits& operator=(const WeightLits&);
	friend class  WeightConstraint;
	friend struct mt::SharedWeightLits;
	uint32   size_   : 30; // number of lits in constraint (counting the literal associated with the constraint)
	uint32   shared_ :  1; // if shared, this object is actually part of a SharedWeightConstraintLits object
	uint32   hasW_   :  1; // 1 if this is a weight constraint, otherwise 0 (in that case no weights are stored)
	Literal  lits_[0];     // Literals of constraint: ~B [Bw], l1 [w1], ..., ln-1 [Wn-1]
};

//! Class implementing smodels-like cardinality- and weight constraints.
/*!
 * \ingroup constraint
 * This class represents a constraint of type W == w1*x1 ... wn*xn >= B,
 * where W and each xi are literals and B and each wi are strictly positive integers.
 * The class is used to represent smodels-like weight constraint, i.e.
 * the body of a basic weight rule. In this case W is the literal associated with the body.
 * A cardinality constraint is handled like a weight constraint where all weights are equal to 1.
 *
 * The class implements the following four inference rules:
 * Let L be the set of literals of the constraint,
 * let sumTrue be the sum of the weights of all literals l in L that are currently true,
 * let sumReach be the sum of the weights of all literals l in L that are currently not false,
 * let U = {l in L | value(l.var()) == value_free}
 * - FTB: If sumTrue >= bound: set W to true.
 * - BFB: If W is false: set false all literals l in U for which sumTrue + weight(l) >= bound.
 * - FFB: If sumReach < bound: set W to false.
 * - BTB: If W is true: set true all literals l in U for which sumReach - weight(l) < bound.
 */
class WeightConstraint : public Constraint {
public:
	typedef WeightLits WL;

	//! Creates a new weight constraint from the given weight literals.
	/*!
	 * If the right hand side of the weight constraint is initially true/false (FTB/FFB),
	 * W is assigned appropriately but no constraint is created. Otherwise
	 * the new weight constraint is added to the context.
	 * \param ctx context in which the new constraint is to be used.
	 * \param W the literal that is associated with the constraint
	 * \param lits the literals of the weight constraint
	 * \param bound the lower bound of the weight constraint.
	 * \return false if the constraint is initially conflicting w.r.t the current assignment.
	 * \note Cardinality constraint are represented as weight constraints with all weights equal
	 * to 1.
	 */
	static bool newWeightConstraint(SharedContext& ctx, Literal W, WeightLitVec& lits, weight_t bound, Constraint** out = 0);
	// constraint interface
	Constraint* cloneAttach(Solver&);
	bool simplify(Solver& s, bool = false);
	void destroy(Solver*, bool);
	PropResult propagate(Solver& s, Literal p, uint32& data);
	void reason(Solver&, Literal p, LitVec& lits);
	bool minimize(Solver& s, Literal p, CCMinRecursive* r);
	void undoLevel(Solver& s);
	uint32 estimateComplexity(const Solver& s) const;
	/*!
	 * Logically, we distinguish two constraints: 
	 * FFB_BTB for handling forward false body and backward true body and
	 * FTB_BFB for handling forward true body and backward false body.
	 * Physically, we store the literals in one array: ~W=1, l0=w0,...,ln-1=wn-1.
	 */
	enum ActiveConstraint {
		FFB_BTB   = 0, /**< (SumW-bound)+1 [~W=1, l0=w0,..., ln-1=wn-1]; */
		FTB_BFB   = 1, /**< bound          [ W=1,~l0=w0,...,~ln-1=wn-1]  */
	};
	/*!
	 * Returns the i'th literal of constraint c, i.e.
	 *  li, iff c == FFB_BTB
	 * ~li, iff c == FTB_BFB.
	 */
	Literal  lit(uint32 i, ActiveConstraint c) const { return Literal::fromIndex( lits_->lit(i).index() ^ c ); }
	//! Returns the weight of the i'th literal or 1 if constraint is a cardinality constraint.
	weight_t weight(uint32 i)                  const { return lits_->weight(i); }
	//! Returns the number of literals in this constraint (including W).
	uint32   size()                            const { return lits_->size();    }
	//! Returns false if constraint is a cardinality constraint.
	bool     isWeight()                        const { return lits_->hasWeights();}
private:
	WeightConstraint(SharedContext& ctx, Literal W, const WeightLitVec& lits, uint32 bound, uint32 sumW, WL* out);
	WeightConstraint(Solver& s, const WeightConstraint& other);
	~WeightConstraint();
	static weight_t canonicalize(Solver& s, WeightLitVec& lits, weight_t& bound);
	static const uint32 NOT_ACTIVE = 3u;
	
	// Represents a literal on the undo stack.
	// idx()        returns the index of the literal.
	// constraint() returns the constraint that added the literal to the undo stack.
	// Note: Only 31-bits are used for undo info.
	// The remaining bit is used as a flag for marking processed literals.
	struct UndoInfo {
		explicit UndoInfo(uint32 d = 0) : data(d) {}
		uint32           idx()        const { return data >> 2; }
		ActiveConstraint constraint() const { return static_cast<ActiveConstraint>((data&2) != 0); }
		uint32 data; 
	};
	// Is literal idx contained as reason lit in the undo stack?
	bool litSeen(uint32 idx) const { return (undo_[idx].data & 1) != 0; }
	// Mark/unmark literal idx.
	void toggleLitSeen(uint32 idx) { undo_[idx].data ^= 1; }
	// Add watch for idx'th literal of c to the solver.
	void addWatch(Solver& s, uint32 idx, ActiveConstraint c);
	// Updates bound_[c] and adds an undo watch to the solver if necessary.
	// Then adds the literal at position idx to the reason set (and the undo stack).
	void updateConstraint(Solver& s, uint32 idx, ActiveConstraint c);
	// Returns the starting index of the undo stack.
	uint32   undoStart()       const { return lits_->hasWeights(); }
	UndoInfo undoTop()         const { assert(up_ != undoStart()); return undo_[up_-1]; }
	// Returns the decision level of the last assigned literal
	// or 0 if no literal was assigned yet.
	inline uint32	highestUndoLevel(Solver&) const;
	
	// Returns the index of next literal to look at during backward propagation.
	uint32   getBpIndex() const  { return !lits_->hasWeights() ? 1 : undo_[0].data>>1; }
	void     setBpIndex(uint32 n){ if (lits_->hasWeights()) undo_[0].data = (n<<1)+(undo_[0].data&1); }
	
	WL*      lits_;        // literals of constraint
	uint32   up_     : 29; // undo position; [undoStart(), up_] is the undo stack
	uint32   ownsLit_:  1; // owns lits_?
	uint32   active_ :  2; // which of the two sub-constraints is currently unit?
	weight_t bound_[2];    // FFB_BTB: (sumW-bound)+1 / FTB_BFB: bound
	UndoInfo undo_[0];     // undo stack + seen flag for each literal
};
}

#endif
