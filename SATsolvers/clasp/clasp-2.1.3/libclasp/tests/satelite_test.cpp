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
#include <algorithm>
#include <clasp/satelite.h>
#include <clasp/solver.h>
#include <clasp/clause.h>
#include <clasp/reader.h>
#ifdef _MSC_VER
#pragma warning (disable : 4267) //  conversion from 'size_t' to unsigned int
#pragma once
#endif


namespace Clasp { namespace Test {

class SatEliteTest : public CppUnit::TestFixture {
  CPPUNIT_TEST_SUITE(SatEliteTest);
	
	CPPUNIT_TEST(testDontAddSatClauses);
	CPPUNIT_TEST(testSimpleSubsume);
	CPPUNIT_TEST(testSimpleStrengthen);
	
	CPPUNIT_TEST(testClauseCreatorAddsToPreprocessor);
	
	CPPUNIT_TEST(testDimacs);
	CPPUNIT_TEST(testFreeze);

	CPPUNIT_TEST(testElimPureLits);
	CPPUNIT_TEST(testDontElimPureLits);
	CPPUNIT_TEST(testExtendModel);
	CPPUNIT_TEST_SUITE_END();	
public:
	SatEliteTest(){
		for (int i = 0; i < 10; ++i) {
			ctx.addVar(Var_t::atom_var);
		}
		ctx.startAddConstraints();
		pre.setContext(ctx);
	}
	
	void testDontAddSatClauses() {
		LitVec cl;
		cl.push_back(posLit(1)); cl.push_back(posLit(2));
		pre.addClause(cl);
		ctx.addUnary(posLit(1));
		CPPUNIT_ASSERT_EQUAL(true, pre.preprocess(false));
		CPPUNIT_ASSERT_EQUAL(0u, ctx.numConstraints());
	}

	void testSimpleSubsume() {
		LitVec cl;
		cl.push_back(posLit(1)); cl.push_back(posLit(2));
		pre.addClause(cl);
		cl.push_back(posLit(3));
		pre.addClause(cl);
		CPPUNIT_ASSERT_EQUAL(true, pre.preprocess(false));
		CPPUNIT_ASSERT_EQUAL(0u, ctx.numConstraints());
	}

	void testSimpleStrengthen() {
		LitVec cl;
		cl.push_back(posLit(1)); cl.push_back(posLit(2));
		pre.addClause(cl);
		cl[0] = negLit(1);
		pre.addClause(cl);
		CPPUNIT_ASSERT_EQUAL(true, pre.preprocess(false));
		CPPUNIT_ASSERT_EQUAL(0u, ctx.numConstraints());
	}

	void testClauseCreatorAddsToPreprocessor() {
		SatElite::SatElite* p = new SatElite::SatElite(&ctx);
		ctx.satPrepro.reset(p);
		p->options.elimPure = false;
		ClauseCreator nc(ctx.master());
		nc.start().add(posLit(1)).add(posLit(2)).end();
		CPPUNIT_ASSERT_EQUAL(0u, ctx.numConstraints());
		ctx.endInit();
		CPPUNIT_ASSERT_EQUAL(1u, ctx.numConstraints());
		CPPUNIT_ASSERT_EQUAL(1u, ctx.numBinary());
	}

	void testDimacs() {
		std::stringstream prg;
		SharedContext ctx2; 
		ctx2.satPrepro.reset(new SatElite::SatElite(&ctx2));
		static_cast<SatElite::SatElite*>(ctx2.satPrepro.get())->options.bce = 0;
		
		prg << "c simple test case\n"
				<< "p cnf 2 4\n"
			  << "1 2 2 1 0\n"
				<< "1 2 1 2 0\n"
				<< "-1 -2 -1 0\n"
				<< "-2 -1 -2 0\n";
		CPPUNIT_ASSERT(parseDimacs(prg, ctx2, PureLitMode_t::assert_pure_yes));
		CPPUNIT_ASSERT(ctx2.numVars() == 2 && ctx2.master()->numAssignedVars() == 0);
		CPPUNIT_ASSERT(ctx2.endInit());
		CPPUNIT_ASSERT(ctx2.eliminated(2));
	}

	
	void testFreeze() {
		std::stringstream prg;
		SharedContext ctx2; 
		ctx2.satPrepro.reset(new SatElite::SatElite(&ctx2));
		
		prg << "c simple test case\n"
				<< "p cnf 2 2\n"
			  << "1 2 0\n"
				<< "-1 -2 0\n";
		CPPUNIT_ASSERT(parseDimacs(prg, ctx2, PureLitMode_t::assert_pure_yes));
		ctx2.setFrozen(1, true);
		ctx2.setFrozen(2, true);
		CPPUNIT_ASSERT(ctx2.numVars() == 2);
		CPPUNIT_ASSERT(ctx2.endInit());
		CPPUNIT_ASSERT(ctx2.numConstraints() == 2);
		CPPUNIT_ASSERT(ctx2.numBinary() == 2);
	}

	void testElimPureLits() {
		LitVec cl;
		cl.push_back(posLit(1)); cl.push_back(posLit(2));
		pre.addClause(cl);
		pre.options.elimPure	= true;
		pre.options.bce       = 0;
		pre.preprocess(false);
		CPPUNIT_ASSERT(0u == ctx.numConstraints());
		CPPUNIT_ASSERT(ctx.eliminated(1) == true);
	}
	void testDontElimPureLits() {
		LitVec cl;
		cl.push_back(posLit(1)); cl.push_back(posLit(2));
		pre.addClause(cl);
		pre.options.elimPure	= false;
		pre.preprocess(false);
		CPPUNIT_ASSERT(1u == ctx.numConstraints());
		CPPUNIT_ASSERT(ctx.eliminated(1) == false);
	}

	void testExtendModel() {
		SharedContext ctx2; 
		ctx2.satPrepro.reset(new SatElite::SatElite(&ctx2));
		static_cast<SatElite::SatElite*>(ctx2.satPrepro.get())->options.bce = 0;
		static_cast<SatElite::SatElite*>(ctx2.satPrepro.get())->options.elimPure = false;
		ctx2.addVar(Var_t::atom_var);
		ctx2.addVar(Var_t::atom_var);
		ctx2.addVar(Var_t::atom_var);
		ctx2.startAddConstraints();
		ClauseCreator nc(ctx2.master());
		nc.start().add(negLit(1)).add(posLit(2)).end();
		nc.start().add(posLit(1)).add(negLit(2)).end();
		nc.start().add(posLit(2)).add(negLit(3)).end();
		ctx2.endInit();
		CPPUNIT_ASSERT(1u == ctx2.numConstraints());
		CPPUNIT_ASSERT(ctx2.eliminated(1) == true && ctx2.numEliminatedVars() == 1);
		// Eliminated vars are initially true
		CPPUNIT_ASSERT_EQUAL(value_true, ctx2.master()->value(1));
		ctx2.master()->assume(negLit(2)) && ctx2.master()->propagate();
		ctx2.master()->search(-1, -1, 1.0);
		// negLit(2) -> negLit(1) -> 1 == F
		CPPUNIT_ASSERT_EQUAL(value_false, ctx2.master()->value(1));
	}
private:
	SharedContext       ctx;
	SatElite::SatElite	pre;
};
CPPUNIT_TEST_SUITE_REGISTRATION(SatEliteTest);
} } 
