// 
// Copyright (c) 2006-2012, Benjamin Kaufmann
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
#ifndef CLASP_READER_H_INCLUDED
#define CLASP_READER_H_INCLUDED

#ifdef _MSC_VER
#pragma once
#endif

#include <istream>
#include <stdexcept>
#include <clasp/literal.h>
/*!
 * \file 
 * Defines basic functions and classes for program input.
 */
namespace Clasp {
class ProgramBuilder;
class SharedContext;
class MinimizeBuilder;
struct PureLitMode_t {
	enum Mode { 
		assert_pure_no  = 0, /**< an unremovable constraint (e.g. a problem constraint)        */
		assert_pure_yes = 1, /**< a (removable) constraint derived from conflict analysis      */
		assert_pure_auto= 2  /**< a (removable) constraint derived from unfounded set checking */
	};
};  
typedef PureLitMode_t::Mode PureLitMode;
struct ObjectiveFunction {
	WeightLitVec lits;
	wsum_t       adjust;
	wsum_t       rhs;
};

class Input {
public:
	enum Format     { SMODELS, DIMACS, OPB};
	enum Property   { PRESERVE_MODELS = 1, PRESERVE_MODELS_ON_MIN = 2, AS_MAX_SAT = 4};
	union ApiPtr { 
		explicit ApiPtr(ProgramBuilder* a) : api(a) {}
		explicit ApiPtr(SharedContext* c)  : ctx(c) {}
		ProgramBuilder* api;
		SharedContext*  ctx;
	};
	Input() {}
	virtual ~Input() {}
	virtual Format format() const = 0;
	virtual bool   read(ApiPtr api, uint32 properties) = 0;
	virtual void   addMinimize(MinimizeBuilder& m, ApiPtr api) = 0;
	virtual void   getAssumptions(LitVec& a) = 0;
private:
	Input(const Input&);
	Input& operator=(const Input&);
};

class StreamInput : public Input {
public:
	explicit StreamInput(std::istream& in, Format f);
	Format format() const { return format_; }
	bool   read(ApiPtr api, uint32 properties);
	void   addMinimize(MinimizeBuilder& m, ApiPtr api);
	void   getAssumptions(LitVec&) {}
private:
	ObjectiveFunction func_;
	std::istream&     prg_;
	Format            format_;
};


//! Auto-detect input format of problem.
Input::Format detectFormat(std::istream& prg);

//! Reads a logic program in SMODELS-input format.
/*!
 * \ingroup problem
 * \param prg The stream containing the logic program.
 * \param api The ProgramBuilder object to use for program creation.
 * \pre The api is ready, i.e. startProgram() was called.
 */
bool parseLparse(std::istream& prg, ProgramBuilder& api);

//! Reads a CNF/WCNF in simplified DIMACS-format.
/*!
 * \ingroup problem
 * \param prg The stream containing the CNF.
 * \param ctx The context object in which to store the problem.
 * \param pm  How to handle pure literals.
 * \param o An out parameter storing optional objective function. 
 * \param maxSat Treat problem as maxsat.
 */
bool parseDimacs(std::istream& prg, SharedContext& ctx, PureLitMode pm, ObjectiveFunction& o, bool maxSat = false);
inline bool parseDimacs(std::istream& prg, SharedContext& ctx, PureLitMode pm = PureLitMode_t::assert_pure_no) {
	ObjectiveFunction o;
	return parseDimacs(prg, ctx, pm, o, false);
}

//! Reads a Pseudo-Boolean problem in OPB-format.
/*!
 * \ingroup problem
 * \param prg The stream containing the PB-problem.
 * \param ctx The context object in which to store the problem.
 * \param objective An out parameter that contains an optional objective function. 
 */
bool parseOPB(std::istream& prg, SharedContext& ctx, ObjectiveFunction& objective);


//! Instances of this class are thrown if a problem occurs during reading the input.
struct ReadError : public ClaspError {
	ReadError(unsigned line, const char* msg);
	static std::string format(unsigned line, const char* msg);
	unsigned line_;
};

//! Wrapps an std::istream and provides basic functions for extracting numbers and strings.
class StreamSource {
public:
	explicit StreamSource(std::istream& is);
	//! Returns the character at the current reading-position.
	char operator*() {
		if (buffer_[pos_] == 0) { underflow(); }
		return buffer_[pos_];
	}
	//! Advances the current reading-position.
	StreamSource& operator++() { ++pos_; **this; return *this; }
	
	//! Reads a base-10 integer.
	/*!
	 * \pre system uses ASCII
	 */
	bool parseInt(int& val);
	bool parseInt64(int64& val);
	bool parseInt(int& val, int min, int max);
	//! Consumes next character if equal to c.
	bool match(char c) { return (**this == c) && (++*this, true); }
	//! Consumes next character(s) if equal to EOL.
	/*!
	 * Consumes the next character if it is either '\n' or '\r'
	 * and increments the internal line counter.
	 * 
	 * \note If next char is '\r', the function will also consume
	 * a following '\n' (i.e. matchEol also matches CR/LF).
	 */
	bool matchEol();
	//! Skips horizontal white-space.
	bool skipSpace() { while (match(' ') || match('\t')) { ; } return true; }
	//! Skips horizontal and vertical white-space.
	bool skipWhite() { do { skipSpace(); } while (matchEol()); return true; }
	//! Returns the number of matched EOLs + 1.
	unsigned line() const { return line_; }
private:
	StreamSource(const std::istream&);
	StreamSource& operator=(const StreamSource&);
	void underflow();
	char buffer_[2048];
	std::istream& in_;
	unsigned pos_;
	unsigned line_;
};

//! Skips the current line.
inline void skipLine(StreamSource& in) {
	while (*in && !in.matchEol()) { ++in; }
}

//! Consumes next character if equal to c.
/*!
 * \param in StreamSource from which characters should be read
 * \param c character to match
 * \param sw skip leading spaces
 * \return
 *  - true if character c was consumed
 *  - false otherwise
 *  .
 */
inline bool match(StreamSource& in, char c, bool sw) {
	return (!sw || in.skipSpace()) && in.match(c);
}

//! Consumes string str.
/*!
 * \param in StreamSource from which characters should be read
 * \param str string to match
 * \param sw skip leading spaces
 * \pre   str != 0
 * \return
 *  - true if string str was consumed
 *  - false otherwise
 *  .
 */
inline bool match(StreamSource& in, const char* str, bool sw) {
	if (sw) in.skipSpace();
	while (*str && in.match(*str)) { ++str; }
	return *str == 0;
}

inline bool matchEol(StreamSource& in, bool sw) {
	if (sw) in.skipSpace();
	return in.matchEol();
}

//! Extracts characters from in and stores them into buf until a newline character or eof is found.
/*!
 * \note    The newline character is extracted and discarded, i.e. it is not stored and the next input operation will begin after it.
 * \return  True if a newline was found, false on eof
 * \post    buf.back() == '\0'
 */
bool readLine( StreamSource& in, PodVector<char>::type& buf );

}
#endif
