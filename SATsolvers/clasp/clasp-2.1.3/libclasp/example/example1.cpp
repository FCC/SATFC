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

// Add the libclasp directory to the list of 
// include directoies of your build system.
#include <clasp/program_builder.h>   // for defining logic programs
#include <clasp/unfounded_check.h>   // unfounded set checkers
#include <clasp/model_enumerators.h> // for enumerating answer sets
#include <clasp/solve_algorithms.h>  // for enumerating answer sets
#include <iostream>

// callback interface for printing answer sets
struct AspOutPrinter : public Clasp::Enumerator::Report {
	void reportModel(const Clasp::Solver& s, const Clasp::Enumerator&);
	void reportSolution(const Clasp::Solver& s, const Clasp::Enumerator&, bool);
};


// Compute the stable models of the program
//    a :- not b.
//    b :- not a.
void example1() {
	// The ProgramBuilder lets you define logic programs.
	// It preprocesses logic programs and converts them
	// to the internal solver format.
	// See program_builder.h for details.
	Clasp::ProgramBuilder api;
	
	// Among other things, SharedContext maintains a Solver object 
	// which hosts the data and functions for CDNL answer set solving.
	// SharedContext also contains the symbol table which stores the 
	// mapping between atoms of the logic program and the 
	// propositional literals in the solver.
	// See shared_context.h for details.
	Clasp::SharedContext  ctx;
	
	// startProgram must be called once before we can add atoms/rules
	api.startProgram(ctx);
	
	// Populate symbol table. Each atoms must have a unique id, the name is optional.
	// The symbol table then maps the ids to the propositional 
	// literals in the solver.
	api.setAtomName(1, "a");
	api.setAtomName(2, "b");
	
	// Define the rules of the program.
	api.startRule(Clasp::BASICRULE).addHead(1).addToBody(2, false).endRule();
	api.startRule(Clasp::BASICRULE).addHead(2).addToBody(1, false).endRule();
	
	// Once all rules are defined, call endProgram() to load the (simplified)
	// program into the context object.
	api.endProgram();
	if (api.dependencyGraph() && api.dependencyGraph()->nodes() > 0) {
		// The program is non tight - add unfounded set checker.
		Clasp::DefaultUnfoundedCheck* ufs = new Clasp::DefaultUnfoundedCheck();
		// Register with solver and graph & transfer ownership.
		ufs->attachTo(*ctx.master(), api.dependencyGraph()); 
	}
	
	// For printing answer sets.
	AspOutPrinter printer;

	// Since we want to compute more than one
	// answer set, we need an enumerator.
	// See enumerator.h for details
	ctx.addEnumerator(new Clasp::BacktrackEnumerator(0,&printer));
	ctx.enumerator()->enumerate(0);

	// endInit() must be called once before the search starts.
	ctx.endInit();

	// Aggregates some solving parameters, e.g.
	//  - restart-strategy
	//  - ...
	// See solve_algorithms.h for details.
	Clasp::SolveParams    params;

	// solve() starts the search for answer sets. It uses the 
	// given parameters to control the search.
	Clasp::solve(ctx, params);
}

void AspOutPrinter::reportModel(const Clasp::Solver& s, const Clasp::Enumerator& e) {
	std::cout << "Model " << e.enumerated << ": \n";
	// get the symbol table from the solver
	const Clasp::SymbolTable& symTab = s.sharedContext()->symTab();
	for (Clasp::SymbolTable::const_iterator it = symTab.begin(); it != symTab.end(); ++it) {
		// print each named atom that is true w.r.t the current assignment
		if (s.isTrue(it->second.lit) && !it->second.name.empty()) {
			std::cout << it->second.name.c_str() << " ";
		}
	}
	std::cout << std::endl;
}
void AspOutPrinter::reportSolution(const Clasp::Solver&, const Clasp::Enumerator&, bool complete) {
	if (complete) std::cout << "No more models!" << std::endl;
	else          std::cout << "More models possible!" << std::endl;
}
