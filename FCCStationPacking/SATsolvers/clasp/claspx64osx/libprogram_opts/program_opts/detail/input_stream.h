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
#ifndef PROGRAM_OPTIONS_INPUT_STREAM_H_INCLUDED
#define PROGRAM_OPTIONS_INPUT_STREAM_H_INCLUDED
#include <istream>
#include <string>
namespace ProgramOptions { namespace detail {

// A primitive input stream buffer for fast extraction from a given string
// NOTE: The input string is NOT COPIED, hence it 
//       MUST NOT CHANGE during extraction
template<class T, class Traits = std::char_traits<T> >
class input_from_string : public std::basic_streambuf<T, Traits> {
	typedef std::basic_streambuf<T, Traits>   base_type;
	typedef typename Traits::char_type*       pointer_type;
	typedef const typename Traits::char_type* const_pointer_type;
	typedef typename base_type::pos_type      pos_type;
	typedef typename base_type::off_type      off_type;
public:
	explicit input_from_string(const_pointer_type p, size_t size)
		: buffer_(const_cast<pointer_type>(p))
		, size_(size) {
		base_type::setp(0, 0); // no write buffer
		base_type::setg(buffer_, buffer_, buffer_+size_); // read buffer
	}
	pos_type seekoff(off_type offset, std::ios_base::seekdir dir, std::ios_base::openmode which) {
		if(which & std::ios_base::out) {
			// not supported!
			return base_type::seekoff(offset, dir, which);
		}
		if(dir == std::ios_base::cur) {
			offset += static_cast<off_type>(base_type::gptr() - base_type::eback());
		}
		else if(dir == std::ios_base::end) {
			offset = static_cast<off_type>(size_) - offset;
		}
		return seekpos(offset, which);
	}
	pos_type seekpos(pos_type offset, std::ios_base::openmode which) {
		if((which & std::ios_base::out) == 0 && offset >= pos_type(0) && ((size_t)offset) <= size_) {
			base_type::setg(buffer_, buffer_+(size_t)offset, buffer_+size_);
			return offset;
		}
		return base_type::seekpos(offset, which);
	}
private:
	input_from_string(const input_from_string&);
	input_from_string& operator=(const input_from_string&);
protected:
	pointer_type buffer_;
	size_t       size_;
};

template<class T, class Traits = std::char_traits<T> >
class input_stream : public std::basic_istream<T, Traits> {
public:
	input_stream(const std::string& str)
		: std::basic_istream<T, Traits>(0)
		, buffer_(str.data(), str.size()) {
		std::basic_istream<T, Traits>::rdbuf(&buffer_);
	}
	input_stream(const char* x, size_t size)
		: std::basic_istream<T, Traits>(0)
		, buffer_(x, size) {
		std::basic_istream<T, Traits>::rdbuf(&buffer_);
	}
private:
	input_from_string<T, Traits> buffer_;
};

} }
#endif
