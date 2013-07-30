/* MARCH II! Satisfiability Solver

   Copyright (C) 2001 M. Dufour, M. Heule, J. van Zwieten
   [m.dufour@student.tudelft.nl, marijn@heule.nl, zwieten@ch.tudelft.nl]

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

*/

#ifndef __DEBUG_H__
#define __DEBUG_H__

#include <stdio.h>

#define DEBUG_FILE "/tmp/flats.log"
//#define DEBUG_TO_FILE

FILE* debugFile;

/* ANSI C Syntax */
/* #define debug( format, ... ) if( debugFile ) fprintf( debugFile, format, __VA_ARGS__ ) */

/* GNU C extension: ... argument can be named ( args in this case ) and the ## deletes
   the comma after format if no arguments are passed. So debug( "Hello World!" ) is allowed.
   See also the manpages for stdarg.h for information on how to create a variadic function */

/* DEBUG levels */
#define	STD	0		// always printed
#define	RC	1		// top-level recursion
#define	LA	2		// lookahead
#define	MAR	3		// mar heuristic
#define	PA	4		// parser
#define	PREP	5		// preprocessor
#define	IC	6		// implication

#define NR_OF_DEBUG_LEVELS	7
int levelOn[ NR_OF_DEBUG_LEVELS ];

#ifdef DEBUGGING 
	#ifdef DEBUG_TO_FILE
		#define DEBUG( level, format, args... ) \
		{ \
			if( debugFile && ( level >= 0 ) && ( level < NR_OF_DEBUG_LEVELS ) && levelOn[ level ] ) \
			{
				fprintf( debugFile, format, ## args ); \
				fflush( debugFile ); \
			} \
		}
	#else
		#define DEBUG( level, format, args... ) \
		{ \
			if( ( level >= 0 ) && ( level < NR_OF_DEBUG_LEVELS ) && levelOn[ level ] ) \
			{ \
				printf( format, ## args ); \
				fflush( stdout ); \
			} \
		}
	#endif
#else
	#define DEBUG( level, format, args... )
#endif

int debug_init();
void debug_dispose();

#endif
