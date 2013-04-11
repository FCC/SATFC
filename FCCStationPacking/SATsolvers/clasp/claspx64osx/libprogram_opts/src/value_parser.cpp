//
//  Copyright (c) Benjamin Kaufmann 2004
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
#ifdef _MSC_VER
#pragma warning (disable : 4996)
#endif
#include <program_opts/value_parser.h>
#include <cctype>
#include <algorithm>
#include <iterator>
#include <cstdlib>
#include <limits>
#include <cassert>
#include <cstring>
namespace ProgramOptions {  namespace {
	struct CmpToLower {
		bool operator()(char lhs, char rhs) const {
			return (char)std::tolower(static_cast<unsigned char>(lhs))
				== rhs;
		}
	};
	struct ToLower {
		char operator()(char c) const { return (char)std::tolower(static_cast<unsigned char>(c)); }
	};
} // end namespace detail


FlagStr FlagStr::map_s[8] = {
	{"1",true}   , {"0",false},     // [0-1]: 1
	{"on",true}  , {"no",false},    // [2-3]: 2
	{"yes",true} , {"off",false},   // [4-5]: 3
	{"true",true}, {"false",false}  // [6-7]: > 3
};

const FlagStr* FlagStr::find(const std::string& s) {	
	return findImpl(s.data(), s.size());
}
const FlagStr* FlagStr::find(const char* s) {
	return findImpl(s, std::strlen(s));
}
const FlagStr* FlagStr::findImpl(const char* s, std::size_t len) {
	if (len && len < 6) {
		CmpToLower cmp;
		std::size_t pos       = std::min( (len - 1) << 1, std::size_t(6) );
		const std::string& s1 = map_s[pos].str;
		const std::string& s2 = map_s[pos+1].str;
		if (s1.size() == len && std::equal(s, s+len, s1.data(), cmp)) {
			return &map_s[pos];
		}
		if (s2.size() == len && std::equal(s, s+len, s2.data(), cmp)) {
			return &map_s[pos+1];
		}
	}
	return 0;
}

StringSlice FlagStr::parse(const StringSlice& in, bool& out, int extra) {
	if (in.size() == 0 || !in.ok()) {
		return in.parsed(false);
	}
	StringSlice ret = in;
	bool temp       = true;
	if (const FlagStr* x= FlagStr::findImpl(in.data(), in.size())) {
		temp = x->val;
		ret  = in.parsed(true, x->str.size());
	}
	if (ret.complete() || (ret.ok() && extra)) {
		out = temp;
		return ret;
	}
	return in.parsed(false);
}
bool FlagStr::store_true(const std::string& val, bool& out) {
	if (val.empty()) { return out = true; }
	if (const FlagStr* x= FlagStr::find(val)) {
		out = x->val;
		return true;
	}
	return false;
}

bool FlagStr::store_false(const std::string& val, bool& out) {
	bool temp;
	return store_true(val, temp)
		&&   ((out = !temp), true);
}

std::string toLower(const std::string& s) {
	std::string ret; ret.reserve(s.size());
	std::transform(s.begin(), s.end(), std::back_inserter(ret), ToLower());
	return ret;
}
void StringSlice::skip(size_t num) {
	assert(ok() && num <= size());
	ptr_ += num;
	len_ -= (num*2);
	while (len_ && std::isspace(static_cast<unsigned char>(*ptr_))) {
		++ptr_;
		len_ -= 2;
	}
}

}
