// 
// Copyright (c) 2006, Benjamin Kaufmann
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
#include "test.h"
#include <clasp/solver.h>
#include <clasp/program_builder.h>
#include <clasp/minimize_constraint.h>
#include <clasp/unfounded_check.h>
#include <sstream>
using namespace std;
namespace Clasp { namespace Test {
	struct ClauseObserver : public DecisionHeuristic {
		Literal doSelect(Solver&){return Literal();}
		void newConstraint(const Solver&, const Literal* first, LitVec::size_type size, ConstraintType) {
			Clause c;
			for (LitVec::size_type i = 0; i < size; ++i, ++first) {
				c.push_back(*first);
			}
			std::sort(c.begin(), c.end());
			clauses_.push_back(c);
		}
		typedef std::vector<Literal> Clause;
		typedef std::vector<Clause> Clauses;
		Clauses clauses_;
	};

class ProgramBuilderTest : public CppUnit::TestFixture {

  CPPUNIT_TEST_SUITE(ProgramBuilderTest);
	CPPUNIT_TEST(testIgnoreRules);
	CPPUNIT_TEST(testDuplicateRule);
	CPPUNIT_TEST(testNotAChoice);
	CPPUNIT_TEST(testMergeToSelfblocker);
	CPPUNIT_TEST(testMergeToSelfblocker2);
	CPPUNIT_TEST(testDerivedTAUT);
	CPPUNIT_TEST(testOneLoop);
	CPPUNIT_TEST(testZeroLoop);
	CPPUNIT_TEST(testEqSuccs);
	CPPUNIT_TEST(testEqCompute);
	CPPUNIT_TEST(testFactsAreAsserted);
	CPPUNIT_TEST(testSelfblockersAreAsserted);
	CPPUNIT_TEST(testConstraintsAreAsserted);
	CPPUNIT_TEST(testConflictingCompute);
	CPPUNIT_TEST(testForceUnsuppAtomFails);
	CPPUNIT_TEST(testTrivialConflictsAreDeteced);
	CPPUNIT_TEST(testBuildEmpty);
	CPPUNIT_TEST(testAddOneFact);
	CPPUNIT_TEST(testTwoFactsOnlyOneVar);
	CPPUNIT_TEST(testDontAddOnePredsThatAreNotHeads);
	CPPUNIT_TEST(testDontAddDuplicateBodies);
	CPPUNIT_TEST(testDontAddDuplicateSumBodies);
	CPPUNIT_TEST(testDontAddUnsupported);
	CPPUNIT_TEST(testDontAddUnsupportedNoEq);
	CPPUNIT_TEST(testDontAddUnsupportedExtNoEq);
	CPPUNIT_TEST(testAssertSelfblockers);
	
	CPPUNIT_TEST(testCloneShare);
	CPPUNIT_TEST(testCloneShareSymbols);
	CPPUNIT_TEST(testCloneFull);

	CPPUNIT_TEST(testBug);
	CPPUNIT_TEST(testSatBodyBug);
	CPPUNIT_TEST(testAddUnknownAtomToMinimize);
	CPPUNIT_TEST(testWriteWeakTrue);
	CPPUNIT_TEST(testSimplifyBodyEqBug);
	
	CPPUNIT_TEST(testAssertEqSelfblocker);
	CPPUNIT_TEST(testAddClauses);
	CPPUNIT_TEST(testAddCardConstraint);
	CPPUNIT_TEST(testAddWeightConstraint);
	CPPUNIT_TEST(testAddMinimizeConstraint);
	CPPUNIT_TEST(testNonTight);

	CPPUNIT_TEST(testIgnoreCondFactsInLoops);
	CPPUNIT_TEST(testCrEqBug);
	CPPUNIT_TEST(testEqOverChoiceRule);
	CPPUNIT_TEST(testEqOverBodiesOfDiffType);
	CPPUNIT_TEST(testEqOverComp);
	CPPUNIT_TEST(testNoBodyUnification);
	CPPUNIT_TEST(testNoEqAtomReplacement);
	CPPUNIT_TEST(testAllBodiesSameLit);

	CPPUNIT_TEST(testCompLit);
	CPPUNIT_TEST(testFunnySelfblockerOverEqByTwo);

	CPPUNIT_TEST(testRemoveKnownAtomFromWeightRule);
	CPPUNIT_TEST(testMergeEquivalentAtomsInConstraintRule);
	CPPUNIT_TEST(testMergeEquivalentAtomsInWeightRule);
	CPPUNIT_TEST(testBothLitsInConstraintRule);
	CPPUNIT_TEST(testBothLitsInWeightRule);
	CPPUNIT_TEST(testWeightlessAtomsInWeightRule);
	CPPUNIT_TEST(testSimplifyToNormal);
	CPPUNIT_TEST(testSimplifyToCardBug);
	CPPUNIT_TEST(testSimplifyCompBug);

	CPPUNIT_TEST(testBPWeight);

	CPPUNIT_TEST(testExtLitsAreFrozen);	
	CPPUNIT_TEST(writeIntegrityConstraint);

	CPPUNIT_TEST(testSimpleIncremental);
	CPPUNIT_TEST(testIncrementalFreeze);
	CPPUNIT_TEST(testIncrementalKeepFrozen);
	CPPUNIT_TEST(testIncrementalUnfreezeUnsupp);
	CPPUNIT_TEST(testIncrementalUnfreezeCompute);
	CPPUNIT_TEST(testIncrementalEq);
	CPPUNIT_TEST(testIncrementalEqComplement);
	CPPUNIT_TEST(testIncrementalEqUpdateAssigned);
	CPPUNIT_TEST(testIncrementalUnfreezeUnsuppEq);
	

	CPPUNIT_TEST(testIncrementalUnfreezeEq);

	CPPUNIT_TEST(testIncrementalCompute);
	CPPUNIT_TEST(testIncrementalComputeBackprop);
	CPPUNIT_TEST(testComputeTrueBug);
	CPPUNIT_TEST(testIncrementalStats);
	CPPUNIT_TEST(testIncrementalTransform);

	CPPUNIT_TEST(testBackprop);
	CPPUNIT_TEST(testIncrementalBackpropCompute);
	CPPUNIT_TEST(testIncrementalBackpropSolver);

	CPPUNIT_TEST(testIncrementalFreezeUnfreeze);
	CPPUNIT_TEST(testIncrementalSymbolUpdate);

	CPPUNIT_TEST(testIncrementalFreezeDefined);
	CPPUNIT_TEST(testIncrementalUnfreezeDefined);
	CPPUNIT_TEST(testIncrementalImplicitUnfreeze);
	CPPUNIT_TEST(testIncrementalRedefine);
	CPPUNIT_TEST(testIncrementalGetAssumptions);

	CPPUNIT_TEST(testMergeValue);
	CPPUNIT_TEST_SUITE_END();	
public:
	void tearDown(){
		ctx.reset();
	}
	
	void testIgnoreRules() {
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule().addHead(1).addToBody(1, true).endRule()  // a :- a.
			.startRule().addHead(2).addToBody(1, true).endRule()  // b :- a.
		;
		CPPUNIT_ASSERT_EQUAL(1u, builder.stats.rules[0]);
	}

	void testDuplicateRule() {
		builder.startProgram(ctx, ProgramBuilder::EqOptions().iterations(1))
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule(CHOICERULE).addHead(2).endRule()  // {b}.
			.startRule().addHead(1).addToBody(2, true).endRule()  // a :- b.
			.startRule().addHead(1).addToBody(2, true).endRule()  // a :- b.
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT(ctx.symTab()[1].lit == ctx.symTab()[2].lit);
	}

	void testNotAChoice() {
		// {b}.
		// {a} :- not b.
		// a :- not b.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule(CHOICERULE).addHead(2).endRule()
			.startRule(CHOICERULE).addHead(1).addToBody(2, false).endRule()
			.startRule().addHead(1).addToBody(2, false).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		const SymbolTable& index = ctx.symTab();
		ctx.master()->assume(~index[2].lit) && ctx.master()->propagate();
		// if b is false, a must be true because a :- not b. is stronger than {a} :- not b.
		CPPUNIT_ASSERT_EQUAL(true, ctx.master()->isTrue(index[1].lit));
	}
	
