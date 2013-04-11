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
#ifndef APP_OPTIONS_H_INCLUDED
#define APP_OPTIONS_H_INCLUDED

#ifdef _MSC_VER
#pragma warning (disable : 4200) // nonstandard extension used : zero-sized array
#pragma once
#endif

#include <string>
#include <utility>
#include "program_options.h"

namespace ProgramOptions {
typedef std::vector<std::string> StringSeq;
struct Messages {
	std::string error;
	StringSeq   warning;
	void clear() { error.clear(); warning.clear(); }
};
/////////////////////////////////////////////////////////////////////////////////////////
// Interface for parsing application options
/////////////////////////////////////////////////////////////////////////////////////////
class AppOptions {
public:
	AppOptions() {}
	virtual ~AppOptions() {}
	bool parse(int argc, char** argv, const char* appName, ProgramOptions::PosOption p);
	
	Messages   messages; // n warnings and at most one error
	int        help;     // print help and exit
	bool       version;  // print version and exit
	
	typedef std::pair<DescriptionLevel, std::string> HelpOpt;
private:
	AppOptions(const AppOptions&);
	AppOptions& operator=(const AppOptions&);
	virtual HelpOpt initHelpOption() const { return HelpOpt(desc_level_default, "Print help information and exit"); }
	virtual void initOptions(ProgramOptions::OptionContext& root) = 0;
	virtual bool validateOptions(const ProgramOptions::OptionContext& root, const ProgramOptions::ParsedOptions&, Messages&) = 0;
	virtual void printHelp(const ProgramOptions::OptionContext& root) = 0;
	virtual void printVersion(const ProgramOptions::OptionContext& root) = 0;
};
}
#endif
