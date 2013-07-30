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

#include <malloc.h>
#include <stdlib.h>
#include <time.h>

#include "common.h"
#include "debug.h"
#include "vicii.h"
#include "parser.h"
#include "solver.h"
#include "mar.h"
#include "equivalence.h"
#include "progressBar.h"

int main( int argc, char** argv )
{
	int result, exitcode;
        FILE *flup;

#ifndef EQ
	int  _resolventen;
#endif

	/* let's not be too optimistic... :) */
	result = UNKNOWN;
	exitcode = EXIT_CODE_UNKNOWN;

	printf( "c main():: ***                                   [ vicii satisfiability solver ]                                   ***\n" );
	printf( "c main()::  **              Copyright (C) 2001 M. Dufour, M. Heule, J. van Zwieten and H. van Maaren               **\n" );
	printf( "c main()::   * This program may be redistributed and/or modified under the terms of the GNU Gereral Public License *\n" );
	printf( "c main()::\n" );

        if( argc != 2 )
        {
                printf( "c main():: input file missing, usage: vicii < DIMACS-file.cnf >\n" );
                return EXIT_CODE_ERROR;
        }

	#ifdef DEBUGGING
		if( !debug_init() )
		{
			printf( "c main():: debug file %s could not be opened!\n", DEBUG_FILE );
			return EXIT_CODE_ERROR;
		}

		levelOn[ STD ] = 1;
		levelOn[ RC ] = 1;
		levelOn[ LA ] = 1;
		levelOn[ IC ] = 1;
		levelOn[ MAR ] = 1;
		levelOn[ PA ] = 0;
		levelOn[ PREP ] = 0;
	#endif

	/*
		Parsing...
	*/
	runParser( argv[ 1 ] );

        printf( "c preprocessing fase I completed:: there are now %i free variables and %i clauses.\n", freeNrofvars, nrofclauses );

	/*
		Preprocessing...
	*/

//        addResolvents();

	if( eqProduct() == UNSAT )
	{
	        flup = fopen("results.py","w");
	        fprintf(flup,"nodes=%d\n",nodeCount);
	        fprintf(flup,"lookaheads=%d\n", lookAheadCount);
	        fprintf(flup,"time=%f\n", ((double)(clock()))/((double)(CLOCKS_PER_SEC)) );
	        fclose(flup);

       	        printf( "s UNSATISFIABLE\n" );
                exit( EXIT_CODE_UNSAT);
	}

	subst_tri_to_bieq();
	propagate_bieq();
/*
	if ( !simplifyFormula() )
	{
		printf( "c runParser():: conflicting unary clauses, so instance is unsatisfiable!\n" );
        	printf( "s UNSATISFIABLE\n" );
		exit( EXIT_CODE_UNSAT );
	}
*/

#ifdef PRINT_FORMULA
	printCeq();
	printCNF( Cv );
//	exit(0);
#endif

	lessRedundantTransformation();

#ifdef RESOLVENT

#ifdef EQ
        addResolvents();
	sortCv( 0, NULL, NULL );
#else
        do
        {
        	_resolventen = addResolvents();
        }
        while ( ( _resolventen != sortCv( 0, NULL, NULL ) ) && (_resolventen < _CvSize) );
#endif
        printf( "c preprocessing fase II completed:: there are now %i free variables and %i clauses.\n", freeNrofvars, nrofclauses );
#endif

        transformTo3SAT();

        printf( "c main():: clause / variable ratio: ( %i / %i ) = %.2f\n", nrofclauses, nrofvars, (double) nrofclauses / nrofvars );

	lookaheadArray = (int*) malloc( sizeof( int ) * 2 * nrofvars );

        nodeCount = 0;
        lookAheadCount = 0;
        unitResolveCount = 0;
	naCounter = 0;
	naCounter2 = 0;
	sateqCounter = 0;
	depth = 0;

	if( initSolver() )
	{
		printf( "c main():: all systems go!\n" );

#ifdef PROGRESS_BAR
		pb_init( 6 );
#endif

		result = marsolverec();

#ifdef PROGRESS_BAR
		pb_dispose();
#endif
	}
	else
	{
		printf( "c main():: conflict caused by unary equivalence clause found.\n" );
		result = UNSAT;
	}

        printf( "c main():: nodeCount: %i\n", nodeCount );
	printf( "c main():: dead ends in main: %i\n", mainDead );
        printf( "c main():: lookAheadCount: %i\n", lookAheadCount );
        printf( "c main():: unitResolveCount: %i\n", unitResolveCount );
        printf( "c main():: time=%f\n", ((float)(clock()))/CLOCKS_PER_SEC );
	printf( "c main():: naCounter2: %i\n", naCounter2 );
	printf( "c main():: pre-satisfied equivalences: %i \n", sateqCounter );

        flup = fopen("results.py","w");
        fprintf(flup,"nodes=%d\n",nodeCount);
        fprintf(flup,"lookaheads=%d\n", lookAheadCount);
        fprintf(flup,"time=%f\n", ((double)(clock()))/((double)(CLOCKS_PER_SEC)) );
        fclose(flup);

	switch( result )
	{
		case SAT:
			printf( "c main():: SOLUTION VERIFIED :-)\n" );
			printf( "s SATISFIABLE\n" );
			printSolution( originalNrofvars );
			exitcode = EXIT_CODE_SAT;
			break;

		case UNSAT:
        	        printf( "s UNSATISFIABLE\n" );
	                exitcode = EXIT_CODE_UNSAT;
			break;

		default:
			printf( "s UNKNOWN\n" );
			exitcode = EXIT_CODE_UNKNOWN;
        }

	disposeSolver();

	free( lookaheadArray );

	disposeFormula();

#ifdef DEBUGGING
	debug_dispose();
#endif

        return exitcode;
}

