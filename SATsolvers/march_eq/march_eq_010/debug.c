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

#include "common.h"
#include "debug.h"

FILE* debugFile=NULL;

int debug_init() {
	int i;

	levelOn[ 0 ] = 1;
	for( i = 1; i < NR_OF_DEBUG_LEVELS; i++ )
		levelOn[ i ] = 0;

#ifdef DEBUG_TO_FILE
	debugFile = fopen( DEBUG_FILE, "w" );
	return ( debugFile != NULL );  
#else
	return 1;
#endif
}

void debug_dispose() {
#ifdef DEBUG_TO_FILE
  if( debugFile ) fclose( debugFile );
#endif
}

