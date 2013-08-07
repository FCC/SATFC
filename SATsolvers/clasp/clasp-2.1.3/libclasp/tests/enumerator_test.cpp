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
#include <clasp/unfounded_check.h>
#include <clasp/minimize_constraint.h>
#include <clasp/model_enumerators.h>
#include <sstream>
using namespace std;
namespace Clasp { namespace Test {
	
class EnumeratorTest : public CppUnit::TestFixture {
  CPPUNIT_TEST_SUITE(EnumeratorTest);
	CPPUNIT_TEST(testMiniProject);
	CPPUNIT_TEST(testProjectBug);
	CPPUNIT_TEST(testTerminateRemovesWatches);
	CPPUNIT_TEST(testParallelRecord);
	CPPUNIT_TEST(testParallelUpdate);
	CPPUNIT_TEST_SUITE_END();	
public:
	void testMiniProject() {
		SharedContext ctx;
		Solver& solver = *ctx.master();
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noEq().noScc())
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "_x")
			.startRule(CHOICERULE).addHead(1).addHead(2).addHead(3).endRule()
			.startRule().addHead(3).addToBody(1, false).endRule()
			.startRule().addHead(3).addToBody(2, false).endRule()
			.startRule(OPTIMIZERULE).addToBody(3, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		std::auto_ptr<BacktrackEnumerator> e(new BacktrackEnumerator(0));
		e->setEnableProjection(true);
		e->enumerate(0);
		MinimizeBuilder b;
		builder.addMinimize(b);
		ctx.addEnumerator(e.release());
		ctx.enumerator()->setMinimize(b.build(ctx));
		ctx.endInit();
		SymbolTable& index = ctx.symTab();
		solver.assume(index[1].lit);
		solver.propagate();
		solver.assume(index[2].lit);
		solver.propagate();
		solver.assume(index[3].lit);
		solver.propagate();
		CPPUNIT_ASSERT(solver.numVars() == solver.numAssignedVars());
		CPPUNIT_ASSERT_EQUAL(Enumerator::enumerate_continue, ctx.enumerator()->backtrackFromModel(solver));
		CPPUNIT_ASSERT(false == solver.propagate());
		solver.backtrack();
		CPPUNIT_ASSERT(false == solver.propagate() && !solver.resolveConflict());
		ctx.detach(solver);
	}

	void testProjectBug() {
		SharedContext ctx;
		Solver& solver = *ctx.master();
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noEq().noScc())
			.setAtomName(1, "x").setAtomName(2, "y").setAtomName(3, "z").setAtomName(4, "_p").setAtomName(5, "_q").setAtomName(6, "_r")
			.startRule(CHOICERULE).addHead(1).addHead(2).addHead(4).endRule() // {x,y,_p}
			.startRule().addHead(5).addToBody(1, true).addToBody(4, true).endRule() // _q :- x,_p.
			.startRule().addHead(6).addToBody(2, true).addToBody(4, true).endRule() // _r :- y,_p.
			.startRule().addHead(3).addToBody(5, true).addToBody(6, true).endRule() // z :- _q,_r.
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		std::auto_ptr<BacktrackEnumerator> e(new BacktrackEnumerator(7,0));
		e->setEnableProjection(true);
		e->enumerate(0);
		ctx.addEnumerator(e.release());
		ctx.endInit();
		SymbolTable& index = ctx.symTab();
		solver.assume(index[1].lit);
		solver.propagate();
		solver.assume(index[2].lit);
		solver.propagate();
		solver.assume(index[4].lit);
		solver.propagate();
		CPPUNIT_ASSERT(solver.numVars() == solver.numAssignedVars());
		CPPUNIT_ASSERT_EQUAL(Enumerator::enumerate_continue, ctx.enumerator()->backtrackFromModel(solver));

		solver.undoUntil(0);
		uint32 numT = 0;
		if (solver.value(index[1].lit.var()) == value_free) {
			solver.assume(index[1].lit) && solver.propagate();
			++numT;
		}
		else if (solver.isTrue(index[1].lit)) {
			++numT;
		}
		if (solver.value(index[2].lit.var()) == value_free) {
			solver.assume(index[2].lit) && solver.propagate();
			++numT;
		}
		else if (solver.isTrue(index[2].lit)) {
			++numT;
		}
		if (solver.value(index[4].lit.var()) == value_free) {
			solver.assume(index[4].lit) && solver.propagate();
		}
		if (solver.isTrue(index[3].lit)) {
			++numT;
		}
		CPPUNIT_ASSERT(numT < 3);
		ctx.detach(solver);
	}

