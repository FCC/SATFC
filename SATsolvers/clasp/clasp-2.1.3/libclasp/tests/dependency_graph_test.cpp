// 
// Copyright (c) 2009, Benjamin Kaufmann
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
#include <clasp/dependency_graph.h>
#include <clasp/solver.h>
namespace Clasp { namespace Test {
	class DependencyGraphTest : public CppUnit::TestFixture {
	CPPUNIT_TEST_SUITE(DependencyGraphTest);
	CPPUNIT_TEST(testTightProgram);
	CPPUNIT_TEST(testInitOrder);
	CPPUNIT_TEST(testProgramWithLoops);
	CPPUNIT_TEST(testCloneProgramWithLoops);
	CPPUNIT_TEST(testWithSimpleCardinalityConstraint);
	CPPUNIT_TEST(testWithSimpleWeightConstraint);
	CPPUNIT_TEST_SUITE_END(); 
public:
	DependencyGraphTest() {
	}
	void setUp() {
		ctx   = new SharedContext();
	}
	void tearDown() {
		delete ctx;
	}
	void testTightProgram() { 
		builder.startProgram(*ctx)
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule().addHead(1).addToBody(2, false).endRule()
		.endProgram();
		CPPUNIT_ASSERT_EQUAL(true, builder.stats.sccs == 0);
		CPPUNIT_ASSERT_EQUAL(uint32(0), builder.dependencyGraph()->nodes());
	}
	
	void testInitOrder() {
		builder.startProgram(*ctx, ProgramBuilder::EqOptions().noEq())
			.setAtomName(1,"a").setAtomName(2,"b").setAtomName(3,"x").setAtomName(4,"y")
			.startRule().addHead(4).addToBody(3, true).endRule()  // y :- x.
			.startRule().addHead(3).addToBody(4, true).endRule()  // x :- y.
			.startRule().addHead(2).addToBody(3, true).endRule()  // b :- x.
			.startRule().addHead(2).addToBody(1, true).endRule()  // b :- a.
			.startRule().addHead(1).addToBody(2, true).endRule()  // a :- b.
			.startRule().addHead(3).addToBody(1, false).endRule() // x :- not a.
		.endProgram();
		
		CPPUNIT_ASSERT_EQUAL(true, builder.stats.sccs == 2);
		
		DG* graph = builder.dependencyGraph();

		CPPUNIT_ASSERT_EQUAL(uint32(9), graph->nodes());
		
		SymbolTable& index = ctx->symTab();
		DG::NodePair<DG::AtomNode> b = graph->getAtomByLit(index[2].lit);
		DG::NodePair<DG::AtomNode> x = graph->getAtomByLit(index[3].lit);
		CPPUNIT_ASSERT(graph->getBody(b.node->bodies()[0]).node->scc != b.node->scc);
		CPPUNIT_ASSERT(graph->getBody(b.node->bodies()[1]).node->scc == b.node->scc);
		CPPUNIT_ASSERT(b.node->bodies()+2 == b.node->bodies_end());

		CPPUNIT_ASSERT(graph->getBody(x.node->bodies()[0]).node->scc != x.node->scc);
		CPPUNIT_ASSERT(graph->getBody(x.node->bodies()[1]).node->scc == x.node->scc);
		CPPUNIT_ASSERT(x.node->bodies()+2 == x.node->bodies_end());

		const DG::BodyNode& xBody = *graph->getBody(b.node->bodies()[0]).node;
		CPPUNIT_ASSERT(graph->getAtom(xBody.heads()[0]).node->scc == xBody.scc);
		CPPUNIT_ASSERT(graph->getAtom(xBody.heads()[1]).node->scc != xBody.scc);
		CPPUNIT_ASSERT(xBody.heads()+2 == xBody.heads_end());
	}