	void testMergeToSelfblocker() {
		// a :- not b.
		// b :- a.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule().addHead(1).addToBody(2, false).endRule()
			.startRule().addHead(2).addToBody(1, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(false, builder.endProgram() && ctx.endInit());
	}
	
	void testMergeToSelfblocker2() {
		// a :- not z.
		// a :- not x.
		// q :- not x.
		// x :- a.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "q").setAtomName(3, "x").setAtomName(4, "z")
			.startRule().addHead(1).addToBody(4, false).endRule()
			.startRule().addHead(1).addToBody(3, false).endRule()
			.startRule().addHead(2).addToBody(3, false).endRule()
			.startRule().addHead(3).addToBody(1, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL(true, ctx.master()->isTrue(ctx.symTab()[1].lit));
		CPPUNIT_ASSERT(ctx.numVars() == 0);
	}

	void testDerivedTAUT() {
		// {y, z}.
		// a :- not z.
		// x :- a.
		// a :- x, y.
		builder.startProgram(ctx)
			.setAtomName(1, "y").setAtomName(2, "z").setAtomName(3, "a").setAtomName(4, "x")
			.startRule(CHOICERULE).addHead(1).addHead(2).endRule()
			.startRule().addHead(3).addToBody(2, false).endRule()
			.startRule().addHead(4).addToBody(3, true).endRule()
			.startRule().addHead(3).addToBody(1, true).addToBody(4, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT(ctx.numVars() == 2);
	}

	void testOneLoop() {
		// a :- not b.
		// b :- not a.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule().addHead(1).addToBody(2, false).endRule()
			.startRule().addHead(2).addToBody(1, false).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL( 1u, ctx.numVars() );
		CPPUNIT_ASSERT_EQUAL( 0u, ctx.numConstraints() );
		CPPUNIT_ASSERT( ctx.symTab()[1].lit == ~ctx.symTab()[2].lit );
	}

	void testZeroLoop() {
		// a :- b.
		// b :- a.
		// a :- not x.
		// x :- not a.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "x")
			.startRule().addHead(1).addToBody(2, true).endRule()
			.startRule().addHead(2).addToBody(1, true).endRule()
			.startRule().addHead(1).addToBody(3, false).endRule()
			.startRule().addHead(3).addToBody(1, false).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL( 1u, ctx.numVars() );
		CPPUNIT_ASSERT_EQUAL( 0u, ctx.numConstraints() );
		CPPUNIT_ASSERT( ctx.symTab()[1].lit == ctx.symTab()[2].lit );
	}

	void testEqSuccs() {
		// {a,b}.
		// c :- a, b.
		// d :- a, b.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "d")
			.startRule(CHOICERULE).addHead(1).addHead(2).endRule()
			.startRule().addHead(3).addToBody(1, true).addToBody(2, true).endRule()
			.startRule().addHead(4).addToBody(1, true).addToBody(2, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL( 3u, ctx.numVars() );
		CPPUNIT_ASSERT_EQUAL( 3u, ctx.numConstraints() );
		CPPUNIT_ASSERT( ctx.symTab()[3].lit == ctx.symTab()[4].lit );
	}

	void testEqCompute() {
		// {x}.
		// a :- not x.
		// a :- b.
		// b :- a.
		// compute{b}.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "x")
			.startRule(CHOICERULE).addHead(3).endRule()
			.startRule().addHead(1).addToBody(3, false).endRule()
			.startRule().addHead(1).addToBody(2, true).endRule()
			.startRule().addHead(2).addToBody(1, true).endRule()
			.setCompute(2, true);
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL(true, ctx.master()->isTrue(ctx.symTab()[1].lit));
		PrgAtomNode* a = builder.getAtom(1);
		CPPUNIT_ASSERT(builder.getEqAtom(2) == 1);
		CPPUNIT_ASSERT(a->value() == value_true);
		const LitVec& c= builder.getCompute();
		CPPUNIT_ASSERT(c.size() == 1 && c[0].var() == 1);
	}

	void testFactsAreAsserted() {
		// a :- not x.
		// y.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "x").setAtomName(3, "y")
			.startRule().addHead(1).addToBody(2, false).endRule()	// dynamic fact
			.startRule().addHead(3).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL(true, ctx.master()->isTrue(ctx.symTab()[1].lit));
		CPPUNIT_ASSERT_EQUAL(true, ctx.master()->isTrue(ctx.symTab()[3].lit));
	}
	void testSelfblockersAreAsserted() {
		// a :- not a.
		// b :- not a.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule().addHead(1).addToBody(1, false).endRule()
			.startRule().addHead(2).addToBody(1, false).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(false, builder.endProgram() && ctx.endInit());
	}
	void testConstraintsAreAsserted() {
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule().addHead(1).addToBody(2, false).endRule()	// a :- not b.
			.startRule().addHead(2).addToBody(1, false).endRule()	// b :- not a.
			.setCompute(1, false)	// force not a
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL(true, ctx.master()->isFalse(ctx.symTab()[1].lit));
	}
	
	void testConflictingCompute() {
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule().addHead(1).addToBody(2, false).endRule()	// a :- not b.
			.startRule().addHead(2).addToBody(1, false).endRule()	// b :- not a.
			.setCompute(1, false)	// force not a
			.setCompute(1, true)	// force a
		;
		CPPUNIT_ASSERT_EQUAL(false, builder.endProgram() && ctx.endInit());
	}
	void testForceUnsuppAtomFails() {
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule().addHead(1).addToBody(2, false).endRule()	// a :- not b.
			.setCompute(2, true)	// force b
		;
		CPPUNIT_ASSERT_EQUAL(false, builder.endProgram() && ctx.endInit());
	}

	void testTrivialConflictsAreDeteced() {
		builder.startProgram(ctx)
			.setAtomName(1, "a")
			.startRule().addHead(1).addToBody(1, false).endRule()	// a :- not a.
			.setCompute(1, true)
		;
		CPPUNIT_ASSERT_EQUAL(false, builder.endProgram() && ctx.endInit());

	}
	void testBuildEmpty() {
		builder.startProgram(ctx);
		builder.endProgram();
		builder.writeProgram(str);
		CPPUNIT_ASSERT_EQUAL(0u, ctx.numVars());
		CPPUNIT_ASSERT(str.str() == "0\n0\nB+\n0\nB-\n0\n1\n");
	}
	void testAddOneFact() {
		builder.startProgram(ctx);
		builder.startRule().addHead(1).endRule().setAtomName(1, "A");
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT_EQUAL(0u, ctx.numVars());
		builder.writeProgram(str);
		std::string lp = "1 1 0 0 \n0\n1 A\n0\nB+\n1\n0\nB-\n0\n1\n";
		CPPUNIT_ASSERT_EQUAL(lp, str.str());

		// a fact does not introduce a constraint, it is just a top-level assignment
		CPPUNIT_ASSERT_EQUAL(0u, ctx.numConstraints());
	}
	
	void testTwoFactsOnlyOneVar() {
		builder.startProgram(ctx)
			.startRule().addHead(1).endRule()
			.startRule().addHead(2).endRule()
			.setAtomName(1, "A").setAtomName(2, "B")
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT_EQUAL(0u, ctx.numVars());
		builder.writeProgram(str);
		std::string lp = "1 1 0 0 \n1 2 1 0 1 \n0\n1 A\n2 B\n0\nB+\n1\n0\nB-\n0\n1\n";
		CPPUNIT_ASSERT_EQUAL(lp, str.str());
	}

	void testDontAddOnePredsThatAreNotHeads() {
		// a :- not b, not c.
		// c.
		builder.startProgram(ctx)
			.startRule().addHead(1).addToBody(2, false).addToBody(3, false).endRule()
			.startRule().addHead(3).endRule()
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c")
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT_EQUAL(0u, ctx.numVars());
		builder.writeProgram(str);
		std::string lp = "1 3 0 0 \n0\n3 c\n0\nB+\n3\n0\nB-\n0\n1\n"; 
		CPPUNIT_ASSERT_EQUAL(lp, str.str());
	}

	void testDontAddDuplicateBodies() {
		// a :- b, not c.
		// d :- b, not c.
		// b.
		// c.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "d")
			.startRule().addHead(1).addToBody(2, true).addToBody(3, false).endRule()
			.startRule().addHead(4).addToBody(2, true).addToBody(3, false).endRule()
			.startRule().addHead(2).addHead(3).endRule()		
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT_EQUAL(0u, ctx.numVars());
		builder.writeProgram(str);
		std::string lp = "1 2 0 0 \n1 3 1 0 2 \n0\n2 b\n3 c\n0\nB+\n2\n0\nB-\n0\n1\n";
		
		CPPUNIT_ASSERT_EQUAL(lp, str.str());
	}

	void testDontAddDuplicateSumBodies() {
		// {a, b, c}.
		// x :- 2 [a=1, b=2, not c=1].
		// y :- 2 [a=1, b=2, not c=1].
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "x").setAtomName(5, "y")
			.startRule(CHOICERULE).addHead(1).addHead(2).addHead(3).endRule()
			.startRule(WEIGHTRULE, 2).addHead(4).addToBody(1, true, 1).addToBody(2, true, 2).addToBody(3, false, 1).endRule()
			.startRule(WEIGHTRULE, 2).addHead(5).addToBody(1, true, 1).addToBody(2, true, 2).addToBody(3, false, 1).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT(builder.stats.bodies == 2);	
	}

	void testDontAddUnsupported() {
		// a :- c, b.
		// c :- not d.
		// b :- a.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "d")
			.startRule().addHead(1).addToBody(3, true).addToBody(2, true).endRule()
			.startRule().addHead(3).addToBody(4, false).endRule()
			.startRule().addHead(2).addToBody(1, true).endRule()		
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		builder.writeProgram(str);
		std::string lp = "1 3 0 0 \n0";
		CPPUNIT_ASSERT(str.str().find(lp) != std::string::npos);
	}

	void testDontAddUnsupportedNoEq() {
		// a :- c, b.
		// c :- not d.
		// b :- a.
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noEq())
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "d")
			.startRule().addHead(1).addToBody(3, true).addToBody(2, true).endRule()
			.startRule().addHead(3).addToBody(4, false).endRule()
			.startRule().addHead(2).addToBody(1, true).endRule()		
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT_EQUAL(2u, ctx.numVars());
		builder.writeProgram(str);
		std::string lp = "1 3 0 0 \n0\n3 c\n0\nB+\n0\nB-\n0\n1\n";
		CPPUNIT_ASSERT_EQUAL(lp, str.str());
	}

	void testDontAddUnsupportedExtNoEq() {
		// a :- not x.
		// c :- a, x.
		// b :- 2 {a, c, not x}.
		// -> 2 {a, c, not x} -> {a}
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noEq())
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "x")
			.startRule().addHead(1).addToBody(4, false).endRule() // a :- not x
			.startRule().addHead(3).addToBody(1, true).addToBody(4, true).endRule() // c :- a, x
			.startRule(CONSTRAINTRULE, 2).addHead(2).addToBody(1, true).addToBody(3, true).addToBody(4, false).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL(true, ctx.master()->isFalse(ctx.symTab()[3].lit));
		CPPUNIT_ASSERT_EQUAL(true, ctx.master()->isTrue(ctx.symTab()[1].lit));
		CPPUNIT_ASSERT_EQUAL(true, ctx.master()->isTrue(ctx.symTab()[2].lit));
	}

