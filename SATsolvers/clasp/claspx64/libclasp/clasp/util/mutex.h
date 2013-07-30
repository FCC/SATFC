// 
// Copyright (c) 2012, Benjamin Kaufmann
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

#ifndef CLASP_UTIL_MUTEX_H_INCLUDED
#define CLASP_UTIL_MUTEX_H_INCLUDED

#if WITH_THREADS
#if _WIN32||_WIN64
#define WIN32_LEAN_AND_MEAN // exclude APIs such as Cryptography, DDE, RPC, Shell, and Windows Sockets.
#define NOMINMAX            // do not let windows.h define macros min and max
#endif
#include <tbb/compat/condition_variable>
#include <tbb/mutex.h>
#include <tbb/spin_mutex.h>
namespace std { using tbb::mutex; }
#else
namespace Clasp { namespace Serial {

class NullMutex {   
public:   
	NullMutex()     {}
	void lock()     {}
	bool try_lock() { return true; }
	void unlock()   {}
private:
	NullMutex(const NullMutex&);   
	NullMutex& operator=(const NullMutex&);   
};
typedef NullMutex mutex; 
typedef NullMutex spin_mutex;
template<typename M>
class lock_guard {
public:
	typedef M mutex_type;
	explicit lock_guard(mutex_type& m) : pm(m) {m.lock();}
	~lock_guard() { pm.unlock(); }
private:
	lock_guard(const lock_guard&);
	lock_guard& operator=(const lock_guard&);
	mutex_type& pm;
};
}}
namespace std { using Clasp::Serial::mutex; using Clasp::Serial::lock_guard; }
namespace tbb { using Clasp::Serial::spin_mutex; }

#endif

#endif
