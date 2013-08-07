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
#include <clasp/clasp_facade.h>
#include <iostream>

// This example uses the ClaspFacade to compute
// the stable models of the program
//    a :- not b.
//    b :- not a.
//
// The ClaspFacade is a convenient wrapper for the 
// services of the clasp library.
// Basically, it works as a kind of state machine and
// distinguishes three states:
//  - read      : In this state the problem to compute is read
//                from an input object. The input object must
//                implement the Clasp::Input interface. It is
//                provided by the user.
//  - preprocess: In this state the problem is preprocessed and
//                added to the solver.
//  - solve     : In this state, the solutions to the problem are
//                computed. Each time a solution is found, an 
//                ClaspFacade::event_model event is generated. The user
//                can react to this (and other events) by providing
//                an object that implements the ClaspFacade::Callback interface.
//
// See clasp_facade.h for details.

// In order to use the ClaspFacade, we must provide an input class.
// See reader.h for details.
struct Problem : public Clasp::Input {
	// Possible formats are: SMODELS, DIMACS, OPB.
	Format format() const { return SMODELS; }
	// If format is SMODELS, the ClaspFacade will create a ProgramBuilder object
	// to be used to define the logic program.
	bool   read(ApiPtr p, uint32) {
		p.api->setAtomName(1, "a");
		p.api->setAtomName(2, "b");
		p.api->startRule(Clasp::BASICRULE).addHead(1).addToBody(2, false).endRule();
		p.api->startRule(Clasp::BASICRULE).addHead(2).addToBody(1, false).endRule();
		return true;
	}
	// If the problem contains optimize statements, we must provide them here.
	// In our example, no optimize statements exist.
	void addMinimize(Clasp::MinimizeBuilder&, ApiPtr) {}
	// Only to be used in incremental solving.
	void   getAssumptions(Clasp::LitVec&) {}
};

// Callback class for reacting on events and state changes of the
// ClaspFacade.
class MyCallback : public Clasp::ClaspFacade::Callback {
public:
	typedef Clasp::ClaspFacade::Event Event;
	// Called if the current configuration contains unsafe/unreasonable options.
	void warning(const char* msg) { std::cout << "Warning: " << msg << std::endl; }
	// Called on entering/exiting a state.
	void state(Event, Clasp::ClaspFacade&)     { }
	// Called for important events, e.g. a model has been found.
	void event(const Clasp::Solver& s, Event e, Clasp::ClaspFacade& f) {
		if (e == Clasp::ClaspFacade::event_model) {
			std::cout << "Model " << f.config()->ctx.enumerator()->enumerated << ": \n";
			const Clasp::SymbolTable& symTab = s.sharedContext()->symTab();
			for (Clasp::SymbolTable::const_iterator it = symTab.begin(); it != symTab.end(); ++it) {
				if (s.isTrue(it->second.lit) && !it->second.name.empty()) {
					std::cout << it->second.name.c_str() << " ";
				}
			}
			std::cout << std::endl;
		}
	}
};

void example2() {
	// The "interface" to the clasp library.
	Clasp::ClaspFacade libclasp;
	// Our input class.
	Problem            problem;
	// Our class for printing models etc.
	MyCallback         cb;
	
	// Aggregates configuration options.
	// Using config, you can control many parts of the search, e.g.
	// - the amount and kind of preprocessing
	// - the enumerator to use and the number of models to compute
	// - the heuristic used for decision making
	// - the restart strategy
	// - ...
	Clasp::ClaspConfig config;

	// We want to compute all models but
	// otherwise we use the default configuration.
	config.enumerate.numModels = 0;
	
	// Solve the problem using the given configuration.
	// Report status via our callback interface
	libclasp.solve(problem, config, &cb);
	
	std::cout << "No more models!" << std::endl;
}
