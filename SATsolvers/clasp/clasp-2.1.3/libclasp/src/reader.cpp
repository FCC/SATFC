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
#include <clasp/reader.h>
#include <clasp/program_builder.h>
#include <clasp/clause.h>
#include <clasp/solver.h>
#include <clasp/weight_constraint.h>
#include <clasp/minimize_constraint.h>
#include <limits.h>
#include <cassert>
#include <stdio.h>
#include <stdlib.h>
#ifdef _WIN32
#include <io.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <errno.h>
#define snprintf _snprintf
#pragma warning (disable : 4996)
static int mkstemp(char* templ) {
	int fd;
	std::size_t x = strlen(templ);
	assert(x >= 6);
	do {
		_mktemp(templ);
		fd = _open(templ, _O_CREAT | _O_EXCL | _O_BINARY | _O_RDWR, _S_IREAD | _S_IWRITE);
		if (fd != -1) break;
		strcpy((templ+x)-6, "XXXXXX");
	} while (errno == EEXIST);
	return fd;
}
#endif
static const char* getTempPath() { 
	if (const char* x1 = getenv("TMP"))  return x1;
	if (const char* x2 = getenv("TEMP")) return x2;
	return P_tmpdir;
}
namespace Clasp {
ReadError::ReadError(unsigned line, const char* msg) : ClaspError(format(line, msg)), line_(line) {}
std::string ReadError::format(unsigned line, const char* msg) {
	char buffer[1024];
	snprintf(buffer, 1023, "Read Error: Line %u, %s", line, msg);
	buffer[1023] = 0;
	return buffer;
}
/////////////////////////////////////////////////////////////////////////////////////////
// StreamSource
/////////////////////////////////////////////////////////////////////////////////////////
StreamSource::StreamSource(std::istream& is) : in_(is), pos_(0), line_(1) {
	underflow();
}

void StreamSource::underflow() {    
	pos_ = 0;
	buffer_[0] = 0;
	if (!in_) return;
	in_.read( buffer_, sizeof(buffer_)-1 );
	buffer_[in_.gcount()] = 0;
}

bool StreamSource::parseInt64(int64& val) {
	skipSpace();
	bool pos = match('+') || !match('-');
	int  d   = **this - '0';
	if (d < 0 || d > 9) { return false; }
	val      = 0;
	do {
		val *= 10;
		val += d;
		d    = *(++*this) - '0';
	} while (d >= 0 && d <= 9);
	val  = pos ? val : -val;
	return true;
}
bool StreamSource::parseInt(int& val) { 
	int64 x;
	return parseInt64(x)
		&&   x >= INT_MIN
		&&   x <= INT_MAX
		&&   (val=(int)x, true);
}
bool StreamSource::parseInt(int& val, int min, int max) { 
	int64 x;
	return parseInt64(x)
		&&   x >= min
		&&   x <= max
		&&   (val=(int)x, true);
}

bool StreamSource::matchEol() {
	if (match('\n')) {
		++line_;
		return true;
	}
	if (match('\r')) {
		match('\n');
		++line_;
		return true;
	}
	return false;
}

bool readLine(StreamSource& in, PodVector<char>::type& buf ) {
	char buffer[1024];
	bool eol = false;
	uint32 i;
	buf.clear();
	for (i = 0; *in && (eol = in.matchEol()) == false; ++in) {
		buffer[i] = *in;
		if (++i == 1024) {
			buf.insert(buf.end(), buffer, buffer+i);
			i = 0;
		}
	}
	buf.insert(buf.end(), buffer, buffer+i);
	buf.push_back('\0');
	return eol;
}
	
namespace {
/////////////////////////////////////////////////////////////////////////////////////////
// LPARSE PARSING
/////////////////////////////////////////////////////////////////////////////////////////
class LparseReader {
public:
	LparseReader();
	~LparseReader();
	bool parse(std::istream& prg, ProgramBuilder& api);
	void  clear();
	Var   parseAtom();
	bool  readRules();
	bool  readSymbolTable();
	bool  readComputeStatement();
	bool  readModels();
	bool  endParse();
	bool  readRule(int);
	bool  readBody(uint32 lits, uint32 neg, bool weights);
	bool  check(bool cond, const char* m) const {
		return cond || (throw ReadError(source_->line(), m), false);
	}
	PrgRule         rule_;
	StreamSource*   source_;
	ProgramBuilder* api_;
};

LparseReader::LparseReader()
	: source_(0)
	, api_(0) {
}

LparseReader::~LparseReader() {
	clear();
}

void LparseReader::clear() {
	rule_.clear();
	api_  = 0;
}

bool LparseReader::parse(std::istream& prg, ProgramBuilder& api) {
	clear();
	api_ = &api;
	if (!prg) {
		throw ReadError(0, "Could not read from stream!");
	}
	StreamSource source(prg);
	source_ = &source;
	return readRules()
		&& readSymbolTable()
		&& readComputeStatement()
		&& readModels()
		&& endParse();
}

Var LparseReader::parseAtom() {
	int r = -1;
	if (!source_->parseInt(r, 1, (int)varMax)) {
		throw ReadError(source_->line(), (r == -1 ? "Atom id expected!" : "Atom out of bounds"));
	}
	return static_cast<Var>(r);
}

bool LparseReader::readRules() {
	int rt = -1;
	while ( source_->skipWhite() && source_->parseInt(rt) && rt != 0 && readRule(rt) ) ;
	return check(rt == 0, "Rule type expected!")
		&&   check(matchEol(*source_, true), "Symbol table expected!")
		&&   source_->skipWhite();
}

bool LparseReader::readRule(int rt) {
	int bound = -1;
	check(rt > 0 && rt <= 6 && rt != 4, "Unsupported rule type!");
	RuleType type(static_cast<RuleType>(rt));
	rule_.setType(type);
	if ( type == BASICRULE || rt == CONSTRAINTRULE || rt == WEIGHTRULE) {
		rule_.addHead(parseAtom());
		check(rt != WEIGHTRULE || source_->parseInt(bound, 0, INT_MAX), "Weightrule: Positive weight expected!");
	}
	else if (rt == CHOICERULE) {
		int heads = 0;
		check(source_->parseInt(heads, 1, INT_MAX), "Choicerule: To few heads");
		for (int i = 0; i < heads; ++i) {
			rule_.addHead(parseAtom());
		}
	}
	else {
		assert(rt == 6);
		int x = 0;
		check(source_->parseInt(x,0,0), "Minimize rule: 0 expected!");
	}
	int lits = 0, neg = 0;
	check(source_->parseInt(lits, 0, INT_MAX), "Number of body literals expected!");
	check(source_->parseInt(neg, 0, lits), "Illegal negative body size!");
	check(rt != CONSTRAINTRULE || source_->parseInt(bound, 0, INT_MAX), "Constraint rule: Positive bound expected!");
	if (bound >= 0) {
		rule_.setBound(static_cast<uint32>(bound));
	}
	return readBody(static_cast<uint32>(lits), static_cast<uint32>(neg), rt >= 5);  
}

bool LparseReader::readBody(uint32 lits, uint32 neg, bool readWeights) {
	for (uint32 i = 0; i != lits; ++i) {
		rule_.addToBody(parseAtom(), i >= neg, 1);
	}
	if (readWeights) {
		for (uint32 i = 0; i < lits; ++i) {
			int w = 0;
			check(source_->parseInt(w, 0, INT_MAX), "Weight Rule: bad or missing weight!");
			rule_.body[i].second = w;
		}
	} 
	api_->addRule(rule_);
	rule_.clear();
	return check(matchEol(*source_, true), "Illformed rule body!");
}

bool LparseReader::readSymbolTable() {
	int a = -1;
	PodVector<char>::type buf;
	buf.reserve(1024);
	while (source_->skipWhite() && source_->parseInt(a) && a != 0) {
		check(a >= 1, "Symbol Table: Atom id out of bounds!");
		check(source_->skipSpace() && readLine(*source_, buf), "Symbol Table: Atom name too long or end of file!");
		api_->setAtomName(a, &buf[0]);
	}
	check(a == 0, "Symbol Table: Atom id expected!");
	return check(matchEol(*source_, true), "Compute Statement expected!");
}

bool LparseReader::readComputeStatement() {
	const char* B[2] = { "B+", "B-" };
	for (int i = 0; i != 2; ++i) {
		source_->skipWhite();
		check(match(*source_, B[i], false) && matchEol(*source_, true), (i == 0 ? "B+ expected!" : "B- expected!"));
		int id = -1;
		while (source_->skipWhite() && source_->parseInt(id) && id != 0) {
			check(id >= 1, "Compute Statement: Atom out of bounds");
			api_->setCompute(static_cast<Var>(id), B[i][1] == '+');
		}
		check(id == 0, "Compute Statement: Atom id or 0 expected!");
	}
	return true;
}

bool LparseReader::readModels() {
	int m = 1;
	check(source_->skipWhite() && source_->parseInt(m, 0, INT_MAX), "Number of models expected!");
	return true;
}

bool LparseReader::endParse() {
	return true;
} 

/////////////////////////////////////////////////////////////////////////////////////////
// DIMACS PARSING
/////////////////////////////////////////////////////////////////////////////////////////
#define ERROR(x) throw ReadError(in.line(), (x));

class TempFile {
public:
	TempFile() : file_(0), name_(0) {}
	~TempFile() { destroy(); }
	bool create() {
		destroy();
		const char* t = getTempPath();
		name_ = (char*)malloc(strlen(t) + strlen("/clasp.XXXXXX") + 1);
		strcpy(name_, t);
		strcat(name_, "/clasp.XXXXXX");
		int fd= mkstemp(name_);
		return fd != -1 && (file_ = fdopen(fd, "wb+")) != 0;
	}
	void destroy() {
		if (file_) {
			fclose(file_);
			remove(name_);
			file_ = 0;
		}
		free(name_);
		name_ = 0; 
	}
	bool read(void* buf, uint32 size, uint32 count) {
		return fread(buf, size, count, file_) == count;
	}
	bool write(void* buf, uint32 size, uint32 count) {
		return fwrite(buf, size, count, file_) == count;
	}
	void rewind() { ::rewind(file_); }
private:
	TempFile(const TempFile&);
	TempFile& operator=(const TempFile&);
	FILE* file_;
	char* name_;
};

bool parseDimacsImpl(std::istream& prg, SharedContext& ctx, PureLitMode pure, ObjectiveFunction& opt, bool maxSat) {
	LitVec currentClause;
	ClauseCreator nc(ctx.master());
	SatPreprocessor* p = 0;
	int numVars = -1, lastVar = 0, maxVar = 0, numClauses = 0;
	bool ret = true;
	StreamSource in(prg);
	
	// For each var v: 0000p1p2c1c2
	// p1: set if v occurs negatively in any clause
	// p2: set if v occurs positively in any clause
	// c1: set if v occurs negatively in the current clause
	// c2: set if v occurs positively in the current clause
	PodVector<uint8>::type  flags;
	TempFile temp;
	bool  wcnf  = false, partial = false;
	wsum_t top  = 1;
	uint8  keep = 12u;
	for (uint32 state = 0;ret;)  {
		in.skipWhite();
		if      (*in == 'c')       { skipLine(in); }
		else if (*in == 0 && state){ break; }
		else if (state == 1)       { // read clause
			int64 cw = 1;
			if (wcnf && (!in.parseInt64(cw) || cw < 1)) { ERROR("wcnf: clause weight expected!"); }
			int lit;
			Literal rLit;
			bool sat = false;
			currentClause.clear();
			for (;;){
				if (!in.parseInt(lit))  { ERROR("Bad parameter in clause!"); }
				if (abs(lit) > numVars) { ERROR("Unrecognized format - variables must be numbered from 1 up to $VARS!"); }
				in.skipWhite();
				rLit = lit >= 0 ? posLit(lit) : negLit(-lit);
				if (lit == 0) {
					nc.start();
					if (!sat && top != cw) {
						weight_t w = (weight_t)cw;
						if (w != cw) { ERROR("Clause weight too large!"); }
						// soft clause
						if (currentClause.size() > 1) {
							flags.push_back(keep); // not pure!
							currentClause.push_back(posLit(++lastVar));
							opt.lits.push_back(WeightLiteral(currentClause.back(), w));
						}
						else {
							// unit clause - only optimize literal
							Literal x = currentClause.back();
							opt.lits.push_back(WeightLiteral(~x, w));
							flags[x.var()] |= keep;
							sat = true;
						}
					}
					if (!sat && partial) {
						uint32 size = (uint32)currentClause.size();
						if (!temp.write(&size, sizeof(uint32), 1)) {
							throw std::runtime_error("Could not write to temp file!");
						}
						if (!temp.write((uint32*)&currentClause[0], sizeof(uint32), size)) {
							throw std::runtime_error("Could not write to temp file!");
						}
						++numClauses;
						sat = true;
					}
					for (LitVec::iterator it = currentClause.begin(); it != currentClause.end(); ++it) {
						flags[it->var()] &= ~3u; // clear "in clause"-flags
						if (!sat) { 
							// update "in problem"-flags
							flags[it->var()] |= ((1 + it->sign()) << 2);
							nc.add(*it);
						}
					}
					ret = sat || nc.end();
					break;
				}
				else if ( (flags[rLit.var()] & (1+rLit.sign())) == 0 ) {
					currentClause.push_back(rLit);
					flags[rLit.var()] |= 1+rLit.sign();
					if ((flags[rLit.var()] & 3u) == 3u) sat = true;
				}
			}
		}
		else if (match(in, "p ", false)) {
			if (match(in, "cnf", false) || (wcnf=match(in, "wcnf", false)) == true) {
				if (!in.parseInt(numVars, 0, (int)varMax) || !in.parseInt(numClauses, 0, INT_MAX)) {
					ERROR("Bad parameters in the problem line!");
				}
				partial = wcnf && in.parseInt64(top);
				lastVar = numVars;
				if (!partial) {
					top    = 1 - (wcnf || maxSat);
					maxVar = numVars + (int)((1-top)*numClauses);
					flags.reserve(maxVar+1);
					flags.resize(numVars+1);
					ctx.reserveVars(maxVar+1);
					for (int v = 1; v <= maxVar; ++v) {
						ctx.addVar(Var_t::atom_var);
					}
					// prepare solver/preprocessor for adding constraints
					ctx.startAddConstraints(std::min(numClauses, 10000));
				}
				else {
					maxSat = false;
					maxVar = numVars;
					flags.reserve( (maxVar+1) + (numClauses/10) );
					flags.resize(maxVar+1);
					if ( !temp.create() ) {
						throw std::runtime_error("Could not create temp file!");
					}
					numClauses = 0;
				}
				ctx.symTab().startInit();
				ctx.symTab().endInit(SymbolTable::map_direct, numVars+1);
				if (ctx.satPrepro.get() && ctx.satPrepro->limit(numClauses)) {
					// Don't waste time preprocessing a gigantic problem
					p = ctx.satPrepro.release(); 
				}
			}
			else { ERROR("Unrecognized format!"); }
			state = 1;
		}
		else { ERROR("Missing problem line!"); }
	}
	if (partial) {
		maxVar = lastVar;
		assert(flags.size() == static_cast<uint32>(maxVar+1));
		ctx.reserveVars(maxVar + 1);
		for (int v = 1; v <= maxVar; ++v) {
			ctx.addVar(Var_t::atom_var);
		}
		// prepare solver/preprocessor for adding constraints
		ctx.startAddConstraints(numClauses);
		Literal x;
		temp.rewind();
		uint32 size;
		for (int i = 0; i != numClauses && ret; ++i) {
			if (!temp.read(&size, sizeof(uint32), 1)) {
				throw std::runtime_error("Could not read from temp file!");
			}
			currentClause.resize(size);
			if (!temp.read((uint32*)&currentClause[0], sizeof(uint32), size)) {
				throw std::runtime_error("Could not read from temp file!");
			}
			nc.start();
			for (uint32 j = 0; j != size; ++j) {
				x = currentClause[j];
				nc.add(x);
				flags[x.var()] |= ((1 + x.sign()) << 2);
			}
			ret = nc.end();
		}
		temp.destroy();
	}
	if (p) {
		ctx.satPrepro.reset(p);
	}
	if (pure == PureLitMode_t::assert_pure_auto) {
		if (opt.lits.empty()) { pure = PureLitMode_t::assert_pure_yes; }
		else                  { pure = PureLitMode_t::assert_pure_no;  }
	}
	if (pure == PureLitMode_t::assert_pure_yes) {
		for (int i = 1; ret && i <= lastVar && ret; ++i) {
			if ((flags[i] & keep) != keep) {
				ret = ctx.addUnary(Literal(i, ((flags[i]>>2) & 1u) != 1));
			}
		}
	}
	for (int v = lastVar+1; v <= maxVar; ++v) {
		ctx.addUnary(negLit(v));
	}
	return ret;
}

/////////////////////////////////////////////////////////////////////////////////////////
// OPB PARSING
/////////////////////////////////////////////////////////////////////////////////////////
class OPBParser {
public:
	OPBParser(std::istream& in, SharedContext& ctx, ObjectiveFunction& obj) 
		: source_(in)
		, ctx_(ctx)
		, obj_(obj)
		, nlc_(0) {
		obj_.lits.clear();
	}
	~OPBParser() { delete nlc_; }
	bool parse();
private:
	OPBParser(const OPBParser&);
	OPBParser& operator=(const OPBParser&);
	bool good() const   { return !ctx_.master()->hasConflict(); }
	void skipComments() { while (*source_ == '*') skipLine(source_); }
	void parseHeader();
	void parseOptObjective();
	void parseConstraint();
	void parseSum();
	void parseTerm(Literal& out);
	void addConstraint(const char* relOp, int rhs, uint32 var = 0);
	bool finalize();
	struct NLCData {
		typedef std::multimap<uint32, uint32> ProductIndex;
		typedef std::pair<ProductIndex::const_iterator, ProductIndex::const_iterator> ProductRange;
		explicit NLCData(unsigned lits) {
			productLits.reserve(lits+1);
			// Begin/End marker
			productLits.push_back(posLit(0));
			productLits.back().watch();
		}
		bool getProduct(const LitVec& term, unsigned hash, Literal& out) const;
		void addProduct(const LitVec& term, unsigned hash, Literal in);
		ProductIndex productIndex;
		LitVec       productLits;
	};
	StreamSource       source_;
	WeightLitVec       lhs_;
	LitVec             term_;
	SharedContext&     ctx_;
	ObjectiveFunction& obj_;
	NLCData*           nlc_;
	int                numVars_, products_, soft_, min_, max_;
	int                nextVar_;
	int                maxVar_;
	int                adjust_;
};

bool OPBParser::parse() {
	parseHeader();
	skipComments();
	parseOptObjective();
	skipComments();
	while (*source_ && good()) {
		parseConstraint();
		skipComments();
	}
	return good() && finalize();
}

// * #variable= int #constraint= int [#product= int sizeproduct= int] [#soft= int mincost= int maxcost= int sumcost= int]
// where [] indicate optional parts, i.e.
//  LIN-PBO: * #variable= int #constraint= int
//  NLC-PBO: * #variable= int #constraint= int #product= int sizeproduct= int
//  LIN-WBO: * #variable= int #constraint= int #soft= int mincost= int maxcost= int sumcost= int
//  NLC-WBO: * #variable= int #constraint= int #product= int sizeproduct= int #soft= int mincost= int maxcost= int sumcost= int
void OPBParser::parseHeader() {
	StreamSource& in = source_;
	numVars_ = products_ = soft_ = min_ = max_ = 0;
	int64 sum= 0;
	delete nlc_; nlc_ = 0;
	const int MAX     = INT_MAX;
	if (!match(in, "* #variable=", false)){ ERROR("Missing problem line \"* #variable=\""); }
	in.skipWhite();
	if (!in.parseInt(numVars_,0,varMax))  { ERROR("Number of vars expected\n"); }
	if (!match(in, "#constraint=", true)) { ERROR("Bad problem line. Missing \"#constraint=\""); }
	int numCons;
	if (!in.parseInt(numCons, 0, MAX))    { ERROR("Number of constraints expected\n"); }
	if (match(in, "#", true)) {
		const char* next = "soft=";
		if (match(in, "product=", true))    { 
			if (!in.parseInt(products_,0,MAX)){ ERROR("Number of products expected\n"); }
			next = "sizeproduct=";
			if (!match(in, next,true))        { ERROR("'sizeproduct=' expected\n"); }
			int prodLits = 0;
			if (!in.parseInt(prodLits,0,MAX)) { ERROR("Number of product literals expected\n"); }
			nlc_ = new NLCData(prodLits+products_);
			next = "#soft=";
		}
		if (match(in, next, true))          {
			if (!in.parseInt(soft_,0,MAX))    { ERROR("Number of soft constraints expected\n"); }
			if (!match(in, "mincost=", true)) { ERROR("'mincost=' expected\n"); }
			if (!in.parseInt(min_))           { ERROR("Minimal cost expected\n"); }
			if (!match(in, "maxcost=", true)) { ERROR("'maxcost=' expected\n"); }
			if (!in.parseInt(max_))           { ERROR("Maximal cost expected\n"); }
			if (!match(in, "sumcost=", true)) { ERROR("'sumcost=' expected\n"); }
			if (!in.parseInt64(sum))          { ERROR("Sum of soft constraints cost expected\n"); }
		}
	}
	if (!matchEol(in, true))              { ERROR("Unrecognized characters in problem line\n"); }
	
	// Init solver
	maxVar_  = numVars_ + soft_ + products_;
	nextVar_ = numVars_;
	ctx_.reserveVars(maxVar_+1);
	ctx_.symTab().startInit();
	for (int v = 1; v <= maxVar_; ++v) {
		ctx_.addVar(Var_t::atom_var);
	}
	ctx_.symTab().endInit(SymbolTable::map_direct, numVars_+1);
	ctx_.startAddConstraints();
}

// <objective>::= "min:" <zeroOrMoreSpace> <sum>  ";"
// OR
// <softobj>  ::= "soft:" [<unsigned_integer>] ";"
void OPBParser::parseOptObjective() {
	StreamSource& in = source_;
	obj_.lits.clear();
	obj_.adjust = 0;
	obj_.rhs    = -1;
	if (match(source_, "min:", true)) {
		source_.skipWhite();
		parseSum();
		addConstraint(0, adjust_);
	}
	else if (match(in, "soft:", true)){
		if (!in.parseInt64(obj_.rhs))  { ERROR("Top cost expected!\n"); }
		if (obj_.rhs < 0)              { ERROR("Top cost must be positive!\n"); }
		if (!match(in, ';', true))     { ERROR("Semicolon missing after constraint\n"); }
		in.skipWhite();
	}
}

// <constraint>::= <sum> <relational_operator> <zeroOrMoreSpace> <integer> <zeroOrMoreSpace> ";"
// OR
// <softconstr>::= "[" <zeroOrMoreSpace> <unsigned_integer> <zeroOrMoreSpace> "]" <constraint>
void OPBParser::parseConstraint() {
	StreamSource& in = source_;
	Var            x = 0;
	if (match(in, '[', true)) {
		int cost = -1;
		if (!in.parseInt(cost,min_,max_) || !match(in, "]", true)) {
			ERROR("Invalid soft constraint\n");
		}
		if (--soft_ < 0) { ERROR("Too many soft constraints\n"); }
		x = ++nextVar_;
		assert(ctx_.validVar(x));
		obj_.lits.push_back(WeightLiteral(negLit(x), cost));
	}
	parseSum();
	const char* relOp;	
	if (!match(in, (relOp="="), true) && !match(in, (relOp=">="), false)) {
		ERROR("Relational operator expected\n");
	}
	in.skipWhite();
	int coeff;
	if (!in.parseInt(coeff))   { ERROR("Missing coefficient on rhs of constraint\n"); }
	if (!match(in, ';', true)) { ERROR("Semicolon missing after constraint\n"); }
	in.skipWhite();
	if (x == 0 || lhs_.size() > 1 || lhs_[0].second != coeff+adjust_) {
		addConstraint(relOp, coeff+adjust_, x);
	}
	else {
		obj_.lits.back().first = ~lhs_[0].first;
		--nextVar_;
	}
}

// <sum>::= <weightedterm> | <weightedterm> <sum>
// <weightedterm>::= <integer> <oneOrMoreSpace> <term> <oneOrMoreSpace>
void OPBParser::parseSum() {
	StreamSource& in = source_;
	int  coeff;
	adjust_ = 0;
	Literal x;
	lhs_.clear();
	while (!match(in, ';', true)) {
		if (!in.parseInt(coeff)) { ERROR("Coefficient expected\n"); }
		parseTerm(x);
		if (coeff < 0) {
			coeff   = -coeff;
			adjust_+= coeff;
			x       = ~x;
		}
		lhs_.push_back(WeightLiteral(x, coeff));
		if (*in == '>' || *in == '=') break;
		in.skipWhite();
	}
	in.skipWhite();
}

// <term>::=<variablename>
// OR
// <term>::= <literal> | <literal> <space>+ <term>
void OPBParser::parseTerm(Literal& out) {
	StreamSource& in = source_;
	term_.clear();
	int  var, last = 0;
	bool sign, sorted = true;
	unsigned hash = 0;
	do {
		match(in, '*', true);         // optionally
		sign  = match(in, '~', true); // optionally
		if (!match(in, 'x', true) || !in.parseInt(var)) { ERROR("Identifier expected\n"); }
		if (var <= 0)       { ERROR("Identifier must be strictly positive\n"); }
		if (var > numVars_) { ERROR("Identifier out of bounds\n"); }
		term_.push_back(Literal(var, sign));
		sorted &= (var > last);
		hash += !sign ? hashId(static_cast<unsigned>(var)) : hashId(static_cast<unsigned>(-var));
		last = var;
		in.skipWhite();
	} while (*in == '*' || *in == '~' || *in == 'x');
	if (term_.size() == 1) {
		out = term_[0];
	}
	else {
		assert(nlc_);
		if (!sorted) {
			std::sort(term_.begin(), term_.end());
			LitVec::size_type j  = 1;
			LitVec::size_type i  = 0;
			LitVec::size_type end= term_.size(); 
			while (j != end) {
				if (term_[i].var() != term_[j].var()) {
					term_[++i] = term_[j++];
				}
				else if (term_[i] == term_[j]) {
					++j;
				}
				else { 
					out = negLit(0);
					return;
				}
			}
			shrinkVecTo(term_, ++i);
		}
		if (!nlc_->getProduct(term_, hash, out)) {
			out = posLit(++nextVar_);
			assert(ctx_.validVar(out.var()));
			nlc_->addProduct(term_, hash, out);
		}
	}
}

bool OPBParser::NLCData::getProduct(const LitVec& term, unsigned hash, Literal& out) const {
	for (ProductRange r = productIndex.equal_range(hash); r.first != r.second; ++r.first) {
		uint32 j = r.first->second;
		uint32 i = 0;
		for (; i != term.size(); ++i, ++j) {
			if (term[i] != productLits[j]) break;
		}
		if (i == term.size() && productLits[j].watched()) {
			out = productLits[j];
			out.clearWatch();
			return true;
		}
	}
	return false;
}

void OPBParser::NLCData::addProduct(const LitVec& term, unsigned hash, Literal in) {
	uint32 newPos = (uint32)productLits.size();
	productLits.insert(productLits.end(), term.begin(), term.end());
	in.watch();
	productLits.push_back(in);
	productIndex.insert(ProductIndex::value_type(hash, newPos));
}

bool OPBParser::finalize() {
	if (nlc_) {
		nlc_->productIndex.clear();
		LitVec::size_type i = nlc_->productLits.size();
		ClauseCreator gc(ctx_.master()), bc(ctx_.master());
		while (i > 1) {
			Literal B = nlc_->productLits[--i];
			assert(B.watched()); 
			B.clearWatch();
			gc.start();
			gc.add(B);
			for (Literal x; !(x = nlc_->productLits[i-1]).watched(); --i) {
				if (!bc.start().add(~B).add(x).end()) return false;
				gc.add(~x);
			}
			if (!gc.end()) return false;
		}
	}
	while (nextVar_ < maxVar_) {
		ctx_.addUnary(negLit(++nextVar_));
	}
	return true;
}

#undef ERROR

void OPBParser::addConstraint(const char* relOp, int rhs, uint32 var) {
	if (good()) {
		if (relOp != 0) {
			if (relOp[0] == '>') {
				assert(relOp[1] == '=');
				// var == [lhs >= rhs]
				WeightConstraint::newWeightConstraint(ctx_, posLit(var), lhs_, rhs);	
			}
			else {
				assert(relOp[0] == '=');
				// replace var == [lhs = rhs] with
				// var  == [lhs >= rhs]   and
				// ~var == [lhs >= rhs+1]
				WeightLitVec temp(lhs_); // copy because newWeightConstraint() removes assigned literals 
				WeightConstraint::newWeightConstraint(ctx_, posLit(var), lhs_, rhs) &&
				WeightConstraint::newWeightConstraint(ctx_, negLit(var), temp, rhs+1);
			}
		}
		else {
			obj_.lits   = lhs_;	
			obj_.adjust = rhs;
		}
	}	
}
} // end of anonymous namespace


/////////////////////////////////////////////////////////////////////////////////////////
// interface functions
/////////////////////////////////////////////////////////////////////////////////////////
bool parseLparse(std::istream& prg, ProgramBuilder& api) {
	LparseReader reader;
	return reader.parse(prg, api);
}

bool parseDimacs(std::istream& prg, SharedContext& ctx, PureLitMode pm, ObjectiveFunction& o, bool maxSat) {
	return parseDimacsImpl(prg, ctx, pm, o, maxSat);
}

bool parseOPB(std::istream& prg, SharedContext& ctx, ObjectiveFunction& objective) {
	OPBParser parser(prg, ctx, objective);
	return parser.parse();
}

Input::Format detectFormat(std::istream& in) {
	std::istream::int_type x = std::char_traits<char>::eof();
	while (in && (x = in.peek()) != std::char_traits<char>::eof() ) {
		unsigned char c = static_cast<unsigned char>(x);
		if (c >= '0' && c <= '9') return Input::SMODELS;
		if (c == 'c' || c == 'p') return Input::DIMACS;
		if (c == '*')             return Input::OPB;
		if (c == ' ' || c == '\t') { in.get(); continue; }
		break;
	}
	char msg[] = "'c': Unrecognized input format!\n";
	msg[1]     = (char)(unsigned char)x;
	in && x != std::char_traits<char>::eof() 
		? throw ReadError(1, msg)
		: throw ReadError(0, "Bad input stream!\n");
}

StreamInput::StreamInput(std::istream& in, Format f)
	: prg_(in), format_(f)
{ }

bool StreamInput::read(ApiPtr api, uint32 properties) {
	if (format_ == Input::SMODELS) {
		return parseLparse(prg_, *api.api);
	}
	else if (format_ == Input::DIMACS) {
		func_.adjust = 0; func_.rhs = -1;
		PureLitMode p= PureLitMode_t::assert_pure_yes;
		if      ((properties & PRESERVE_MODELS) != 0)        { p = PureLitMode_t::assert_pure_no;   }
		else if ((properties & PRESERVE_MODELS_ON_MIN) != 0) { p = PureLitMode_t::assert_pure_auto; }
		return parseDimacsImpl(prg_, *api.ctx, p, func_, (properties & AS_MAX_SAT) != 0);
	}
	else {
		return parseOPB(prg_, *api.ctx, func_);
	}
}

void StreamInput::addMinimize(MinimizeBuilder& m, ApiPtr api) {
	if (format_ == Input::SMODELS) {
		api.api->addMinimize(m);
	}
	else if (!func_.lits.empty()) {
		wsum_t adjust = func_.adjust > 0 ? -func_.adjust : 0;
		m.addRule(func_.lits, adjust);
		if (func_.rhs != -1) {
			// subtract 1 because minimize constraint uses <= but
			// only solutions with cost < rhs are admissible
			m.setOptimum(0, func_.rhs-1);
		}
	}
}
}
