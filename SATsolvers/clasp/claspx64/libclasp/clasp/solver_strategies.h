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
#ifndef CLASP_SOLVER_STRATEGIES_H_INCLUDED
#define CLASP_SOLVER_STRATEGIES_H_INCLUDED
#ifdef _MSC_VER
#pragma once
#endif

#include <clasp/constraint.h>
#include <clasp/util/misc_types.h>

/*!
 * \file 
 * Contains strategies used to configure search of a Solver.
 */
namespace Clasp {

//! Implements clasp's configurable schedule-strategies.
/*!
 * clasp currently supports the following basic strategies:
 *  - geometric sequence  : X = n1 * n2^k   (k >= 0)
 *  - arithmetic sequence : X = n1 + (n2*k) (k >= 0)
 *  - fixed sequence      : X = n1 + (0*k)  (k >= 0)
 *  - luby's sequence     : X = n1 * luby(k)(k >= 0) 
 *  .
 * Furthermore, an inner-outer scheme can be applied to the selected sequence.
 * In that case, the sequence is repeated every <limit>+j restarts, where
 * <limit> is the initial outer-limit and j is the number of times the
 * sequence was already repeated.
 *
 * \note For luby's seqeuence, j is not a repetition counter
 * but the index where the sequence grows to the next power of two.
 *
 * \see Luby et al. "Optimal speedup of las vegas algorithms."
 *
 */
struct ScheduleStrategy {
public:
	enum Type { geometric_schedule = 0, arithmetic_schedule = 1, luby_schedule = 2, user_schedule = 3 }; 
	