	void testProgramWithLoops() {
		builder.startProgram(*ctx)
		.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c")
		.setAtomName(4, "d").setAtomName(5, "g").setAtomName(6, "x").setAtomName(7, "y")
		.startRule().addHead(1).addToBody(6, false).endRule() // a :- not x.
		.startRule().addHead(2).addToBody(1, true).endRule()  // b :- a.
		.startRule().addHead(1).addToBody(2, true).addToBody(4, true).endRule() // a :- b, d.
		.startRule().addHead(2).addToBody(5, false).endRule() // b :- not g.
		.startRule().addHead(3).addToBody(6, true).endRule()  // c :- x.
		.startRule().addHead(4).addToBody(3, true).endRule()  // d :- c.
		.startRule().addHead(3).addToBody(4, true).endRule()  // c :- d.
		.startRule().addHead(4).addToBody(5, false).endRule() // d :- not g.
		.startRule().addHead(7).addToBody(5, false).endRule() // y :- not g.
		.startRule().addHead(6).addToBody(7, true).endRule()  // x :- y.
		.startRule().addHead(5).addToBody(7, false).endRule() // g :- not y.
		.endProgram();
		
		SymbolTable& index = ctx->symTab();
		DG*           graph= builder.dependencyGraph();
		CPPUNIT_ASSERT_EQUAL(index[6].lit, index[7].lit);
		CPPUNIT_ASSERT_EQUAL(~index[6].lit, index[5].lit);
		
		CPPUNIT_ASSERT( graph->getAtomByLit(index[1].lit).node->scc == 0 );
		CPPUNIT_ASSERT( graph->getAtomByLit(index[2].lit).node->scc == 0 );
		CPPUNIT_ASSERT( graph->getAtomByLit(index[3].lit).node->scc == 1 );
		CPPUNIT_ASSERT( graph->getAtomByLit(index[4].lit).node->scc == 1 );
		CPPUNIT_ASSERT( graph->getAtomByLit(index[5].lit).node == 0 );
		CPPUNIT_ASSERT( graph->getAtomByLit(index[6].lit).node == 0 );
		CPPUNIT_ASSERT( graph->getAtomByLit(index[7].lit).node == 0 );
		
		CPPUNIT_ASSERT(uint32(10) == graph->nodes());
		// check that lists are partitioned by component number
		DG::NodePair<DG::AtomNode> a = graph->getAtomByLit(index[1].lit);
		CPPUNIT_ASSERT(graph->getBody(a.node->bodies()[0]).node->scc == PrgNode::noScc);
		CPPUNIT_ASSERT(graph->getBody(a.node->bodies()[1]).node->scc == a.node->scc);
		CPPUNIT_ASSERT(a.node->bodies()+2 == a.node->bodies_end());
		CPPUNIT_ASSERT_EQUAL(true, ctx->frozen(a.node->lit.var()));

		const DG::BodyNode& bd = *graph->getBody(a.node->bodies()[1]).node;
		CPPUNIT_ASSERT_EQUAL(true, ctx->frozen(bd.lit.var()));
		CPPUNIT_ASSERT(graph->getAtom(bd.preds()[0]).node->lit == index[2].lit);
		CPPUNIT_ASSERT(bd.preds()[1]== idMax);
		CPPUNIT_ASSERT(bd.heads()[0] == a.id);
		CPPUNIT_ASSERT(bd.heads()+1 == bd.heads_end());
	}

	void testCloneProgramWithLoops() {
		builder.startProgram(*ctx)
		.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c")
		.setAtomName(4, "d").setAtomName(5, "g").setAtomName(6, "x").setAtomName(7, "y")
		.startRule().addHead(1).addToBody(6, false).endRule() // a :- not x.
		.startRule().addHead(2).addToBody(1, true).endRule()  // b :- a.
		.startRule().addHead(1).addToBody(2, true).addToBody(4, true).endRule() // a :- b, d.
		.startRule().addHead(2).addToBody(5, false).endRule() // b :- not g.
		.startRule().addHead(3).addToBody(6, true).endRule()  // c :- x.
		.startRule().addHead(4).addToBody(3, true).endRule()  // d :- c.
		.startRule().addHead(3).addToBody(4, true).endRule()  // c :- d.
		.startRule().addHead(4).addToBody(5, false).endRule() // d :- not g.
		.startRule().addHead(7).addToBody(5, false).endRule() // y :- not g.
		.startRule().addHead(6).addToBody(7, true).endRule()  // x :- y.
		.startRule().addHead(5).addToBody(7, false).endRule() // g :- not y.
		.endProgram();

		uint32 sccs = builder.stats.sccs;
		uint32 ufs  = builder.stats.ufsNodes;
		DG*    graph= builder.dependencyGraph();
		uint32 n    = graph->nodes();

		SharedContext ctx2;
		builder.cloneProgram(ctx2);
		
		CPPUNIT_ASSERT_EQUAL(sccs, builder.stats.sccs);
		CPPUNIT_ASSERT_EQUAL(ufs, builder.stats.ufsNodes);
		CPPUNIT_ASSERT_EQUAL(graph, builder.dependencyGraph());
		CPPUNIT_ASSERT_EQUAL(n, builder.dependencyGraph()->nodes());
	}