	void testTerminateRemovesWatches() {
		SharedContext ctx; Solver& solver = *ctx.master();
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noEq().noScc())
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "d")
			.startRule(CHOICERULE).addHead(1).addHead(2).addHead(3).addHead(4).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		ctx.addEnumerator(new RecordEnumerator(0));
		ctx.enumerator()->enumerate(0);
		CPPUNIT_ASSERT_EQUAL(true, ctx.endInit());
		
		SymbolTable& index = ctx.symTab();
		solver.assume(index[1].lit) && solver.propagate();
		solver.assume(index[2].lit) && solver.propagate();
		solver.assume(index[3].lit) && solver.propagate();
		solver.assume(index[4].lit) && solver.propagate();
		CPPUNIT_ASSERT_EQUAL(uint32(0), solver.numFreeVars());
		ctx.enumerator()->backtrackFromModel(solver);
		uint32 numW = solver.numWatches(index[1].lit) + solver.numWatches(index[2].lit) + solver.numWatches(index[3].lit) + solver.numWatches(index[4].lit);
		CPPUNIT_ASSERT(numW > 0);
		ctx.detach(solver);
		numW = solver.numWatches(index[1].lit) + solver.numWatches(index[2].lit) + solver.numWatches(index[3].lit) + solver.numWatches(index[4].lit);
		CPPUNIT_ASSERT(numW == 0);
	}

	void testParallelRecord() {
		SharedContext ctx; Solver& solver = *ctx.master();
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noEq().noScc())
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c").setAtomName(4, "d")
			.startRule(CHOICERULE).addHead(1).addHead(2).addHead(3).addHead(4).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		ctx.setSolvers(2);
		ctx.addEnumerator(new RecordEnumerator(0));
		ctx.enumerator()->enumerate(0);
		ctx.endInit();
		Solver solver2;
		ctx.attach(solver2);
		SymbolTable& index = ctx.symTab();
		solver.assume(index[1].lit) && solver.propagate();
		solver.assume(index[2].lit) && solver.propagate();
		solver.assume(index[3].lit) && solver.propagate();
		solver.assume(index[4].lit) && solver.propagate();
		CPPUNIT_ASSERT_EQUAL(uint32(0), solver.numFreeVars());
		ctx.enumerator()->backtrackFromModel(solver);
		solver.undoUntil(0);
		
		CPPUNIT_ASSERT_EQUAL(true, ctx.enumerator()->update(solver2, false));

		solver2.assume(index[1].lit) && solver2.propagate();
		solver2.assume(index[2].lit) && solver2.propagate();
		solver2.assume(index[3].lit) && solver2.propagate();
		CPPUNIT_ASSERT(solver2.isFalse(index[4].lit) && solver2.propagate());
		CPPUNIT_ASSERT_EQUAL(uint32(0), solver2.numFreeVars());
		ctx.enumerator()->backtrackFromModel(solver2);
		solver.undoUntil(0);

		CPPUNIT_ASSERT_EQUAL(true, ctx.enumerator()->update(solver, false));

		solver.assume(index[1].lit) && solver.propagate();
		solver.assume(index[2].lit) && solver.propagate();
		CPPUNIT_ASSERT(solver.isFalse(index[3].lit));

		ctx.detach(solver2);
		ctx.detach(solver);
		solver2.undoUntil(0);
		ctx.attach(solver2);
		solver2.assume(index[1].lit) && solver2.propagate();
		solver2.assume(index[2].lit) && solver2.propagate();
		solver2.assume(index[3].lit) && solver2.propagate();
		CPPUNIT_ASSERT(solver2.value(index[4].lit.var()) == value_free);
	}

	void testParallelUpdate() {
		SharedContext ctx; Solver& solver = *ctx.master();
		builder.startProgram(ctx, ProgramBuilder::EqOptions().noEq().noScc())
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c")
			.startRule(CHOICERULE).addHead(1).addHead(2).addHead(3).endRule()
			.startRule(OPTIMIZERULE).addToBody(2, true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		MinimizeBuilder minBuilder;
		builder.addMinimize(minBuilder);
		ctx.setSolvers(2);
		ctx.addEnumerator(new RecordEnumerator(0));
		ctx.enumerator()->enumerate(0);
		ctx.enumerator()->setMinimize(minBuilder.build(ctx));
		ctx.endInit();
		Solver solver2;
		ctx.attach(solver2);
		SymbolTable& index = ctx.symTab();

		// a
		solver.assume(index[1].lit);
		solver.pushRootLevel(1);
		solver.propagate();
		// ~a
		solver2.assume(~index[1].lit);
		solver2.pushRootLevel(1);
		solver2.propagate();

		// M1: ~b, c
		solver.assume(~index[2].lit);
		solver.propagate();
		solver.assume(index[3].lit);
		solver.propagate();	
		CPPUNIT_ASSERT_EQUAL(uint32(0), solver.numFreeVars());
		ctx.enumerator()->backtrackFromModel(solver);
		solver.undoUntil(0);
		
		// M2: ~b, ~c 
		solver2.assume(~index[2].lit);
		solver2.propagate();
		solver2.assume(~index[3].lit);
		solver2.propagate();	
		// M2 is NOT VALID!
		CPPUNIT_ASSERT_EQUAL(false, ctx.enumerator()->update(solver2, true));
	}
private:
	ProgramBuilder builder;
	stringstream str;
};
CPPUNIT_TEST_SUITE_REGISTRATION(EnumeratorTest);
 } } 