	ScheduleStrategy(Type t = geometric_schedule, uint32 b = 100, double g = 1.5, uint32 o = 0);
	//! Creates luby's sequence with unit-length unit and optional outer limit.
	static ScheduleStrategy luby(uint32 unit, uint32 limit = 0)              { return ScheduleStrategy(luby_schedule, unit, 0, limit);  }
	//! Creates geometric sequence base * (grow^k) with optional outer limit.
	static ScheduleStrategy geom(uint32 base, double grow, uint32 limit = 0) { return ScheduleStrategy(geometric_schedule, base, grow, limit);  }
	//! Creates arithmetic sequence base + (add*k) with optional outer limit.
	static ScheduleStrategy arith(uint32 base, double add, uint32 limit = 0) { return ScheduleStrategy(arithmetic_schedule, base, add, limit);  }
	//! Creates fixed sequence with length base.
	static ScheduleStrategy fixed(uint32 base)                               { return ScheduleStrategy(arithmetic_schedule, base, 0, 0);  }
	static ScheduleStrategy none()                                           { return ScheduleStrategy(geometric_schedule, 0); }
	static ScheduleStrategy def()                                            { return ScheduleStrategy(user_schedule, 0, 0.0); }
	uint64 current()  const;
	bool   disabled() const { return base == 0; }
	bool   defaulted()const { return base == 0 && type == user_schedule; }
	void   reset()          { idx  = 0;         }
	uint64 next();
	uint32 base : 30;  // base of sequence (n1)
	uint32 type :  2;  // type of basic sequence
	uint32 idx;        // current index into sequence
	uint32 len;        // length of sequence (0 if infinite) (once reached, sequence is repeated and len increased)
	float  grow;       // update parameter n2
	
};

uint32 lubyR(uint32 idx);
double growR(uint32 idx, double g);
double addR(uint32 idx, double a);
inline uint32 log2(uint32 x) {
	uint32 ln = 0;
	if (x & 0xFFFF0000u) { x >>= 16; ln |= 16; }
	if (x & 0xFF00u    ) { x >>=  8; ln |=  8; }
	if (x & 0xF0u      ) { x >>=  4; ln |=  4; }
	if (x & 0xCu       ) { x >>=  2; ln |=  2; }
	if (x & 0x2u       ) { x >>=  1; ln |=  1; }
	return ln;
}

//! Reduce strategy used during solving.
/*!
 * A reduce strategy mainly consists of an algorithm and a scoring scheme
 * for measuring "activity" of learnt constraints.
 */
struct ReduceStrategy {
	//! Reduction algorithm to use during solving.
	enum Algorithm {
		reduce_linear   = 0, /*!< Linear algorithm from clasp-1.3.x. */
		reduce_stable   = 1, /*!< Sort constraints by score but keep order in learnt db. */
		reduce_sort     = 2, /*!< Sort learnt db by score and remove fraction with lowest score. */
		reduce_heap     = 3  /*!< Similar to reduce_sort but only partially sorts learnt db.  */
	};
	//! Score to measure "activity" of learnt constraints.
	enum Score {
		score_act  = 0, /*!< Activity only: how often constraint is used during conflict analysis. */
		score_lbd  = 1, /*!< Use literal block distance as activity. */
		score_both = 2  /*!< Use activity and lbd together. */
	};
	static uint32 scoreAct(const Activity& act)  { return act.activity(); }
	static uint32 scoreLbd(const Activity& act)  { return uint32(128)-act.lbd(); }
	static uint32 scoreBoth(const Activity& act) { return (act.activity()+1) * scoreLbd(act); }
	ReduceStrategy() : glue(0), fReduce(75), fRestart(0), score(0), algo(0), noGlue(0), estimate(0) {}
	static int    compare(Score sc, const Clasp::Activity& lhs, const Clasp::Activity& rhs) {
		int fs = 0;
		if      (sc == score_act) { fs = ((int)scoreAct(lhs)) - ((int)scoreAct(rhs)); }
		else if (sc == score_lbd) { fs = ((int)scoreLbd(lhs)) - ((int)scoreLbd(rhs)); }
		return fs != 0 ? fs : ((int)scoreBoth(lhs)) - ((int)scoreBoth(rhs)); 
	}
	static uint32 asScore(Score sc, const Clasp::Activity& act) {
		if (sc == score_act)  { return scoreAct(act); }
		if (sc == score_lbd)  { return scoreLbd(act); }
		/*  sc == score_both*/{ return scoreBoth(act);}
	}
	uint32 glue    : 8; /*!< Don't remove nogoods with lbd <= glue.    */
	uint32 fReduce : 7; /*!< Fraction of nogoods to remove in percent. */
	uint32 fRestart: 7; /*!< Fraction of nogoods to remove on restart. */
	uint32 score   : 2; /*!< One of Score.                             */
	uint32 algo    : 2; /*!< One of Algorithm.                         */
	uint32 noGlue  : 1; /*!< Do not count glue clauses in limit        */
	uint32 estimate: 1; /*!< Use estimate of problem complexity in init*/
};

class DecisionHeuristic;

//! Parameter-Object for configuring a solver.
struct SolverStrategies {
	typedef DecisionHeuristic* (*HeuFactory)(const SolverStrategies&); 
	//! Clasp's two general search strategies
	enum SearchStrategy {
		use_learning = 0, /*!< Analyze conflicts and learn First-1-UIP-clause */
		no_learning  = 1  /*!< Don't analyze conflicts - chronological backtracking */
	};
	//! Antecedents to consider during conflict clause minimization.
	enum CCMinAntes {
		no_antes     = 0,  /*!< Don't minimize first-uip-clauses. */
		all_antes    = 1,  /*!< Consider all antecedents.         */
		short_antes  = 2,  /*!< Consider only short antecedents.  */
		binary_antes = 3,  /*!< Consider only binary antecedents. */
	};
	enum OptHeu {
		opt_sign     = 1,  /*!< Use optimize statements in sign heuristic */ 
		opt_model    = 2,  /*!< Apply model heuristic when optimizing */
	};
	enum SignDef {
		sign_type    = 0, /*!< prefer literal based on var's type */
		sign_no      = 1, /*!< prefer positive literal */
		sign_yes     = 2, /*!< prefer negative literal */
		sign_rnd     = 3, /*!< prefer random literal   */
	};
	enum WatchInit { watch_first = 0, watch_rand = 1, watch_least = 2 };
	static    HeuFactory heuFactory_s; 
	//! Creates a default-initialized object.
	SolverStrategies();
	RNG       rng;                 /*!< RNG used during search.    */
	uint32    compress;            /*!< If > 0, enable compression for learnt clauses of size > compress. */
	uint32    heuId    : 3;        /**< Type of decision heuristic. */
	uint32    heuOther : 2;        /**< Consider other learnt nogoods in heuristic (0=no, 1=loops, 2=all, 3=let heuristic decide) */
	uint32    optHeu   : 2;        /*!< Set of OptHeu values. */
	uint32    signDef  : 2;        /*!< One of SignDef. */
	uint32    signFix  : 1;        /**< Disable all sign heuristics and always use default sign. */
	uint32    heuReinit: 1;        /*!< Enable/disable reinitialization of existing vars in incremental setting */
	uint32    heuMoms  : 1;        /**< Use MOMS-score as top-level heuristic */
	uint32    berkHuang: 1;        /**< Only for Berkmin. */
	uint32    berkOnce : 1;        /**< Only for Berkmin. */
	uint32    unitNant : 1;        /**< Only for unit.    */
	uint32    heuParam :17;        /**< Extra parameter for heuristic with meaning depending on type */
	uint32    saveProgress : 16;   /*!< Enable progress saving if > 0. */
	uint32    loopRep      :  3;   /*!< How to represent loops? */
	uint32    ccMinAntes   :  2;   /*!< Antecedents to look at during conflict clause minimization. */
	uint32    reverseArcs  :  2;   /*!< Use "reverse-arcs" during learning if > 0. */
	uint32    otfs         :  2;   /*!< Enable "on-the-fly" subsumption if > 0. */
	uint32    updateLbd    :  2;   /*!< Update lbds of antecedents during conflict analysis. */
	uint32    initWatches  :  2;   /*!< Initialize watches randomly in clauses. */
	uint32    bumpVarAct   :  1;   /*!< Bump activities of vars implied by learnt clauses with small lbd. */
	uint32    strRecursive :  1;   /*!< If 1, use more expensive recursive nogood minimization. */
	uint32    search       :  1;   /*!< Current search strategy. */
};

}
#endif