	void testWithSimpleCardinalityConstraint() {
		builder.startProgram(*ctx)
			.setAtomName(1, "a").setAtomName(2, "b")
			.startRule(CHOICERULE).addHead(2).endRule()
			.startRule(CONSTRAINTRULE, 1).addHead(1).addToBody(1, true).addToBody(2,true).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		DG* graph = builder.dependencyGraph();
		CPPUNIT_ASSERT( uint32(2) == graph->nodes() );
		SymbolTable& index = ctx->symTab();
		DG::NodePair<DG::AtomNode> a = graph->getAtomByLit(index[1].lit);
		const DG::BodyNode& body     = *graph->getBody(a.node->bodies()[0]).node;

		CPPUNIT_ASSERT(body.num_preds() == 2);
		CPPUNIT_ASSERT(body.extended());
		CPPUNIT_ASSERT(body.ext_bound() == 1);
		CPPUNIT_ASSERT(body.pred_inc() == 1);
		CPPUNIT_ASSERT(body.preds()[0] == a.id);
		CPPUNIT_ASSERT(body.preds()[1] == idMax);
		CPPUNIT_ASSERT(body.preds()[2] == index[2].lit.asUint());
		CPPUNIT_ASSERT(body.preds()[3] == idMax);
		CPPUNIT_ASSERT(body.pred_weight(0,false) == 1);
		CPPUNIT_ASSERT(body.pred_weight(1,true) == 1);

		CPPUNIT_ASSERT(a.node->type == DG::Node::type_ext);
		CPPUNIT_ASSERT(a.node->succs()[0] == idMax);
		CPPUNIT_ASSERT(a.node->succs()[1] == a.node->bodies()[0]);
		CPPUNIT_ASSERT(a.node->succs()[2] == 0);
		CPPUNIT_ASSERT(a.node->succs()[3] == idMax);
	}

	void testWithSimpleWeightConstraint() {
		builder.startProgram(*ctx)
			.setAtomName(1, "a").setAtomName(2, "b").setAtomName(3, "c")
			.startRule(CHOICERULE).addHead(2).addHead(3).endRule()
			.startRule(WEIGHTRULE, 2).addHead(1).addToBody(1, true, 2).addToBody(2,true, 2).addToBody(3, true, 1).endRule()
		;
		CPPUNIT_ASSERT_EQUAL(true, builder.endProgram());
		DG* graph = builder.dependencyGraph();
		CPPUNIT_ASSERT( uint32(2) == graph->nodes() );
		
		SymbolTable& index = ctx->symTab();
		DG::NodePair<DG::AtomNode> a = graph->getAtomByLit(index[1].lit);
		const DG::BodyNode& body     = *graph->getBody(a.node->bodies()[0]).node;

		CPPUNIT_ASSERT(body.num_preds() == 3);
		CPPUNIT_ASSERT(body.extended());
		CPPUNIT_ASSERT(body.ext_bound() == 2);
		CPPUNIT_ASSERT(body.pred_inc() == 2);
		CPPUNIT_ASSERT(body.preds()[0] == a.id);
		CPPUNIT_ASSERT(body.preds()[2] == idMax);
		CPPUNIT_ASSERT(body.preds()[3] == index[2].lit.asUint());
		CPPUNIT_ASSERT(body.preds()[5] == index[3].lit.asUint());
		CPPUNIT_ASSERT(body.preds()[7] == idMax);
		CPPUNIT_ASSERT(body.pred_weight(0, false) == 2);
		CPPUNIT_ASSERT(body.pred_weight(1, true) == 2);
		CPPUNIT_ASSERT(body.pred_weight(2, true) == 1);
		
		CPPUNIT_ASSERT(a.node->type == DG::Node::type_ext);
		CPPUNIT_ASSERT(a.node->succs()[0] == idMax);
		CPPUNIT_ASSERT(a.node->succs()[1] == a.node->bodies()[0]);
		CPPUNIT_ASSERT(a.node->succs()[2] == 0);
		CPPUNIT_ASSERT(a.node->succs()[3] == idMax);
	}
private:
	SharedContext* ctx;
	ProgramBuilder builder;
	typedef ProgramBuilder::PBADG DG;
};
CPPUNIT_TEST_SUITE_REGISTRATION(DependencyGraphTest);
} } 
