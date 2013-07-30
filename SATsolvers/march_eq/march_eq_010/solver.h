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

#ifndef __SOLVER_H__
#define __SOLVER_H__

#include "common.h"

int initSolver();
void disposeSolver();

void centerPtrs();

int verifySolution();
int marsolverec();
int unitresolve( int nrval );
void backtrack();
int get_direction( int nrval );
void printSolution( int orignrofvars );

int fixonevar( int nrval );
void unfixonevar( int nrval );

int addImplication( int var1, int var2 );
int add_ceq_imp( int varnr );
int add_cvlong_imp( int varnr );

int full_lookahead();

int newNA( int nrval );

void fillIcOne();
void printCv();

#ifdef DEBUGGING
void printIc();
void printIIc();
void printCvII();
void printVc();
void printStack();
void printFixStack();
void printChecksum();
void printDeHeleHandel();
#endif

#endif
