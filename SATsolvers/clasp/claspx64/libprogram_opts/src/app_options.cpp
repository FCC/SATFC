// 
// Copyright (c) 2006-2007, Benjamin Kaufmann
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
#include <program_opts/app_options.h>
#include <program_opts/typed_value.h>
#include <program_opts/composite_value_parser.h>
#include <cctype>
#include <limits.h>
#include <cstring>
#ifdef _MSC_VER
#pragma warning (disable : 4996)
#endif
#include <stdio.h>

using namespace ProgramOptions;
using namespace std;
namespace ProgramOptions {
/////////////////////////////////////////////////////////////////////////////////////////
// Parsing & Validation of command line
/////////////////////////////////////////////////////////////////////////////////////////
bool AppOptions::parse(int argc, char** argv, const char* appName, ProgramOptions::PosOption p) {
	ParsedOptions parsed;
	try {
		OptionContext allOpts(std::string(appName).append(" Options"));
		HelpOpt       helpO = initHelpOption();
		help    = 0;
		version = false;
		messages.clear();
		Value* hv = helpO.first == desc_level_default ? storeTo(help)->flag() : storeTo(help)->arg("<n>")->implicit("1");
		OptionGroup basic("Basic Options");
		basic.addOptions()
			("help,h"   , hv,  helpO.second.c_str())
			("version,v", flag(version),   "Print version information and exit")    
		;
		allOpts.add(basic);
		initOptions(allOpts);
		parsed.assign(parseCommandLine(argc, argv, allOpts, false, p));
		allOpts.assignDefaults(parsed);
		if (help) {
			if (help < 0 || (help-1) > (int)helpO.first) { 
				char buf[128];
				sprintf(buf, "%d", help);
				throw ValueError(allOpts.caption(), ValueError::invalid_value, "help", buf);
			}
			DescriptionLevel x = (DescriptionLevel)std::min(help-1, (int)desc_level_all);
			allOpts.setActiveDescLevel(x);
			printHelp(allOpts);
			return true;
		}
		help = 0;
		if (version) {
			printVersion(allOpts);
			return true;
		}
		return validateOptions(allOpts, parsed, messages);
	}
	catch(const std::exception& e) {
		messages.error = e.what();
		return false;
	}
	return true;
}
}