	void testAssertSelfblockers() {
		// a :- b, not c.
		// c :- b, not c.
		// b.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c")
			.startRule().addHead(1).addToBody(2, true).addToBody(3, false).endRule()
			.startRule().addHead(3).addToBody(2, true).addToBody(3, false).endRule()
			.startRule().addHead(2).endRule()		
		;
		// Program is unsat because b must be true and false at the same time.
		CPPUNIT_ASSERT_EQUAL(false, builder.endProgram() && ctx.endInit());
	}

	void testCloneShare() {
		// {a, b, c}.
		// d :- a, b, c. 
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noEq()) // no prepro
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "d")
			.startRule(CHOICERULE).addHead(1).addHead(2).addHead(3).endRule()
			.startRule().addHead(4).addToBody(1, true).addToBody(2, true).addToBody(3, true).endRule()
		;
		ctx.setSolvers(2);
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL( uint32(6), ctx.numVars() );
		CPPUNIT_ASSERT_EQUAL( uint32(6), ctx.numConstraints() );

		Solver solver2;
		ctx.attach(solver2);
		
		CPPUNIT_ASSERT_EQUAL( uint32(6), solver2.numVars() );
		CPPUNIT_ASSERT_EQUAL( uint32(6), solver2.numConstraints() );

		CPPUNIT_ASSERT(ctx.isShared());
	}

	void testCloneShareSymbols() {
		// {a, b, c}.
		// d :- a, b, c. 
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noEq()) // no prepro
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "d")
			.startRule(CHOICERULE).addHead(1).addHead(2).addHead(3).endRule()
			.startRule().addHead(4).addToBody(1, true).addToBody(2, true).addToBody(3, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL( uint32(6), ctx.numVars() );
		CPPUNIT_ASSERT_EQUAL( uint32(6), ctx.numConstraints() );

		SharedContext ctx2(ctx, SharedContext::init_share_symbols);
		CPPUNIT_ASSERT_EQUAL(true, builder.cloneProgram(ctx2) && ctx2.endInit());
		CPPUNIT_ASSERT_EQUAL( uint32(6), ctx2.numVars() );
		CPPUNIT_ASSERT_EQUAL( uint32(6), ctx2.numConstraints() );
		CPPUNIT_ASSERT(!ctx.isShared());
		CPPUNIT_ASSERT(!ctx2.isShared());
		CPPUNIT_ASSERT( &ctx.symTab() == &ctx2.symTab() );
	}
	void testCloneFull() {
		// {a, b, c}.
		// d :- a, b, c. 
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noEq()) // no prepro
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "d")
			.startRule(CHOICERULE).addHead(1).addHead(2).addHead(3).endRule()
			.startRule().addHead(4).addToBody(1, true).addToBody(2, true).addToBody(3, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL( uint32(6), ctx.numVars() );
		CPPUNIT_ASSERT_EQUAL( uint32(6), ctx.numConstraints() );

		SharedContext ctx2;
		CPPUNIT_ASSERT_EQUAL(true, builder.cloneProgram(ctx2) && ctx2.endInit());
		CPPUNIT_ASSERT_EQUAL( uint32(6), ctx2.numVars() );
		CPPUNIT_ASSERT_EQUAL( uint32(6), ctx2.numConstraints() );
		CPPUNIT_ASSERT(!ctx.isShared());
		CPPUNIT_ASSERT(!ctx2.isShared());
		CPPUNIT_ASSERT( &ctx.symTab() != &ctx2.symTab() );
	}

	void testBug() {
		builder.startProgram(ctx)
			.setAtomName(1, "d").setAtomName(2, "c").setAtomName(3, "b").setAtomName(4, "a")
			.startRule().addHead(1).addToBody(2, true).endRule()								// d :- c
			.startRule().addHead(2).addToBody(3, true).addToBody(1, true).endRule() // c :- b, d.
			.startRule().addHead(3).addToBody(4, true).endRule()								// b:-a.
			.startRule().addHead(4).addToBody(3, false).endRule()								// a:-not b.
		;
		CPPUNIT_ASSERT_EQUAL(false, builder.endProgram() && ctx.endInit());
	}

	void testSatBodyBug() {
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "d")
			.startRule(CHOICERULE).addHead(3).addHead(2).addHead(1).endRule()
			.startRule().addHead(1).endRule()		// a.
			.startRule(CONSTRAINTRULE).setBound(1).addHead(2).addToBody(1, true).addToBody(3, true).endRule() // b :- 1 {a, c}.
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL(value_free, ctx.master()->value(ctx.symTab()[3].lit.var()));
	}

	void testAddUnknownAtomToMinimize() {
		builder.startProgram(ctx)
			.startRule(OPTIMIZERULE).addToBody(1, true, 1).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL(true, builder.hasMinimize());
	}

	void testWriteWeakTrue() {
		// {z}.
		// x :- not y, z.
		// y :- not x.
		// compute{x}.
		builder.startProgram(ctx)
			.setAtomName(1, "x").setAtomName(2, "y").setAtomName(3, "z")
			.startRule(CHOICERULE).addHead(3).endRule()
			.startRule().addHead(1).addToBody(2, false).addToBody(3, true).endRule()
			.startRule().addHead(2).addToBody(1, false).endRule()
			.setCompute(1, true)
		; 
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		builder.writeProgram(str);
		std::string bp("B+\n1\n");
		CPPUNIT_ASSERT(str.str().find(bp) != std::string::npos);
	}

	void testSimplifyBodyEqBug() {
		// {x,y,z}.
		// a :- x,z.
		// b :- x,z.
		// c :- a, y, b.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "x").setAtomName(5, "y").setAtomName(6, "z")
			.startRule(CHOICERULE).addHead(4).addHead(5).addHead(6).endRule()
			.startRule().addHead(1).addToBody(4, true).addToBody(6, true).endRule()
			.startRule().addHead(2).addToBody(4, true).addToBody(6, true).endRule()
			.startRule().addHead(3).addToBody(1, true).addToBody(5, true).addToBody(2, true).endRule()
		; 
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		PrgAtomNode* a = builder.getAtom(1);
		PrgAtomNode* b = builder.getAtom(2);
		CPPUNIT_ASSERT(b->eq() && b->eqNode() == 1);
		CPPUNIT_ASSERT(a->posDep.size() == 1);
	}

	void testAssertEqSelfblocker() {
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "x").setAtomName(3, "y").setAtomName(4, "q").setAtomName(5, "r")
			.startRule().addHead(1).addToBody(2, false).addToBody(3, false).endRule()	// a :- not x, not y.
			.startRule().addHead(1).addToBody(4, false).addToBody(5, false).endRule()	// a :- not q, not r.
			.startRule().addHead(2).addToBody(3, false).endRule() // x :- not y.
			.startRule().addHead(3).addToBody(2, false).endRule()	// y :- not x.
			.startRule().addHead(4).addToBody(5, false).endRule() // q :- not r.
			.startRule().addHead(5).addToBody(4, false).endRule()	// r :- not q.								
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL(2u, ctx.numVars());
		CPPUNIT_ASSERT_EQUAL(true, ctx.master()->isFalse(ctx.symTab()[1].lit));
	}

	void testAddClauses() {
		ClauseObserver* o = new ClauseObserver;
		ctx.master()->setHeuristic(7, o);
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c")
			.startRule().addHead(1).addToBody(2, false).endRule()								// a :- not b.
			.startRule().addHead(1).addToBody(2, true).addToBody(3, true).endRule() // a :- b, c.
			.startRule().addHead(2).addToBody(1, false).endRule()								// b :- not a.
			.startRule().addHead(3).addToBody(2, false).endRule()								// c :- not b.
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL(3u, ctx.numVars());
		CPPUNIT_ASSERT_EQUAL(0u, ctx.master()->numAssignedVars());
	
		CPPUNIT_ASSERT_EQUAL(8u, ctx.numConstraints());

		Var bodyNotB = 1;
		Var bodyBC = 3;
		CPPUNIT_ASSERT_EQUAL(Var_t::body_var, ctx.type(3));
		Literal a = ctx.symTab()[1].lit;
		CPPUNIT_ASSERT(ctx.symTab()[2].lit == ~ctx.symTab()[1].lit);

		// a - HeadClauses
		ClauseObserver::Clause ac;
		ac.push_back(~a);
		ac.push_back(posLit(bodyNotB));
		ac.push_back(posLit(bodyBC));
		std::sort(ac.begin(), ac.end());
		CPPUNIT_ASSERT(std::find(o->clauses_.begin(), o->clauses_.end(), ac) != o->clauses_.end());
		
		// bodyNotB - Body clauses
		ClauseObserver::Clause cl;
		cl.push_back(negLit(bodyNotB)); cl.push_back(~ctx.symTab()[2].lit);
		std::sort(cl.begin(), cl.end());
		CPPUNIT_ASSERT(std::find(o->clauses_.begin(), o->clauses_.end(), cl) != o->clauses_.end());
		cl.clear();
		cl.push_back(posLit(bodyNotB)); cl.push_back(ctx.symTab()[2].lit);
		std::sort(cl.begin(), cl.end());
		CPPUNIT_ASSERT(std::find(o->clauses_.begin(), o->clauses_.end(), cl) != o->clauses_.end());
		cl.clear();
		cl.push_back(negLit(bodyNotB)); cl.push_back(a);
		std::sort(cl.begin(), cl.end());
		CPPUNIT_ASSERT(std::find(o->clauses_.begin(), o->clauses_.end(), cl) != o->clauses_.end());
		
		// bodyBC - Body clauses
		cl.clear();
		cl.push_back(negLit(bodyBC)); cl.push_back(ctx.symTab()[2].lit);
		std::sort(cl.begin(), cl.end());
		CPPUNIT_ASSERT(std::find(o->clauses_.begin(), o->clauses_.end(), cl) != o->clauses_.end());
		cl.clear();
		cl.push_back(negLit(bodyBC)); cl.push_back(ctx.symTab()[3].lit);
		std::sort(cl.begin(), cl.end());
		CPPUNIT_ASSERT(std::find(o->clauses_.begin(), o->clauses_.end(), cl) != o->clauses_.end());
		cl.clear();
		cl.push_back(posLit(bodyBC)); cl.push_back(~ctx.symTab()[2].lit); cl.push_back(~ctx.symTab()[3].lit);
		std::sort(cl.begin(), cl.end());
		CPPUNIT_ASSERT(std::find(o->clauses_.begin(), o->clauses_.end(), cl) != o->clauses_.end());
		cl.clear();
		cl.push_back(negLit(bodyBC)); cl.push_back(a);
		std::sort(cl.begin(), cl.end());
		CPPUNIT_ASSERT(std::find(o->clauses_.begin(), o->clauses_.end(), cl) != o->clauses_.end());
	}

	void testAddCardConstraint() {
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "d")
			// a :- 1 { not b, c, d }
			// {b,c}.
			.startRule(CONSTRAINTRULE).setBound(1).addHead(1).addToBody(2, false).addToBody(3, true).addToBody(4, true).endRule()
			.startRule(CHOICERULE).addHead(2).addHead(3).endRule();
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT_EQUAL(3u, ctx.numVars());

		builder.writeProgram(str);
		std::string exp = "2 1 2 1 1 2 3 \n3 2 2 3 0 0 \n0\n1 a\n2 b\n3 c\n0\nB+\n0\nB-\n0\n1\n";
		CPPUNIT_ASSERT_EQUAL(exp, str.str());		
	}

	void testAddWeightConstraint() {
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "d")
			// a :- 2 [not b=1, c=2, d=2 ]
			.startRule(WEIGHTRULE).setBound(2).addHead(1).addToBody(2, false, 1).addToBody(3, true, 2).addToBody(4, true, 2).endRule()
			.startRule(CHOICERULE).addHead(2).addHead(3).endRule();
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT_EQUAL(3u, ctx.numVars());

		builder.writeProgram(str);
		std::string exp = "5 1 2 2 1 2 3 1 2 \n3 2 2 3 0 0 \n0\n1 a\n2 b\n3 c\n0\nB+\n0\nB-\n0\n1\n";
		CPPUNIT_ASSERT_EQUAL(exp, str.str());		
	}
	void testAddMinimizeConstraint() {
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule(BASICRULE).addHead(1).addToBody(2, false).endRule()
			.startRule(BASICRULE).addHead(2).addToBody(1, false).endRule()
			.startRule(OPTIMIZERULE).addToBody(1, true).endRule()
			.startRule(OPTIMIZERULE).addToBody(2, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		MinimizeBuilder minBuilder;
		builder.addMinimize(minBuilder);
		MinimizeConstraint* c1 = minBuilder.buildAndAttach(ctx);
		SharedContext ctx2;
		CPPUNIT_ASSERT_EQUAL(true, builder.cloneProgram(ctx2));
		MinimizeConstraint* c2 = minBuilder.buildAndAttach(ctx2);

		CPPUNIT_ASSERT(c1 != 0 && c2 != 0 && c1 != c2);
		c1->destroy(ctx.master(), true);
		c2->destroy(ctx2.master(), true);
		builder.writeProgram(str);
		std::stringstream exp; 
		exp
			<< "6 0 1 0 1 1 \n"
			<< "6 0 1 0 2 1 \n"
			<< "1 1 1 1 2 \n"
			<< "1 2 1 1 1 \n"
			<< "0\n1 a\n2 b\n0\nB+\n0\nB-\n0\n1\n";
		CPPUNIT_ASSERT_EQUAL(exp.str(), str.str());
	}

	void testNonTight() {
		// p :- q.
		// q :- p.
		// p :- not a.
		// q :- not a.
		// a :- not p.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "p").setAtomName(3, "q")
			.startRule().addHead(2).addToBody(3, true).endRule()
			.startRule().addHead(3).addToBody(2, true).endRule()
			.startRule().addHead(2).addToBody(1, false).endRule()
			.startRule().addHead(3).addToBody(1, false).endRule()
			.startRule().addHead(1).addToBody(2, false).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT( builder.stats.sccs != 0 );
	}

	void testIgnoreCondFactsInLoops() {
		// {a}.
		// b :- a.
		// a :- b.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule(CHOICERULE).addHead(1).endRule()
			.startRule().addHead(2).addToBody(1, true).endRule()
			.startRule().addHead(1).addToBody(2, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT( builder.stats.sccs == 0);
	}

	void testCrEqBug() {
		// a.
		// {b}.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule().addHead(1).endRule()
			.startRule(CHOICERULE).addHead(2).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL(1u, ctx.numVars());
		CPPUNIT_ASSERT_EQUAL(value_free, ctx.master()->value(ctx.symTab()[2].lit.var()));
	}

	void testEqOverChoiceRule() {
		// {a}.
		// b :- a.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule(CHOICERULE).addHead(1).endRule()
			.startRule().addHead(2).addToBody(1, true).endRule()	
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT_EQUAL(1u, ctx.numVars());
		builder.writeProgram(str);
		std::stringstream exp; 
		exp
			<< "3 1 1 0 0 \n"
			<< "1 2 1 0 1 \n"
			<< "0\n1 a\n2 b\n0\nB+\n0\nB-\n0\n1\n";
		CPPUNIT_ASSERT_EQUAL(exp.str(), str.str());
	}

	void testEqOverComp() {
		// x1 :- not x2.
		// x1 :- x2.
		// x2 :- not x3.
		// x3 :- not x1.
		builder.startProgram(ctx)
			.setAtomName(1, "x1").setAtomName(2, "x2").setAtomName(3, "x3")
			.startRule().addHead(1).addToBody(2, false).endRule()
			.startRule().addHead(1).addToBody(2, true).endRule()
			.startRule().addHead(2).addToBody(3, false).endRule()
			.startRule().addHead(3).addToBody(1, false).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT_EQUAL(ctx.symTab()[1].lit, ctx.symTab()[2].lit);
		CPPUNIT_ASSERT(ctx.master()->numFreeVars() == 0 && ctx.master()->isTrue(ctx.symTab()[1].lit));
	}

	void testEqOverBodiesOfDiffType() {
		builder.startProgram(ctx)
			.setAtomName(1, "z").setAtomName(2, "y").setAtomName(3, "x").setAtomName(4, "t")
			.startRule(CHOICERULE).addHead(1).addHead(2).endRule() // {z,y}
			.startRule(CONSTRAINTRULE,2).addHead(4).addToBody(1,true).addToBody(2,true).addToBody(3,true).endRule()
			.startRule().addHead(3).addToBody(4,true).endRule()
			.setCompute(2, false)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT(3u >= ctx.numVars());
		CPPUNIT_ASSERT(ctx.master()->isFalse(ctx.symTab()[2].lit));
		CPPUNIT_ASSERT(ctx.master()->isFalse(ctx.symTab()[3].lit));
		CPPUNIT_ASSERT(ctx.master()->isFalse(ctx.symTab()[4].lit));
	}


	void testNoBodyUnification() {
		// {x, y, z}.
		// p :- x, s.
		// p :- y.
		// q :- not p.
		// r :- not q.
		// s :- p.
		// s :- z. 
		builder.startProgram(ctx)
			.setAtomName(1, "x").setAtomName(2, "y").setAtomName(3, "z")
			.setAtomName(4, "p").setAtomName(5, "q").setAtomName(6, "r").setAtomName(7, "s")
			.startRule(CHOICERULE).addHead(1).addHead(2).addHead(3).endRule()
			.startRule().addHead(4).addToBody(1, true).addToBody(7,true).endRule()
			.startRule().addHead(4).addToBody(2, true).endRule()
			.startRule().addHead(5).addToBody(4, false).endRule()
			.startRule().addHead(6).addToBody(5, false).endRule()
			.startRule().addHead(7).addToBody(4, true).endRule()
			.startRule().addHead(7).addToBody(3, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		DefaultUnfoundedCheck* ufsCheck = new DefaultUnfoundedCheck();
		ufsCheck->attachTo(*ctx.master(), builder.dependencyGraph());
		CPPUNIT_ASSERT_EQUAL(true, ctx.endInit());
		ctx.master()->assume(~ctx.symTab()[2].lit);	// ~y
		ctx.master()->propagate();
		ctx.master()->assume(~ctx.symTab()[3].lit);	// ~z
		ctx.master()->propagate();
		CPPUNIT_ASSERT_EQUAL(true, ctx.master()->isFalse(ctx.symTab()[7].lit));
	}

	void testNoEqAtomReplacement() {
		// {x, y}.
		// p :- x.
		// p :- y.
		// q :- not p.
		// r :- not q.
		builder.startProgram(ctx)
			.setAtomName(1, "x").setAtomName(2, "y").setAtomName(3, "p")
			.setAtomName(4, "q").setAtomName(5, "r")
			.startRule(CHOICERULE).addHead(1).addHead(2).endRule()
			.startRule().addHead(3).addToBody(1, true).endRule()
			.startRule().addHead(3).addToBody(2, true).endRule()
			.startRule().addHead(4).addToBody(3, false).endRule()
			.startRule().addHead(5).addToBody(4, false).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		ctx.master()->assume(~ctx.symTab()[1].lit);	// ~x
		ctx.master()->propagate();
		ctx.master()->assume(~ctx.symTab()[2].lit);	// ~y
		ctx.master()->propagate();
		CPPUNIT_ASSERT_EQUAL(true, ctx.master()->isFalse(ctx.symTab()[3].lit));
	}

	void testAllBodiesSameLit() {
		// {z}.
		// r :- not z.
		// q :- not r.
		// s :- r.
		// s :- not q.
		// r :- s.
		builder.startProgram(ctx)
			.setAtomName(1, "z").setAtomName(2, "r").setAtomName(3, "q").setAtomName(4, "s")
			.startRule(CHOICERULE).addHead(1).endRule()
			.startRule().addHead(2).addToBody(1, false).endRule()
			.startRule().addHead(3).addToBody(2, false).endRule()
			.startRule().addHead(4).addToBody(2, true).endRule()
			.startRule().addHead(4).addToBody(3, false).endRule()
			.startRule().addHead(2).addToBody(4, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL(ctx.symTab()[2].lit, ctx.symTab()[4].lit);
		CPPUNIT_ASSERT(ctx.symTab()[1].lit != ctx.symTab()[3].lit);
		ctx.master()->assume(ctx.symTab()[1].lit) && ctx.master()->propagate();
		CPPUNIT_ASSERT(ctx.master()->value(ctx.symTab()[3].lit.var()) == value_free);
		ctx.master()->assume(~ctx.symTab()[3].lit) && ctx.master()->propagate();
		CPPUNIT_ASSERT(ctx.master()->numFreeVars() == 0 && ctx.master()->isTrue(ctx.symTab()[2].lit));
	}
	
	void testCompLit() {
		// {y}.
		// a :- not x.
		// x :- not a.
		// b :- a, x.
		// c :- a, y, not x
		// -> a == ~x -> {a,x} = F -> {a, not x} = {a, y}
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "x").setAtomName(4, "y").setAtomName(5, "c")
			.startRule(CHOICERULE).addHead(4).endRule()
			.startRule().addHead(1).addToBody(3, false).endRule()
			.startRule().addHead(3).addToBody(1, false).endRule()
			.startRule().addHead(2).addToBody(1, true).addToBody(3, true).endRule()
			.startRule().addHead(5).addToBody(1, true).addToBody(4, true).addToBody(3, false).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL(3u, ctx.numVars());
		CPPUNIT_ASSERT(ctx.master()->isFalse(ctx.symTab()[2].lit));
	}

	void testFunnySelfblockerOverEqByTwo() {
		// {x,y,z}.
		// q :- x, y.
		// d :- x, y.
		// c :- y, z.
		// a :- y, z.
		// c :- q, not c.
		// r :- d, not a.
		// s :- x, r.
		// -> q == d, c == a -> {d, not a} == {q, not c} -> F
		// -> r == s are both unsupported!
		builder.startProgram(ctx)
			.setAtomName(1, "x").setAtomName(2, "y").setAtomName(3, "z").setAtomName(4, "q")
			.setAtomName(5, "d").setAtomName(6, "c").setAtomName(7, "a").setAtomName(8, "r").setAtomName(9, "s")
			.startRule(CHOICERULE).addHead(1).addHead(2).addHead(3).endRule()
			.startRule().addHead(4).addToBody(1, true).addToBody(2,true).endRule()
			.startRule().addHead(5).addToBody(1, true).addToBody(2,true).endRule()
			.startRule().addHead(6).addToBody(2, true).addToBody(3,true).endRule()
			.startRule().addHead(7).addToBody(2, true).addToBody(3,true).endRule()
			.startRule().addHead(6).addToBody(4, true).addToBody(6, false).endRule()
			.startRule().addHead(8).addToBody(5, true).addToBody(7, false).endRule()
			.startRule().addHead(9).addToBody(1, true).addToBody(8, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT(ctx.master()->isFalse(ctx.symTab()[8].lit));
		CPPUNIT_ASSERT(ctx.master()->isFalse(ctx.symTab()[9].lit));
	}
	
	void testRemoveKnownAtomFromWeightRule() {
		// {q, r}.
		// a.
		// x :- 5 [a = 2, not b = 2, q = 1, r = 1].
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "q").setAtomName(4, "r").setAtomName(5, "x")
			.startRule(CHOICERULE).addHead(3).addHead(4).endRule()
			.startRule().addHead(1).endRule()
			.startRule(WEIGHTRULE).addHead(5).setBound(5).addToBody(1, true,2).addToBody(2, false,2).addToBody(3, true).addToBody(4, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		builder.writeProgram(str);
		// {q, r}.
		// a. 
		// x :- 1 [ q=1, r=1 ].
		std::stringstream exp; 
		exp
			<< "1 1 0 0 \n"
			<< "3 2 3 4 0 0 \n"
			<< "5 5 1 2 0 3 4 1 1 \n"
			<< "0\n1 a\n3 q\n4 r\n5 x\n0\nB+\n1\n0\nB-\n0\n1\n";
		CPPUNIT_ASSERT_EQUAL(exp.str(), str.str());
	}

	void testMergeEquivalentAtomsInConstraintRule() {
		// {a, c}.
		// a :- b.
		// b :- a.
		// x :-  2 {a, b, c}.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "x")
			.startRule(CHOICERULE).addHead(1).addHead(3).endRule()
			.startRule().addHead(1).addToBody(2, true).endRule()
			.startRule().addHead(2).addToBody(1, true).endRule()
			.startRule(CONSTRAINTRULE).addHead(4).setBound(2).addToBody(1, true).addToBody(2, true).addToBody(3, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		builder.writeProgram(str);
		std::string x = str.str();
		// x :-  2 [a=2, c].
		CPPUNIT_ASSERT(x.find("5 4 2 2 0 1 3 2 1") != std::string::npos);
	}

	void testMergeEquivalentAtomsInWeightRule() {
		// {a, c, d}.
		// a :- b.
		// b :- a.
		// x :-  3 [a = 1, c = 4, b = 2, d=1].
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "x").setAtomName(5, "d")
			.startRule(CHOICERULE).addHead(1).addHead(3).addHead(5).endRule()
			.startRule().addHead(1).addToBody(2, true).endRule()
			.startRule().addHead(2).addToBody(1, true).endRule()
			.startRule(WEIGHTRULE).addHead(4).setBound(3).addToBody(1, true,1).addToBody(3, true,4).addToBody(2, true,2).addToBody(5, true, 1).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		builder.writeProgram(str);
		std::string x = str.str();
		// x :-  3 [a=3, c=3,d=1].
		CPPUNIT_ASSERT(x.find("5 4 3 3 0 1 3 5 3 3 1") != std::string::npos);
	}


	void testBothLitsInConstraintRule() {
		// {a}.
		// b :- a.
		// c :- b.
		// x :-  1 {a, b, not b, not c}.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "x")
			.startRule(CHOICERULE).addHead(1).endRule()
			.startRule().addHead(2).addToBody(1, true).endRule()
			.startRule().addHead(3).addToBody(2, true).endRule()
			.startRule(CONSTRAINTRULE).addHead(4).setBound(1).addToBody(1, true).addToBody(2, false).addToBody(2, true,2).addToBody(3,false).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		builder.writeProgram(str);
		std::string x = str.str();
		// x :-  1 [a=2, not a=2].
		CPPUNIT_ASSERT(x.find("5 4 1 2 1 1 1 2 2") != std::string::npos);
	}

	void testBothLitsInWeightRule() {
		// {a, d}.
		// b :- a.
		// c :- b.
		// x :-  3 [a=3, not b=1, not c=3, d=2].
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "x").setAtomName(5, "d")
			.startRule(CHOICERULE).addHead(1).addHead(5).endRule()
			.startRule().addHead(2).addToBody(1, true).endRule()
			.startRule().addHead(3).addToBody(2, true).endRule()
			.startRule(WEIGHTRULE).addHead(4).setBound(3).addToBody(1, true,3).addToBody(2, false,1).addToBody(3,false,3).addToBody(5, true, 2).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		builder.writeProgram(str);
		std::string x = str.str();
		// x :-  3 [a=3, not a=4, d=2].
		CPPUNIT_ASSERT(x.find("5 4 3 3 1 1 1 5 4 3 2") != std::string::npos);
	}

	void testWeightlessAtomsInWeightRule() {
		// {a, b, c}.
		// x :-  1 [a=1, b=1, c=0].
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "x")
			.startRule(CHOICERULE).addHead(1).addHead(2).addHead(3).endRule()
			.startRule(WEIGHTRULE).addHead(4).setBound(1).addToBody(1, true,1).addToBody(2, true,1).addToBody(3,true,0).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		builder.writeProgram(str);
		std::string x = str.str();
		// x :-  1 {a, b}.
		CPPUNIT_ASSERT(x.find("2 4 2 0 1 1 2 ") != std::string::npos);
	}

	void testSimplifyToNormal() {
		// {a}.
		// b :-  2 [a=2,not c=1].
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c")
			.startRule(CHOICERULE).addHead(1).endRule()
			.startRule(WEIGHTRULE).addHead(2).setBound(2).addToBody(1, true,2).addToBody(3, false,1).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		builder.writeProgram(str);
		std::string x = str.str();
		// b :-  a.
		CPPUNIT_ASSERT(x.find("1 2 1 0 1 ") != std::string::npos);
	}

	void testSimplifyToCardBug() {
		// x1.
		// x2.
		// t :- 168 [not x1 = 576, not x2=381].
		// compute { not t }.
		builder.startProgram(ctx)
			.setAtomName(1, "x1").setAtomName(2, "x2").setAtomName(3, "t")
			.startRule().addHead(1).addHead(2).endRule()
			.startRule(WEIGHTRULE).addHead(3).setBound(168).addToBody(1,false,576).addToBody(2,false,381).endRule()
			.setCompute(3, false)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT(ctx.master()->numFreeVars() == 0);
	}

	void testSimplifyCompBug() {
		// x1 :- not x2.
		// x1 :- x2.
		// x2 :- not x3.
		// x3 :- not x1.
		// a. b. f.
		// x4 :- a, b, x2, e, f.
		builder.startProgram(ctx, ProgramBuilder::EqOptions().iterations(1))
			.setAtomName(1, "x1").setAtomName(2, "x2").setAtomName(3, "x3")
			.setAtomName(4, "a").setAtomName(5, "b").setAtomName(6, "e").setAtomName(7, "f")
			.startRule().addHead(1).addToBody(2, false).endRule()
			.startRule().addHead(1).addToBody(2, true).endRule()
			.startRule().addHead(2).addToBody(3, false).endRule()
			.startRule().addHead(3).addToBody(1, false).endRule()
			.startRule().addHead(4).addHead(5).addHead(7).endRule()
			.startRule(CHOICERULE).addHead(6).endRule()
			.startRule().addHead(8).addToBody(4, true).addToBody(5, true).addToBody(2, true).addToBody(6, true).addToBody(7, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		PrgAtomNode* x = builder.getAtom(8);
		PrgBodyNode* B = builder.getBody(x->preds[0].node());
		CPPUNIT_ASSERT(B->size() == 2 && B->posSize() == 1 && B->negSize() == 1);
		CPPUNIT_ASSERT(B->pos(0) == 6);
		CPPUNIT_ASSERT(B->neg(0) == 3);
	}

	void testBPWeight() {
		// {a, b, c, d}.
		// x :-  2 [a=1, b=2, not c=2, d=1].
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "d").setAtomName(5, "x")
			.startRule(CHOICERULE).addHead(1).addHead(2).addHead(3).addHead(4).endRule()
			.startRule(WEIGHTRULE).addHead(5).setBound(2).addToBody(1, true,1).addToBody(2, true,2).addToBody(3,false,2).addToBody(4, true, 1).endRule()
			.setCompute(5, false)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT_EQUAL(true, ctx.master()->isFalse(ctx.symTab()[2].lit));
		CPPUNIT_ASSERT_EQUAL(true, ctx.master()->isTrue(ctx.symTab()[3].lit));
	}

	void testExtLitsAreFrozen() {
		// {a, b, c, d, e, f, g}.
		// x :- 1 {b, c}.
		// y :- 2 [c=1, d=2, e=1].
		// minimize {f, g}.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c")
			.setAtomName(4, "d").setAtomName(5, "e").setAtomName(6, "f")
			.setAtomName(7, "g").setAtomName(8, "x").setAtomName(9, "y")
			.startRule(CHOICERULE).addHead(1).addHead(2).addHead(3).addHead(4).addHead(5).addHead(6).addHead(7).endRule()
			.startRule(CONSTRAINTRULE,1).addHead(8).addToBody(2, true).addToBody(3,true).endRule()
			.startRule(WEIGHTRULE,2).addHead(9).addToBody(3, true,1).addToBody(4,true,2).addToBody(5, true,1).endRule()
			.startRule(OPTIMIZERULE).addToBody(6,true).addToBody(7,true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT_EQUAL(false, ctx.frozen(ctx.symTab()[1].lit.var()));
		CPPUNIT_ASSERT_EQUAL(true, ctx.frozen(ctx.symTab()[2].lit.var()));
		CPPUNIT_ASSERT_EQUAL(true, ctx.frozen(ctx.symTab()[3].lit.var()));
		CPPUNIT_ASSERT_EQUAL(true, ctx.frozen(ctx.symTab()[4].lit.var()));
		CPPUNIT_ASSERT_EQUAL(true, ctx.frozen(ctx.symTab()[5].lit.var()));
		CPPUNIT_ASSERT_EQUAL(true, ctx.frozen(ctx.symTab()[8].lit.var()));
		CPPUNIT_ASSERT_EQUAL(true, ctx.frozen(ctx.symTab()[9].lit.var()));

		// minimize lits only frozen if constraint is actually used
		CPPUNIT_ASSERT_EQUAL(false, ctx.frozen(ctx.symTab()[6].lit.var()));
		CPPUNIT_ASSERT_EQUAL(false, ctx.frozen(ctx.symTab()[7].lit.var()));
		MinimizeBuilder min;
		builder.addMinimize(min);
		min.build(ctx)->destroy();
		CPPUNIT_ASSERT_EQUAL(true, ctx.frozen(ctx.symTab()[6].lit.var()));
		CPPUNIT_ASSERT_EQUAL(true, ctx.frozen(ctx.symTab()[7].lit.var()));
	}

	void writeIntegrityConstraint() {
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "x")
			.startRule(CHOICERULE).addHead(1).addHead(2).addHead(3).endRule()
			.startRule(BASICRULE).addHead(1).addToBody(3, true).addToBody(2, false).endRule()
			.startRule(BASICRULE).addHead(2).addToBody(3, true).addToBody(2, false).endRule();
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		
		builder.writeProgram(str);
		
		// falseAtom :- x, not b.
		CPPUNIT_ASSERT(str.str().find("1 4 2 1 2 3") != std::string::npos);
		// compute {not falseAtom}.
		CPPUNIT_ASSERT(str.str().find("B-\n4") != std::string::npos);
	}

	void testSimpleIncremental() {
		builder.startProgram(ctx);
		// I1: 
		// a :- not b.
		// b :- not a.
		builder.updateProgram()
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule().addHead(1).addToBody(2, false).endRule()
			.startRule().addHead(2).addToBody(1, false).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT(ctx.numVars() == 1);
		CPPUNIT_ASSERT(ctx.symTab()[1].lit == ~ctx.symTab()[2].lit);
	
		// I2: 
		// c :- a, not d.
		// d :- b, not c.
		// x :- d.
		builder.updateProgram()
			.setAtomName(3, "c").setAtomName(4, "d").setAtomName(5, "x")
			.startRule().addHead(3).addToBody(1, true).addToBody(4, false).endRule()
			.startRule().addHead(4).addToBody(2, true).addToBody(3, false).endRule()
			.startRule().addHead(5).addToBody(4, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT(ctx.numVars() == 3);
		CPPUNIT_ASSERT(ctx.symTab()[1].lit == ~ctx.symTab()[2].lit);
		CPPUNIT_ASSERT(ctx.symTab()[5].lit == ctx.symTab()[4].lit);
	}

	void testIncrementalFreeze() {
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noEq());
		// I1:
		// {y}.
		// a :- not x.
		// b :- a, y.
		// freeze(x)
		builder.updateProgram()
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "x").setAtomName(4, "y")
			.startRule(CHOICERULE).addHead(4).endRule()
			.startRule().addHead(1).addToBody(3, false).endRule()
			.startRule().addHead(2).addToBody(1, true).addToBody(4, true).endRule()
			.freeze(3)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT(ctx.symTab()[3].lit != negLit(0));
		Solver& solver = *ctx.master();
		solver.assume(ctx.symTab()[3].lit);
		solver.propagate();
		CPPUNIT_ASSERT(ctx.master()->isFalse(ctx.symTab()[2].lit));
		solver.undoUntil(0);
		// I2: 
		// {z}.
		// x :- z.
		// unfreeze(x)
		builder.updateProgram()
			.setAtomName(5, "z")
			.startRule(CHOICERULE).addHead(5).endRule()
			.startRule().addHead(3).addToBody(5, true).endRule()
			.unfreeze(3)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		solver.assume(ctx.symTab()[5].lit);
		solver.propagate();
		CPPUNIT_ASSERT(ctx.master()->isFalse(ctx.symTab()[2].lit));
		solver.undoUntil(0);
		solver.assume(~ctx.symTab()[5].lit);	// ~z
		solver.propagate();
		CPPUNIT_ASSERT(ctx.master()->isFalse(ctx.symTab()[3].lit));
		solver.assume(ctx.symTab()[4].lit);	// y
		solver.propagate();
		CPPUNIT_ASSERT(ctx.master()->isTrue(ctx.symTab()[2].lit));
	}

	void testIncrementalKeepFrozen() {
		builder.startProgram(ctx);
		// I0:
		// freeze{x}.
		builder.updateProgram()
			.setAtomName(1, "x")
			.freeze(1)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		PrgAtomNode* x = builder.getAtom(1);
		Literal xLit   = x->literal();
		// I1:
		builder.updateProgram()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT(x->literal() == xLit);
		CPPUNIT_ASSERT(x->frozen());
	}
	void testIncrementalUnfreezeUnsupp() {
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noEq());
		// I1:
		// a :- not x.
		// freeze(x)
		builder.updateProgram()
			.setAtomName(1, "a").setAtomName(2, "x")
			.startRule().addHead(1).addToBody(2, false).endRule()
			.freeze(2)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		DefaultUnfoundedCheck* ufsCheck = new DefaultUnfoundedCheck();
		ufsCheck->attachTo(*ctx.master(), builder.dependencyGraph());
		ctx.endInit();;
		// I2: 
		// x :- y.
		// y :- x.
		// -> unfreeze(x)
		builder.updateProgram()
			.setAtomName(3, "y")
			.startRule().addHead(3).addToBody(2, true).endRule()
			.startRule().addHead(2).addToBody(3, true).endRule()
			.unfreeze(2)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		ctx.endInit();;
		CPPUNIT_ASSERT(ctx.master()->isTrue(ctx.symTab()[1].lit));
		CPPUNIT_ASSERT(ctx.master()->isFalse(ctx.symTab()[2].lit));
		CPPUNIT_ASSERT(ctx.master()->isFalse(ctx.symTab()[3].lit));
	}

	void testIncrementalUnfreezeCompute() {
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noEq());
		// I1:
		// {z}.
		// a :- x, y.
		// x :- z.
		// freeze(y)
		builder.updateProgram()
			.setAtomName(1, "a").setAtomName(2, "x").setAtomName(3, "y").setAtomName(4, "z")
			.startRule(CHOICERULE).addHead(4).endRule()
			.startRule().addHead(1).addToBody(2,true).addToBody(3, true).endRule()
			.startRule().addHead(2).addToBody(4,true).endRule()
			.freeze(3)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL(9u, ctx.numConstraints());
		
		builder.updateProgram();
		builder.unfreeze(3);
		builder.setCompute(3, false);
		
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT_EQUAL(4u, ctx.numConstraints());
	}

	void testIncrementalCompute() {
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noEq());
		// I1: 
		// {a, b}.
		// FALSE :- a, b.
		builder.updateProgram()
			.setAtomName(1, "FALSE").setAtomName(2, "a").setAtomName(3, "b")
			.startRule(CHOICERULE).addHead(2).addHead(3).endRule()
			.startRule().addHead(1).addToBody(2, true).addToBody(3, true).endRule()
			.setCompute(1, false)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		// I2:
		// {c}.
		// FALSE :- a, c.
		builder.updateProgram()
			.setAtomName(4, "c")
			.startRule(CHOICERULE).addHead(4).endRule()
			.startRule().addHead(1).addToBody(2, true).addToBody(4, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		ctx.master()->assume(ctx.symTab()[2].lit);
		ctx.master()->propagate();
		CPPUNIT_ASSERT(ctx.master()->isFalse(ctx.symTab()[3].lit));
		CPPUNIT_ASSERT(ctx.master()->isFalse(ctx.symTab()[4].lit));
	}

	void testIncrementalComputeBackprop() {
		builder.startProgram(ctx, ProgramBuilder::EqOptions().backpropagate());
		// I1: 
		// a :- not b.
		// b :- not a.
		builder.updateProgram()
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule().addHead(1).addToBody(2, false).endRule()
			.startRule().addHead(2).addToBody(1, false).endRule()
			.setCompute(1, true)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		// I2:
		builder.updateProgram()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
	}

	void testIncrementalEq() {
		builder.startProgram(ctx);
		builder.updateProgram()
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3,"x").setAtomName(4, "y")
			.startRule(CHOICERULE).addHead(3).addHead(4).endRule() // {x, y}
			.startRule().addHead(1).addToBody(3, true).endRule() // a :- x.
			.startRule().addHead(1).addToBody(4, true).endRule() // a :- y.
			.startRule().addHead(2).addToBody(1, true).endRule() // b :- a.
		;
		// b == a
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		builder.writeProgram(str);
		CPPUNIT_ASSERT(str.str().find("1 2 1 0 1") != std::string::npos);
		builder.updateProgram()
			.setAtomName(5, "c")
			.startRule().addHead(5).addToBody(1, true).addToBody(2, true).endRule() // c :- a,b --> c :- a
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		builder.writeProgram(str);
		CPPUNIT_ASSERT(str.str().find("1 5 1 0 1") != std::string::npos);
	}

	void testIncrementalEqComplement() {
		builder.startProgram(ctx);
		// I0:
		// a :- not b.
		// b :- not a.
		builder.updateProgram()
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule().addHead(1).addToBody(2, false).endRule()
			.startRule().addHead(2).addToBody(1, false).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		PrgAtomNode* a = builder.getAtom(1);
		PrgAtomNode* b = builder.getAtom(2);
		CPPUNIT_ASSERT(b->literal() == ~a->literal());
		// I1: 
		// c :- a, b.
		builder.updateProgram()
			.setAtomName(3, "c")
			.startRule().addHead(3).addToBody(1, false).addToBody(2, false).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());		
		PrgAtomNode* c = builder.getAtom(3);
		CPPUNIT_ASSERT(c->hasVar() == false);
	}

	void testIncrementalEqUpdateAssigned() {
		builder.startProgram(ctx);
		// I0:
		// freeze{x}.
		builder.updateProgram()
			.setAtomName(1, "x")
			.freeze(1)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		// I1: 
		// x :- y.
		// y :- x.
		// unfreeze{x}.
		builder.updateProgram()
			.setAtomName(2, "y")
			.startRule().addHead(1).addToBody(2, true).endRule()
			.startRule().addHead(2).addToBody(1, true).endRule()
			.unfreeze(1)
		;
		PrgAtomNode* x = builder.getAtom(1);
		x->setValue(value_weak_true);
		builder.endProgram();
		// only weak-true so still relevant in bodies!
		CPPUNIT_ASSERT(x->posDep.size() == 1);
	}

	void testIncrementalUnfreezeUnsuppEq() {
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noScc());
		// I0:
		// freeze{x}.
		builder.updateProgram()
			.setAtomName(1, "x")
			.freeze(1)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		// I1: 
		// {z}.
		// x :- y.
		// y :- x, z.
		// unfreeze{x}.
		builder.updateProgram()
			.setAtomName(2, "y").setAtomName(3, "z")
			.startRule().addHead(1).addToBody(2, true).endRule()
			.startRule().addHead(2).addToBody(1, true).addToBody(3, true).endRule()
			.startRule(CHOICERULE).addHead(3).endRule()
			.unfreeze(1)
		;
		builder.endProgram();
		PrgAtomNode* x = builder.getAtom(1);
		PrgAtomNode* y = builder.getAtom(2);
		CPPUNIT_ASSERT(ctx.master()->isFalse(x->literal()));
		CPPUNIT_ASSERT(ctx.master()->isFalse(y->literal()));
	}

	void testIncrementalUnfreezeEq() {
		builder.startProgram(ctx);
		// I0:
		// freeze{x}.
		builder.updateProgram()
			.setAtomName(1, "x")
			.freeze(1)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		// I1: 
		// {z}.
		// y :- z.
		// x :- y.
		// unfreeze{x}.
		builder.updateProgram()
			.setAtomName(2, "y").setAtomName(3, "z")
			.startRule(CHOICERULE).addHead(3).endRule()
			.startRule().addHead(2).addToBody(3, true).endRule()
			.startRule().addHead(1).addToBody(2, true).endRule()
			.unfreeze(1)
		;
		PrgAtomNode* x = builder.getAtom(1);
		builder.endProgram();
		// body {y} is eq to body {z} 
		CPPUNIT_ASSERT(ctx.master()->value(x->var()) == value_free);
		CPPUNIT_ASSERT(x->preds.size() == 1 && x->preds[0].node() == 1);
	}

	void testComputeTrueBug() {
		// a :- not b.
		// b :- a.
		// a :- y.
		// y :- a.
		// compute{a}.
		builder.startProgram(ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "y")
			.startRule().addHead(1).addToBody(2, false).endRule()
			.startRule().addHead(2).addToBody(1, true).endRule()
			.startRule().addHead(1).addToBody(3, true).endRule()
			.startRule().addHead(3).addToBody(1, true).endRule()
			.setCompute(1, true)
		;
		CPPUNIT_ASSERT_EQUAL(false, builder.endProgram() && ctx.endInit());
	}

	void testIncrementalStats() {
		PreproStats incStats;
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noEq());
		// I1:
		// a :- not b.
		// b :- not a.
		// freeze(c).
		builder.updateProgram()
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c")
			.startRule().addHead(1).addToBody(2, false).endRule()
			.startRule().addHead(2).addToBody(1, false).endRule()
			.freeze(3)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		incStats = builder.stats;
		CPPUNIT_ASSERT_EQUAL(uint32(3), incStats.atoms);
		CPPUNIT_ASSERT_EQUAL(uint32(2), incStats.bodies);
		CPPUNIT_ASSERT_EQUAL(uint32(2), incStats.rules[0]);
		CPPUNIT_ASSERT_EQUAL(uint32(0), incStats.ufsNodes);
		CPPUNIT_ASSERT_EQUAL(uint32(0), incStats.sccs);
		
		// I2:
		// {c, z}
		// x :- not c.
		// x :- y, z.
		// y :- x, z.
		builder.updateProgram()
			.setAtomName(4, "x").setAtomName(5, "y").setAtomName(6, "z")
			.startRule(CHOICERULE).addHead(3).addHead(6).endRule()
			.startRule().addHead(4).addToBody(3, false).endRule()
			.startRule().addHead(4).addToBody(5, true).addToBody(6, true).endRule()
			.startRule().addHead(5).addToBody(4, true).addToBody(6, true).endRule()
			.unfreeze(3)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		incStats.accu(builder.stats);
		CPPUNIT_ASSERT_EQUAL(uint32(6), incStats.atoms);
		CPPUNIT_ASSERT_EQUAL(uint32(6), incStats.bodies);
		CPPUNIT_ASSERT_EQUAL(uint32(6), incStats.rules[0]);
		CPPUNIT_ASSERT_EQUAL(uint32(1), incStats.sccs);
		// I3:
		// a :- x, not r.
		// r :- not a.
		// a :- b.
		// b :- a, not z.
		builder.updateProgram()
			.setAtomName(7, "a").setAtomName(8, "b").setAtomName(9, "r")
			.startRule().addHead(7).addToBody(4, true).addToBody(9, false).endRule()
			.startRule().addHead(9).addToBody(7, false).endRule()
			.startRule().addHead(7).addToBody(8, true).endRule()
			.startRule().addHead(8).addToBody(7, true).addToBody(6, false).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		incStats.accu(builder.stats);
		CPPUNIT_ASSERT_EQUAL(uint32(9), incStats.atoms);
		CPPUNIT_ASSERT_EQUAL(uint32(10), incStats.bodies);
		CPPUNIT_ASSERT_EQUAL(uint32(10), incStats.rules[0]);
		CPPUNIT_ASSERT_EQUAL(uint32(2), incStats.sccs);
	}

	void testIncrementalTransform() {
		builder.setExtendedRuleMode(ProgramBuilder::mode_transform);
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noEq());
		// I1:
		// {a}.
		// --> 
		// a :- not a'
		// a':- not a.
		builder.updateProgram()
			.setAtomName(1, "a")
			.startRule(CHOICERULE).addHead(1).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT(builder.dependencyGraph()->nodes() == 0);
		// I2:
		// b :- a.
		// b :- c.
		// c :- b.
		// NOTE: b must not have the same id as a'
		builder.updateProgram()
			.setAtomName(2, "b").setAtomName(3, "c")
			.startRule().addHead(2).addToBody(1, true).endRule()
			.startRule().addHead(2).addToBody(3, true).endRule()
			.startRule().addHead(3).addToBody(2, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT(builder.dependencyGraph()->nodes() != 0);
	}

	void testBackprop() {
		builder.startProgram(ctx, ProgramBuilder::EqOptions().backpropagate())
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "d")
			.setAtomName(5, "x").setAtomName(6, "y").setAtomName(7, "z").setAtomName(8, "_false")
			.startRule(CHOICERULE).addHead(5).addHead(6).addHead(7).endRule()       // {x,y,z}.
			.startRule().addHead(4).addToBody(5, true).addToBody(1, true).endRule() // d :- x,a
			.startRule().addHead(1).addToBody(6, true).addToBody(4, true).endRule() // a :- y,d
			.startRule().addHead(2).addToBody(5, true).addToBody(7, true).endRule() // b :- x,z
			.startRule().addHead(3).addToBody(6, true).addToBody(7, true).endRule() // c :- y,z
			.startRule().addHead(8).addToBody(5, true).addToBody(4, false).endRule() //  :- x,not d
			.startRule().addHead(8).addToBody(6, true).addToBody(2, false).endRule() //  :- y,not b
			.startRule().addHead(8).addToBody(7, true).addToBody(3, false).endRule() //  :- z,not c
			.setCompute(8, false)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram() && ctx.endInit());
		CPPUNIT_ASSERT(ctx.numVars() == 0);
	}

	void testIncrementalBackpropCompute() {
		builder.startProgram(ctx);
		// I0:
		// a :- x.
		// freeze{x}.
		// compute{a}.
		builder.updateProgram()
			.setAtomName(1, "a").setAtomName(2, "x")
			.startRule().addHead(1).addToBody(2, true).endRule()
			.setCompute(1, true)
			.freeze(2)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT(builder.getEqAtom(1) == 2);
		PrgAtomNode* x = builder.getAtom(2);
		CPPUNIT_ASSERT(x->value() == value_weak_true);
		// I1: 
		// x :- y.
		// y :- x.
		// unfreeze{x}.
		builder.updateProgram()
			.setAtomName(3, "y")
			.startRule().addHead(3).addToBody(2, true).endRule()
			.startRule().addHead(2).addToBody(3, true).endRule()
			.unfreeze(2)
		;
		// UNSAT: no support for x,y
		CPPUNIT_ASSERT_EQUAL(false, builder.endProgram());
	}

	void testIncrementalBackpropSolver() {
		builder.startProgram(ctx);
		// I0:
		// {a}.
		// freeze{x}.
		builder.updateProgram()
			.setAtomName(1, "a").setAtomName(2, "x")
			.startRule(CHOICERULE).addHead(1).endRule()
			.setCompute(1, true)
			.setCompute(2, true)
			.freeze(2)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		PrgAtomNode* a = builder.getAtom(1);
		PrgAtomNode* x = builder.getAtom(2);
		CPPUNIT_ASSERT(a->value() == value_true);
		CPPUNIT_ASSERT(x->value() == value_weak_true);
		// I1: 
		builder.updateProgram()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT(a->value() == value_true);
		CPPUNIT_ASSERT(x->value() == value_weak_true);
	}

	void testIncrementalFreezeUnfreeze() {
		builder.startProgram(ctx);
		// I0:
		// {a}.
		// freeze{x}.
		// unfreeze{x}.
		builder.updateProgram()
			.setAtomName(1, "a").setAtomName(2, "x")
			.startRule(CHOICERULE).addHead(1).endRule()
			.freeze(2)
			.unfreeze(2)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT_EQUAL(negLit(0), ctx.symTab()[2].lit);

		// I1:
		// freeze(y).
		// y :- x.
		builder.updateProgram()
			.setAtomName(3, "y")
			.freeze(3)
			.startRule().addHead(3).addToBody(2, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT_EQUAL(true, ctx.master()->isFalse(ctx.symTab()[2].lit));
	}
	void testIncrementalSymbolUpdate() {
		builder.startProgram(ctx);
		// I0:
		// {a}.
		builder.updateProgram()
			.setAtomName(1, "a")
			.startRule(CHOICERULE).addHead(1).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		// I1:
		// {_unnamed, b}.
		builder.updateProgram()
			.setAtomName(3, "b")
			.startRule(CHOICERULE).addHead(2).addHead(3).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT(ctx.symTab().find(2) == 0);
		CPPUNIT_ASSERT(!isSentinel(ctx.symTab()[3].lit));
	}

	void testIncrementalFreezeDefined() {
		builder.startProgram(ctx);
		// I0:
		// {a}.
		builder.updateProgram()
			.setAtomName(1, "a")
			.startRule(CHOICERULE).addHead(1).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		// I1:
		builder.updateProgram()
			.freeze(1)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT(builder.getAtom(1)->frozen() == false);
	}
	void testIncrementalUnfreezeDefined() {
		builder.startProgram(ctx);
		// I0:
		// {a}.
		builder.updateProgram()
			.setAtomName(1, "a")
			.startRule(CHOICERULE).addHead(1).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		// I1:
		builder.updateProgram()
			.unfreeze(1)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT(!ctx.master()->isFalse(builder.getLiteral(1)));
	}
	void testIncrementalImplicitUnfreeze() {
		builder.startProgram(ctx);
		// I0:
		// freeze(a).
		builder.updateProgram()
			.setAtomName(1, "a")
			.freeze(1)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT(builder.getAtom(1)->frozen() == true);
		CPPUNIT_ASSERT(!ctx.master()->isFalse(builder.getLiteral(1)));
		// I1:
		// {a}.
		builder.updateProgram()
			.startRule(CHOICERULE).addHead(1).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		CPPUNIT_ASSERT(builder.getAtom(1)->frozen() == false);
	}
	void testIncrementalRedefine() {
		builder.startProgram(ctx);
		// I0:
		// {a}.
		builder.updateProgram()
			.setAtomName(1, "a")
			.startRule(CHOICERULE).addHead(1).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		// I1:
		// {a}.
		CPPUNIT_ASSERT_THROW(builder.updateProgram().startRule(CHOICERULE).addHead(1).endRule(), RedefinitionError);
	}
	void testIncrementalGetAssumptions() {
		builder.startProgram(ctx);
		// I0:
		builder.updateProgram()
			.setAtomName(1, "a").setAtomName(2, "b")
			.freeze(1).freeze(2)
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		LitVec a;
		builder.getAssumptions(a);
		CPPUNIT_ASSERT(a.size() == 2 && a[0] == ~builder.getLiteral(1) && a[1] == ~builder.getLiteral(2));
	}
	void testMergeValue() {
		PrgNode lhs, rhs;
		ValueRep ok[15] = {
			value_free, value_free, value_free,
			value_free, value_true, value_true,
			value_free, value_false, value_false,
			value_free, value_weak_true, value_weak_true,
			value_true, value_weak_true, value_true
		};
		ValueRep fail[4] = { value_true, value_false, value_weak_true, value_false };
		for (uint32 x = 0; x != 2; ++x) {
			for (uint32 i = 0; i != 15; i += 3) {
				lhs.clearLiteral(true);
				rhs.clearLiteral(true);
				CPPUNIT_ASSERT(lhs.setValue(ok[i+x]));
				CPPUNIT_ASSERT(rhs.setValue(ok[i+(!x)]));
				CPPUNIT_ASSERT(lhs.mergeValue(&rhs));
				CPPUNIT_ASSERT(lhs.value() == ok[i+2] && rhs.value() == ok[i+2]);
			}
		}
		for (uint32 x = 0; x != 2; ++x) {
			for (uint32 i = 0; i != 4; i+=2) {
				lhs.clearLiteral(true);
				rhs.clearLiteral(true);
				CPPUNIT_ASSERT(lhs.setValue(fail[i+x]));
				CPPUNIT_ASSERT(rhs.setValue(fail[i+(!x)]));
				CPPUNIT_ASSERT(!lhs.mergeValue(&rhs));
			}
		}
	}
private:
	SharedContext ctx;
	ProgramBuilder builder;

	typedef SharedDependencyGraph DG;
	stringstream str;
};
CPPUNIT_TEST_SUITE_REGISTRATION(ProgramBuilderTest);
 } } 