void runParser( char* fname )
{
	FILE* in;
	int *_fstack, *_fstackp;
	int bieq, bieqtotal;

	if( ( in = fopen( fname, "r" ) ) == NULL )
	{
		printf( "c runParser():: input file could not be opened!\n" );
		#ifdef DEBUGGING
			debug_dispose();
		#endif
		exit( EXIT_CODE_ERROR );
	}

	if( !initFormula( in ) )
	{
		printf( "c runParser():: p-line not found in input, but required by DIMACS format!\n" );
		fclose( in );
		#ifdef DEBUGGING
			debug_dispose();
		#endif
		exit( EXIT_CODE_ERROR );
	}
	
        /* allocate fix stack */
        /* there can be max. nrofclauses 1-clauses, thus this should be enough... */
        _fstack = (int*) malloc( sizeof( int ) * nrofclauses );
        _fstackp = _fstack;

	if( !parseCNF( in, _fstack, &_fstackp ) )
        {
                printf( "c runParser():: parse error in input!\n" );
		fclose( in );
		free( _fstack );
		#ifdef DEBUGGING
			debug_dispose();
		#endif
                exit( EXIT_CODE_ERROR );
        }

	fclose( in );

	printf( "c runParser():: parsing was successful, warming up engines...\n" );

	init_equivalence();

	bieqtotal = 0;
	
	do
	{
		do
		{
		        if( !purgeUnaryClauses( _fstack, &_fstackp ) )
			{
				printf( "c runParser():: conflicting unary clauses, so instance is unsatisfiable!\n" );
		        	printf( "s UNSATISFIABLE\n" );
	
				disposeFormula();
				#ifdef DEBUGGING
					debug_dispose();
				#endif
				exit( EXIT_CODE_UNSAT );
			}

		        _fstackp = _fstack;

			sortCv( 1, _fstack, &_fstackp );
	        }
		while( _fstackp != _fstack );
		
		bieq = replaceEq();	
		bieqtotal += bieq;
	}
	while( bieq > 0 );
	printf("c found %i bi-equivalences\n", bieqtotal );

	free( _fstack );
}

int simplifyFormula( )
{
	int *_fstack, *_fstackp;

//	int bieq, bieqtotal;
//	bieqtotal = 0;

        _fstack = (int*) malloc( sizeof( int ) * nrofclauses );
        _fstackp = _fstack;


	sortCv( 1, _fstack, &_fstackp );

        if( !purgeUnaryClauses( _fstack, &_fstackp ) )
		return UNSAT;
/*
	do
	{
		do
		{
		        if( !purgeUnaryClauses( _fstack, &_fstackp ) )
				return UNSAT;

		        _fstackp = _fstack;

			sortCv( 1, _fstack, &_fstackp );
	        }
		while( _fstackp != _fstack );
		
		bieq = replaceEq();	
		bieqtotal += bieq;
	}
	while( bieq > 0 );
	printf("c found %i bi-equivalences\n", bieqtotal );
*/
	free( _fstack );

	return 1;
}
