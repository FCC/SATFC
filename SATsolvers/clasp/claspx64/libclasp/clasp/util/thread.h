// 
// Copyright (c) 2010-2012, Benjamin Kaufmann
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

#ifndef CLASP_UTIL_THREAD_H_INCLUDED
#define CLASP_UTIL_THREAD_H_INCLUDED

#if WITH_THREADS
#if _WIN32||_WIN64
#define WIN32_LEAN_AND_MEAN // exclude APIs such as Cryptography, DDE, RPC, Shell, and Windows Sockets.
#define NOMINMAX            // do not let windows.h define macros min and max
#endif
#include <tbb/compat/thread> // replace with std::thread once available
#else
namespace Clasp { namespace Serial {
	struct thread {};
	inline void yield() {}
}}
namespace std { 
	using Clasp::Serial::thread;
	namespace this_thread { using Clasp::Serial::yield; }
}
#endif

#endif
