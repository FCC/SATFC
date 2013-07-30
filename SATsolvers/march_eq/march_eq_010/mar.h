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

#ifndef __MAR_H__
#define __MAR_H__
#include "common.h"

double w2, w3;

void percentmar( int aantalSlagen );
void percentact( int aantalSlagen );
void updateQ(int fixed_nr);
void restoreQ(int fixed_nr);
void updateACT(int fixed_nr);
void restoreACT(int fixed_nr);
void calcMARvalues();

void initMAR( );
void initIndepend( );
void destroyMAR();
void initACT();

int marCompare( const void *prtA, const void *ptrB );
int actCompare( const void *prtA, const void *ptrB );

int getMaxCeqVar();

void mar_add_lit( int lit1, int lit2, int truth );
void mar_remove_lit( int lit1, int lit2, int truth );
void mar_add_cls( int lit1, int lit2, int truth );
void mar_remove_cls( int lit1, int lit2, int truth );


void resetDummyBranch();
int getIndepend();

void setWeight( );
void printMARvalues();

#endif
