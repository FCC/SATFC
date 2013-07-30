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

#ifndef __LOOKAHEAD_H__
#define __LOOKAHEAD_H__

void initLookahead();
void destroyLookahead();
int impParent( int current );
int impChild( const int derived );
int prop3Neg( const int nrval );
int impCurrent( int current );
int impNA( const int current );
int lookahead();
int intellook();
int treelookvar( const int nrval );

int propCeq( int varnr );
int propImp( int varnr );
//void propImp( int varnr );

void get_NA( int **_NA, int *_NAsize );
int get_maxDiffVar();
int get_maxIntellook();

void cleanLookahead();
void cleanFormula();

/*
void setTimeStamp( int nrval );
void unsetTimeStamp( int nrval );

void setClause( int clidx );
void unsetClause( int clidx );
*/

#endif
