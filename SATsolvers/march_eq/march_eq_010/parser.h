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

#ifndef __PARSER_H__
#define __PARSER_H__

#include <stdio.h>
#include "common.h"


/*
	Parsing...
*/
int initFormula( FILE* in );
int parseCNF( FILE* in, int* _fstack, int** _fstackp );
int purgeUnaryClauses( int* _fstack, int** _fstackp );
void disposeFormula();


/*
	Preprocessing...
*/
int sortCv( int useStack, int *_fstack, int **_fstackp );
int replaceEq();
int eqProduct();
void transformTo3SAT();
void lessRedundantTransformation();
int addResolvents();


/*
	Debugging...
*/
#ifdef DEBUGGING
	void printCNF( const CNF *cnf );
	void printFormula( const CNF *cnf );
	void printDIMACSFormula( const CNF *cnf );
#endif

#endif

