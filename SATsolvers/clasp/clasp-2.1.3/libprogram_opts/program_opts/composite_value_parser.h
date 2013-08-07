//
//  Copyright (c) Benjamin Kaufmann 2010
//
//  This is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version. 
// 
//  This file is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this file; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
//
// NOTE: ProgramOptions is inspired by Boost.Program_options
//       see: www.boost.org/libs/program_options
//
#ifndef PROGRAM_OPTIONS_COMPOSITE_VALUE_PARSER_H_INCLUDED
#define PROGRAM_OPTIONS_COMPOSITE_VALUE_PARSER_H_INCLUDED
#include <istream>
#include "value_parser.h"
#include <vector>
#include <utility>
#include <cassert>
#include <iterator>
namespace ProgramOptions { namespace Detail {

inline StringSlice match(const StringSlice& x, char t) {
	if (!x.ok() || x.complete()) { return x; }
	return x.parsed(x.data()[0] == t, 1);
}

}

//! parses a sequence of Ts as '['? val1 [, ...][, valn] ']'?
template <class T, class Out>
StringSlice parseSequence(const StringSlice& in, Out out, int maxNum, int extra) {
	if (in.size() == 0) return in.parsed(false);
	StringSlice x = in, term = Detail::match(in, '[');
	if (term.ok()) { x = term; }
	for (int num = 0, ext = extra + term.ok();;) {
		T temp;
		x = parseValue(x, temp, 1);
		if (!x.ok())      { return in.parsed(false); }
		*out = temp; ++out; ++num;
		if (x.complete()) { break; }
		if (num == maxNum || !ComponentSeparator<T>::isSeparator(x.data()[0])) { x = ext ? x : in.parsed(false); break; }
		x = x.parsed(true, 1);
		if (x.complete()) { return in.parsed(false); }
	}
	if (term.ok()) { x = Detail::match(x, ']'); }
	return x.ok () && (extra != 0 || x.complete()) ? x : in.parsed(false);
}

//! parses a vector<T> as sequence of Ts.
template <class T>
StringSlice parseValue(const StringSlice& in, std::vector<T>& result, int extra) {
	size_t rSize = result.size();
	StringSlice x= parseSequence<T>(in, std::back_inserter(result), -1, extra);
	if (!x.ok() || (extra == 0 && !x.complete())) {
		result.resize(rSize); 
		return in.parsed(false);
	}
	return x;
}

//! parses a pair<T,U> as '('? valT [, valU] ')'?
template <class T, class U>
StringSlice parseValue(const StringSlice& in, std::pair<T, U>& result, int extra) {
	if (in.size() == 0) return in.parsed(false);
	StringSlice x = in, term = Detail::match(in, '(');
	if (term.ok()) { x = term; }
	T tempT(result.first);
	x = parseValue(x, tempT, extra+1);
	if (x.complete()) {
		if (term.ok()) { return in.parsed(false); }
		result.first = tempT;
		return x;
	}
	U tempU(result.second);
	if (x.ok() && x.data()[0] == ',') {
		x = parseValue(x.parsed(true, 1), tempU, extra + term.ok());
	}
	if (term.ok()) { x = Detail::match(x, ')'); }
	if (!x.ok() || (extra == 0 && !x.complete())) { return in.parsed(false); }
	result.first  = tempT;
	result.second = tempU;
	return x;
}
}
#endif
