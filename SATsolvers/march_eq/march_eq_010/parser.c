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

#include <stdio.h>
#include <stdlib.h>
#include <malloc.h>

#include "common.h"
#include "debug.h"
#include "parser.h"
#include "memory.h"
#include "equivalence.h"

#define VAR3SAT( __a )	( __a + nrof3SATvars )
#define VARPRE( __a )	( __a + nrofvars )
#define VARLRT( __a )	( __a + nrofLRTvars )

#define MAXPAIR		1
#undef CORRECT_CLAUSE_LENGTH
#define CVFACTOR	2
/*
************************************************************************
*  CNF init and deinit, parsing and unary clause removal.              *
************************************************************************
*/

/*
	MALLOCS: 	-
	REALLOCS:	-
	FREES:	 	-
*/
int initFormula( FILE* in )
{
	int result, i;

	/*
		initialize global data structure.
	*/
	originalNrofvars = 0;
	originalNrofclauses = 0;
	nrofvars = 0;
	nrofclauses = 0;

	Cv = NULL;
	Clength = NULL;
	Vc = NULL;
	Ic = NULL;
	IcLength = NULL;
	
	timeAssignments = NULL;
	timeValues = NULL;

	VeqDepends = NULL;
	/*
		initialization done.
	*/

	printf( "c initFormula():: searching for DIMACS p-line....\n" );

	/*
		search for p-line in DIMACS format
	*/
	do
	{	
		result = fscanf( in, " p cnf %i %i \n", &( originalNrofvars ), &( originalNrofclauses ) );
		if( result > 0 && result != EOF )
			break;

		result = fscanf( in, "%*s\n" );
	}
	while( result != 2 && result != EOF );

	if( result == EOF || result != 2 )
	{
		return 0;
	}

	nrofvars = originalNrofvars;
	nrofclauses = originalNrofclauses;

	/*
		Number of free variables.
	*/
	freeNrofvars = nrofvars;
	_CvSize = nrofclauses * CVFACTOR;

	VeqDepends = (int*) malloc( sizeof( int ) * ( nrofvars + 1 ) );
	for( i = 0; i <= nrofvars; i++ )
		VeqDepends[ i ] = 0;

	printf( "c initFormula():: the DIMACS p-line indicates a CNF of %i variables and %i clauses.\n", nrofvars, nrofclauses );
	return 1;
}


/*
	MALLOCS: 	-
	REALLOCS:	-
	FREES:	 	Cv[ * ], Cv, Clength, Vc[ * ], Vc, Ic[ * ], Ic, IcLength,
			timeAssignments, timeValues, VeqDepends
*/
void disposeFormula()
{
	int i;

	/*
		Can also be used to delete a partial formula, because Cv[ i ]
		is initialized to NULL.
		IMPORTANT: according to 'man free', free( void *ptr ) does noting 
		iff ( ptr == NULL ). This behaviour is vital to disposeFormula() and
		other parts of the solver where memory is freed.
	*/
	if( Cv != NULL )
	{
		for( i = 0; i < nrofclauses; i++ )
			free( Cv[ i ] );
		free( Cv );
	}
	free( Clength );
	
	if( Ic != NULL )
	{
		for( i = 0; i < ( 2 * nrofvars + 1 ); i++ )
			free( Ic[ i ] );
		free( Ic );
	}
	free( IcLength );

	/*
		IMPORTANT: timeAssignments and timeValues should be corrected before
 		attempting this. (In the lookahead, nrofvars is added to both pointers
		to speed up indexing.) Neglecting this coorection means Segfault Suicide!
	*/
	free( timeAssignments );
	free( timeValues );

	free( VeqDepends );

	/*
		Update cnf structure.
	*/
	originalNrofvars = 0;
	originalNrofclauses = 0;
	nrofvars = 0;
	nrofclauses = 0;

	Cv = NULL;
	Clength = NULL;
	Vc = NULL;
	Ic = NULL;
	IcLength = NULL;
	
	timeAssignments = NULL;
	timeValues = NULL;

	VeqDepends = NULL;
}


/*
	MALLOCS: 	_clause, Cv, Cv[], Clength, timeAssignments, timeValues
	REALLOCS:	-
	FREES:	 	_clause
*/
int parseCNF( FILE* in, int* _fstack, int** _fstackp )
{
	int *_clause, clen, _lit;
	int i, j, error;

	printf( "c parseCNF():: parsing....\n" );

	/* 
		Allocate buffer to hold clause. A clause can never
		be longer than nrofvars, for obvious reasons.
	*/
	_clause = (int*) malloc( sizeof( int ) * nrofvars );

	/* INIT GLOBAL DATASTRUCTURES!! */
	Cv = (int**) malloc( sizeof( int* ) * nrofclauses );
	for( i = 0; i < nrofclauses; i++ )
		Cv[ i ] = NULL;

	/* Clength: length of clause i */
	Clength = (int*) malloc( sizeof( int ) * nrofclauses );

	/* preBieq */
	preBieq = (int*) malloc( sizeof( int ) * 2 * nrofvars );

	/* timeAssignments & timeValues */
	timeAssignments = (tstamp*) malloc( sizeof( tstamp ) * ( nrofvars + 1 ) );
	timeValues = (int*) malloc( sizeof( int ) * ( nrofvars + 1 ) );
	for( i = 0; i < ( nrofvars + 1 ); i++ )
	{
		timeAssignments[ i ] = 0;
		timeValues[ i ] = 0;
	}

	i = clen = error = 0;
	while( i < nrofclauses && !error )
	{
		error = ( fscanf( in, " %i ", &_lit ) != 1 );

		if( !error )
		{
			if( _lit == 0 )
			{
				if( clen == 0 )
				{
					/* a zero-length clause is not good! */
					printf( "c parseCNF():: zero length clause found in input!\n" );
					error = 1;
				}
				else if( clen == 1 )
				{
					/* a unary clause, skip but push literal on stack to fix later */
					*( ( *_fstackp )++ ) = _clause[ 0 ];
					nrofclauses--;
					clen = 0;
				}
				else
				{
					Cv[ i ] = (int*) malloc( sizeof( int ) * clen );
					Clength[ i ] = clen;
					for( j = 0; j < clen; j++ ) Cv[ i ][ j ] = _clause[ j ];
					clen = 0;
					i++;
				}
			}
			else
			{
				if( clen < nrofvars )
				{
					_clause[ clen++ ] = _lit;
				}
				else
				{
					printf( "c parseCNF():: clause length exceeds total number of variables in this CNF.\n" );
					error = 1;
				}
			}
		}
	}


	/* free clause buffer */
	free( _clause );

	if( !error )
	{
		printf( "c parseCNF():: the CNF contains %i unary clauses.\n", ( *_fstackp - _fstack ) );

		#ifdef DEBUGGING
			printFormula( cnf );
		#endif
	}
	else
	{
		disposeFormula();
	}

	return !error;
}


/*
	MALLOCS: 	_Vc, _Vc[], _VcTemp
	REALLOCS:	-
	FREES:	 	_VcTemp, _Vc[ * ], _Vc
*/
int purgeUnaryClauses( int *_fstack, int **_fstackp )
{
        int i, j, nr, nrval, varnr, *purgep, clsidx;
        int **_Vc, *_VcTemp;

	/*
		Create a temporary Vc.
	*/
        _Vc = (int**) malloc( sizeof( int* ) * ( 2 * nrofvars + 1 ) );
        _VcTemp = (int*) malloc( sizeof( int ) * ( 2 * nrofvars + 1 ) );
        for( i = 0; i < ( 2 * nrofvars + 1 ); i++ ) _VcTemp[ i ] = 1;

        for( i = 0; i < nrofclauses; i++ )
                for( j = 0; j < Clength[ i ]; j++ )
                        _VcTemp[ VARPRE( Cv[ i ][ j ] ) ]++;

	/*
		Allocate space...
	*/
        for( i = 0; i < ( 2 * nrofvars + 1 ); i++ )
        {
                _Vc[ i ] = (int*) malloc( sizeof( int ) * _VcTemp[ i ] );
                _Vc[ i ][ 0 ] = _VcTemp[ i ] - 1;

                _VcTemp[ i ] = 1;
        }

        for( i = 0; i < nrofclauses; i++ )
                for( j = 0; j < Clength[ i ]; j++ )
                {
                        varnr = VARPRE( Cv[ i ][ j ] );
                        _Vc[ varnr ][ _VcTemp[ varnr ] ] = i;
                        _VcTemp[ varnr ]++;
                }

	free( _VcTemp );

	purgep = _fstack;
	
	while( purgep < *_fstackp )
	{
		nrval = *( purgep++ );
		nr = NR( nrval );
		varnr = VARPRE( nrval );

	        /*
			Check if variable is already fixed and act accordingly...
		*/
	        if( timeAssignments[ nr ] == VARMAX )
		{
	                if( timeValues[ nr ] == -nrval )
			{
				/*
					Conflicting unary clauses ->
					Free temporary allocated space and return.
				*/
				for( i = 0; i < ( 2 * nrofvars + 1 ); i++ )
					free( _Vc[ i ] );
				free( _Vc );

				return 0;
			}
		}
		else
		{
		        /*
			  	From now on this variable is fixed!
			*/
			freeNrofvars--;
		        timeAssignments[ nr ] = VARMAX;
		        timeValues[ nr ] = nrval;
			
			/*
				All clauses containing varnr are satisfied.
				They are removed from the CNF by setting Clength = 0.
			*/
		        for( i = 1; i <= _Vc[ varnr ][ 0 ] ; i++ )
		        	Clength[ _Vc[ varnr ][ i ] ] = 0;
			
			/*
				All clauses containing ~varnr are shortened by removing
				~varnr from the clause. If this operation results in a unary clause,
				then this clause is removed from the CNF by setting Clength = 0 and
				the literal is pushed on the fix stack to be fixed later.
			*/
		        for( i = 1; i <= _Vc[ VARPRE( -nrval ) ][ 0 ]; i++ )
		        {
				clsidx = _Vc[ VARPRE( -nrval ) ][ i ];
		                for( j = 0; j < Clength[ clsidx ]; j++ )
		                {
		                        if( Cv[ clsidx ][ j ] == -nrval )
		                        {
		                                /*
							Swap literal to the front of the clause.
						*/
		                                Cv[ clsidx ][ j-- ] = Cv[ clsidx ][ --( Clength[ clsidx ] ) ];
		                                if( Clength[ clsidx ] == 1 )
		                                {
		                                        *( ( *_fstackp )++ ) = Cv[ clsidx ][ 0 ];
		                                        Clength[ clsidx ] = 0;
		                                        break;
		                                }
		                        }
		                }
			}
		}
	}

	/*
		Free temporary allocated space.
	*/
	for( i = 0; i < ( 2 * nrofvars + 1 ); i++ )
		free( _Vc[ i ] );
	free( _Vc );

	return 1;
}


/*
	MALLOCS: 	-
	REALLOCS:	Cv[ .. ], Cv, Clength
	FREES:	 	Cv[ .. ]
*/
void compactCNF()
{
	int i, j, clen;

	j = 0;
	for( i = 0; i < nrofclauses; i++ )
	{
		clen = Clength[ i ];
		if( clen == 0 )
		{
			free( Cv[ i ] );
			Cv[ i ] = NULL;
			Clength[ i ] = 0;
		}
		else
		{
			if( i != j )
			{
				Cv[ j ] = Cv[ i ];
				Clength[ j ] = Clength[ i ];
			}

#ifdef CORRECT_CLAUSE_LENGTH
			Cv[ j ] = (int*) realloc( Cv[ j ], sizeof( int ) * clen );
#endif
			j++;
		}
	}
	nrofclauses = j;
	Cv = (int**) realloc( Cv, sizeof( int* ) * nrofclauses );
	Clength = (int*) realloc( Clength, sizeof( int ) * nrofclauses );
}


/*
************************************************************************
*  Clause sorting...                                                   *
************************************************************************
*/

/*
	MALLOCS: 	-
	REALLOCS:	-
	FREES:	 	-
*/
int clsCompare( const void *ptrA, const void *ptrB )
{
	int i;

	/*
		All clauses have minimal length 2. So first compare the first 2 _variables_.
	*/
	if( NR( *( *(int **) ( ptrA ) + 1 ) ) != NR( *( *(int **) ( ptrB ) + 1 ) ) )
		return ( NR( *( *(int **) ( ptrA ) + 1 ) ) - NR( *( *(int **) ( ptrB ) + 1 ) ) > 0 ? -1 : 1 );

	if( NR( *( *(int **) ( ptrA ) + 2 ) ) != NR( *( *(int **) ( ptrB ) + 2 ) ) )
		return ( NR( *( *(int **) ( ptrA ) + 2 ) ) - NR( *( *(int **) ( ptrB ) + 2 ) ) > 0 ? -1 : 1 );

	/*
		Now compare the lengths of the clauses.
	*/
	if( **(int **) ptrA != **(int **) ptrB )
		return ( **(int **) ptrA - **(int **) ptrB > 0 ? -1 : 1 ); 

	/*
		The lengths of A and B are the same and the first 2 _variables_ are also
		the same. So now we take a look at the other _variables_ in the clauses.
	*/
	for( i = 3; i <= **(int **) ptrA; i++ )
		if( NR( *( *(int **) ( ptrA ) + i ) ) != NR( *( *(int **) ( ptrB ) + i ) ) )
			return ( NR( *( *(int **) ( ptrA ) + i ) ) - NR( *( *(int **) ( ptrB ) + i ) ) > 0 ? -1 : 1 );
	/*
		If the two clauses contain the same _variables_, we then consider them as
		literals and compare again. ( So, no NR() is used here ). This is done to make
		removal of duplets easy.
	*/
	for( i = 1; i <= **(int **) ptrA; i++ )
		if( *( *(int **) ( ptrA ) + i ) != *( *(int **) ( ptrB ) + i ) )
			return ( *( *(int **) ( ptrA ) + i ) - *( *(int **) ( ptrB ) + i ) > 0 ? -1 : 1 );

	/*
		Default value if all is equal. ( Thus a duplet... )
	*/
	return 1;
}


/*
	MALLOCS: 	tmpcls, Clength
	REALLOCS:	Cv, Clength
	FREES:	 	Cv[ .. ], Clength
*/
int sortCv( int useStack, int *_fstack, int **_fstackp )
{
	int i, j, k, tmp, taut;
	int *tmpcls, clen, flag, _nrofclauses;
	
	_nrofclauses = nrofclauses;
	compactCNF();
	for( i = 0; i < nrofclauses; i++ )
	{
		clen = Clength[ i ];
		tmpcls = (int*) malloc( sizeof( int ) * ( clen + 1 ) );
		tmpcls[ 0 ] = clen;

		for( j = 0; j < clen; j++ )
			tmpcls[ j + 1 ] = Cv[ i ][ j ];

		free( Cv[ i ] );
		Cv[ i ] = tmpcls;
	}
	free( Clength );
	Clength = NULL;

	/*
		Bubble sort all _variables_ in all clauses of the CNF.
	*/
	taut = 0;
	for( i = 0; i < nrofclauses; i++ )
	{
		int *clause = Cv[ i ];
		for( k = 0; k <= ( clause[ 0 ] - 2 ); k++ )
			for( j = 2; j <= ( clause[ 0 ] - k ); j++ )
	   		{
				if( NR( clause[ j - 1 ] ) > NR( clause[ j ] ) )
				{
					tmp = clause[ j - 1 ];
 					clause[ j - 1 ] = clause[ j ];
					clause[ j ] = tmp;
				}
				else if( NR( clause[ j - 1 ] ) == NR( clause[ j ] ) ) 
				{
					/*
						Double literal? -> swap it out of the clause.
					*/
					if( clause[ j - 1 ] == clause[ j ] )
						clause[ --j ] = clause[ clause[ 0 ]-- ] ;
					else
					{
						/*
							The same literal positive and negative.
							So a tautology. -> eliminate clause.
						*/
						clause[ 0 ] = 0;
						taut++;
					}
				}
	   		}

	}

	/*
		Quick sort all clauses in the CNF.
	*/
	qsort( Cv, nrofclauses, sizeof( int ), clsCompare );

	/*
		Accumulate all 1-clauses on the fstack.
	*/
	if( useStack )
	{
		for( i = 0; i < nrofclauses; i++ )
			if( Cv[ i ][ 0 ] == 1 )
				*( ( *_fstackp )++ ) = Cv[ i ][ 1 ];
	}


	/*
		Remove all identical clauses.
	*/
	for( i = 0; i < nrofclauses - 1; i++ )
	{
	   flag = 1;
	   for( j = 0; j <= Cv[ i ][ 0 ]; j++ )
		if( Cv[ i ][ j ] != Cv[ i + 1 ][ j ] )
		{
			flag = 0;
			break;
		}
		if( flag ) Cv[ i ][ 0 ] = 0;
	}

	/*
		Restore Clength and Cv.	
	*/
	Clength = (int*) malloc( sizeof( int ) * nrofclauses );

	k = 0;
	for( i = 0; i < nrofclauses; i++ )
	{
		clen = Cv[ i ][ 0 ];
		if( clen == 0 )
		{
			free( Cv[ i ] );
			Cv[ i ] = NULL;
		}
		else
		{
			tmpcls = (int*) malloc( sizeof( int ) * clen );
			Clength[ k ] = clen;

			for( j = 0; j < clen; j++ )
				tmpcls[ j ] = Cv[ i ][ j + 1 ];

			free( Cv[ i ] );
			Cv[ k ] = tmpcls;
			k++;
		}
	}
	nrofclauses = k;
	Cv = (int**) realloc( Cv, sizeof( int* ) * nrofclauses );
	Clength = (int*) realloc( Clength, sizeof( int ) * nrofclauses );

	return ( _nrofclauses - nrofclauses );
}


/*
	MALLOCS: 	_VcTemp, _Vc, _Vc[ .. ], _VcLUT, _VcLUT[ .. ]
	REALLOCS:	_Vc[ .. ], _VcLUT[ .. ]
	FREES:	 	_VcTemp, _Vc[ * ], _Vc, _VcLUT[ * ], _VcLUT
*/
int replaceEq()
{
	int i, bieq;
	int **_Vc, **_VcLUT;
	int lit1, lit2;

        allocateVc( &_Vc, &_VcLUT );

	/*
		Check for bi-equivalencies.
	*/
	bieq = 0;
	for( i = 0; i < ( nrofclauses - 1 ); i++ )
	{
		int *clause = Cv[ i ];
		int *next_clause = Cv[ i + 1 ];
		
		if( Clength[ i ] == 2 && Clength[ i + 1 ] == 2 &&
			NR( clause[ 0 ] ) == NR( next_clause[ 0 ] ) &&
			NR( clause[ 1 ] ) == NR( next_clause[ 1 ] ) &&
			( SGN( clause[ 0 ] ) * SGN( clause[ 1 ] ) ) ==
			( SGN( next_clause[ 0 ] ) * SGN( next_clause[ 1 ] ) ) &&
			NR( clause[ 0 ] ) != NR( clause[ 1 ] ) )  // GOED CHECKEN!!!
		{
			if( clause[ 0 ] == next_clause[ 0 ] )
				continue;

			/*
				Don't detect previously detected bi-equivalencies!
			*/
                        if( ( _Vc[ VARPRE( clause[ 0 ] ) ][ 0 ] + _Vc[ VARPRE( -clause[ 0 ] ) ][ 0 ] ) == 2 )
                                continue;

                        if( ( _Vc[ VARPRE( clause[ 1 ] ) ][ 0 ] + _Vc[ VARPRE( -clause[ 1 ] ) ][ 0 ] ) == 2 )
                                continue;

			/*
				We found a new bi-equivalency.
			*/
			bieq++;

			/*
				Remove clauses;
			*/
			Clength[ i ] = 0;
			Clength[ i + 1 ] = 0;

			lit1 = clause[ 0 ];
			lit2 = clause[ 1 ];

			preBieq[ preBieqSize++ ] = lit1;
			preBieq[ preBieqSize++ ] = lit2;

		        timeAssignments[ NR(lit1) ] = VARMAX;

			//printf("bieq found %i %i\n", lit1, lit2);

			replace_bieq( lit1, lit2, &_Vc, &_VcLUT );
		}
	}

	/*
		Free temporary allocated space.
	*/	
	for( i = 0; i < 2 * nrofvars + 1; i++ )
	{
		free( _Vc   [ i ] );
		free( _VcLUT[ i ] );
	}

	free( _Vc );
	free( _VcLUT );

	return bieq;
}


int eqProduct()
{
        int i;

	for( i = 0; i < nrofclauses; i++ )
		if( Clength[ i ] > 2 )
			i += find_equivalence( i );

	//printCeq();

	printNrofEq();

	//return;

	reduceEquivalence();
	

#ifdef FIND_EQ
for( i = 0; i < 5; i++ )
{
	find_bieq();
	printf("\n");
}
#endif

	if( substitude_equivalences() == UNSAT )
		return UNSAT;
	reduceEquivalence();

	//printCeq();

	shorten_equivalence();

	//printCeq();
	
#ifndef	EQ
	printf("c eqProduct():: equivalence reasoning turned off.\n");
#endif

	return 1;
}


void transformTo3SAT()
{
	int 	**_Cv, *_dummy;

	int 	i, b, e, delta, clen, max_clen, nrof3SATvars, two, cp;
	int 	_lit1, _lit2, _varlit1, _varlit2;

#ifdef CHAINPLUS
	int 	_varlitA, _varlitB, _varlitNotX;
#endif
	delta = 0;
	max_clen = 0;
	for( i = 0; i < nrofclauses; i++ )
	{
		if( Clength[ i ] > max_clen )
			max_clen = Clength[ i ];
			
		if( Clength[ i ] > 3 )
			delta += Clength[ i ] - 3;
	}
	nrof3SATvars = nrofvars + delta;

#ifdef CHAINPLUS
	printf( "c transformTo3SAT():: CHAINPLUS defined.\n" );
#endif
	printf( "c transformTo3SAT():: you gave me %i variables and %i clauses.\n", nrofvars, nrofclauses );
	printf( "c transformTo3SAT():: by the way, the maximal clause length before transformation was %i.\n", max_clen );

#ifdef CHAINPLUS
	printf( "c transformTo3SAT():: I will add %i variables and %i clauses!\n", delta, delta * 3 );
	_Cv = (int**) malloc( sizeof( int* ) * ( nrofclauses + ( delta * 3 ) ) );
#else
	printf( "c transformTo3SAT():: I will add %i variables and %i clauses!\n", delta, delta );
	_Cv = (int**) malloc( sizeof( int* ) * ( nrofclauses + delta ) );
#endif

	/*
		Resize global data structures.
	*/
	timeAssignments = (tstamp*) realloc( timeAssignments, sizeof( tstamp ) * ( nrof3SATvars + 1 ) );
	timeValues = (int*) realloc( timeValues, sizeof( int ) * ( nrof3SATvars + 1 ) );
        VeqDepends = (int*) realloc( VeqDepends, sizeof( int ) * ( nrof3SATvars + 1 ) );
	for( i = nrofvars + 1; i < ( nrof3SATvars + 1 ); i++ )
	{
		timeAssignments[ i ] = 0;
		timeValues[ i ] = 0;
		VeqDepends[ i ] = ALL_VARS;
	}

//#ifdef EQ
	Veq = (int**) realloc( Veq, sizeof( int* ) * ( nrof3SATvars + 1 ) );
	VeqLUT = (int**) realloc( VeqLUT, sizeof( int* ) * ( nrof3SATvars + 1 ) );

	for( i = nrofvars + 1; i < ( nrof3SATvars + 1 ); i++ )
	{
		Veq[ i ] = (int*) malloc( sizeof( int ) * 2 );
		VeqLUT[ i ] = (int*) malloc( sizeof( int ) * 2 );
		Veq[ i ][ 0 ] = 1;
		VeqLUT[ i ][ 0 ] = 1;
	}
//#endif

	/* Ic: implication clause table */
	Ic = (int**) malloc( sizeof( int* ) * ( 2 * nrof3SATvars + 1 ) );
	IcLength = (int*) malloc( sizeof( int ) * ( 2 * nrof3SATvars + 1 ) );
        
	for( i = 0; i < 2 * nrof3SATvars + 1; i++ )
  	{
		Ic[ i ] = (int*) malloc( sizeof( int ) * INITIAL_ARRAY_SIZE );
		Ic[ i ][ 0 ] = 2;
		/* aantal orig. 2-clauses ( MISSCHIEN VERWIJDEREN ) */
		Ic[ i ][ 1 ] = 0;
		IcLength[ i ] = INITIAL_ARRAY_SIZE - 1;
	}

	/* dummy array needed for transformation. */
	_dummy = (int*) malloc( sizeof( int ) * max_clen * 2 );
	
	cp = 0;
	two = 0;
	for( i = 0; i < nrofclauses; i++ )
	{
		clen = Clength[ i ];

		/* eliminate 0-clauses from formula!! */
		/* 1-clauses shouldn't be there! */
		if( clen == 0 || clen == 1 )
		{
			continue;
		}
		else if( clen == 2 )
		{
			/* add 2-clause in Ic and increase 2-clause counter. */
			two++;
			
			_lit1 = Cv[ i ][ 0 ];
			_lit2 = Cv[ i ][ 1 ];

			_varlit1 =  VAR3SAT( -_lit1 );
			_varlit2 =  VAR3SAT( -_lit2 );

			CHECK_IC_BOUND( _varlit1 )
			CHECK_IC_BOUND( _varlit2 )

			Ic[ VAR3SAT( -_lit1 ) ][ Ic[ VAR3SAT( -_lit1 ) ][ 0 ]++ ] = _lit2;
			Ic[ VAR3SAT( -_lit2 ) ][ Ic[ VAR3SAT( -_lit2 ) ][ 0 ]++ ] = _lit1;
		}
		else if( clen == 3 )
		{
			/* 
					sacrifice memory to avoid using modulo 3 in 
					the solver code.
			*/
			_Cv[ cp ] = (int*) malloc( sizeof( int ) * 5 );
			_Cv[ cp ][ 0 ] = Cv[ i ][ 0 ];
			_Cv[ cp ][ 1 ] = Cv[ i ][ 1 ];
			_Cv[ cp ][ 2 ] = Cv[ i ][ 2 ];
			_Cv[ cp ][ 3 ] = Cv[ i ][ 0 ];
			_Cv[ cp ][ 4 ] = Cv[ i ][ 1 ];

			free( Cv[ i ] );

			/* next clause */
			cp++;
		}
		else
		{
			/* copy clause to _dummy */
			for( b = 0; b < clen; b++ ) _dummy[ b ] = Cv[ i ][ b ];

			b = 0;
			e = clen;
			while( b < (e - 3) )
			{	
				_Cv[ cp ] = (int*) malloc( sizeof( int ) * 5 );
				_Cv[ cp ][ 0 ] = _dummy[ b ];
				_Cv[ cp ][ 1 ] = _dummy[ b + 1 ];
				_Cv[ cp ][ 2 ] = -( ++nrofvars );
				_Cv[ cp ][ 3 ] = _dummy[ b ];
				_Cv[ cp ][ 4 ] = _dummy[ b + 1 ];
				cp++;
				
#ifdef CHAINPLUS
				two += 2;
				
				/*
					A v B v ~X, maar ook:
					
					X v ~A, ofwel ~X -> ~A en A -> X
					X v ~B, ofwel ~X -> ~B en B -> X
				*/
				
				_varlitA = VAR3SAT( _dummy[ b ] );
				_varlitB = VAR3SAT( _dummy[ b + 1 ] );
				_varlitNotX = VAR3SAT( -nrofvars );
				
				CHECK_IC_BOUND( _varlitA )
				CHECK_IC_BOUND( _varlitNotX )
				
				/* ~X -> ~A */
				Ic[ _varlitNotX ][ Ic[ _varlitNotX ][ 0 ]++ ] = -_dummy[ b ];
				/* A -> X */
				Ic[ _varlitA ][ Ic[ _varlitA ][ 0 ]++ ] = nrofvars;
				
				CHECK_IC_BOUND( _varlitB )
				CHECK_IC_BOUND( _varlitNotX )
				
				/* ~X -> ~B */
				Ic[ _varlitNotX ][ Ic[ _varlitNotX ][ 0 ]++ ] = -_dummy[ b + 1 ];
				/* B -> X */
				Ic[ _varlitB ][ Ic[ _varlitB ][ 0 ]++ ] = nrofvars;
#endif				

				b += 2;
				_dummy[ e++ ] = nrofvars;
			}

			_Cv[ cp ] = (int*) malloc( sizeof( int ) * 5 );
			_Cv[ cp ][ 0 ] = _dummy[ b ];
			_Cv[ cp ][ 1 ] = _dummy[ b + 1 ];
			_Cv[ cp ][ 2 ] = _dummy[ b + 2 ];
			_Cv[ cp ][ 3 ] = _dummy[ b ];
			_Cv[ cp ][ 4 ] = _dummy[ b + 1 ];
			cp++;

			free( Cv[ i ] );
		}
	}

	free( _dummy );

	free( Clength );
	Clength = NULL;

	free( Cv );
	Cv = _Cv;

	printf( "c transformTo3SAT():: the transformation yielded %i variabels and %i clauses ( %i, %i ).\n", nrofvars, two + cp, two, cp );
	nrofclauses = cp;
}


void lessRedundantTransformation()
{
	int **_Vc, *_VcTemp, **_Cv, *_Clength, *freq, A, B, X, maxPairs;
	int i, j, max, nrofvarsIn, nrofLRTvars, nrofLRTclauses, delta;
	int lit, posA, posB, clsidx, VA, VB, VX, last;

	A = 0;
	
	delta = 0;
	for( i = 0; i < nrofclauses; i++ )
		if( Clength[ i ] > 3 )
			delta += Clength[ i ] - 3;

	if( delta == 0 )
	{
		printf( "c lessRedundantTransformation():: nothing to be done.\n" );
		return;
	}


#ifdef CHAINPLUS
	printf( "c lessRedundantTransformation():: CHAINPLUS defined.\n" );
#endif

	nrofvarsIn = nrofvars;
	nrofLRTvars = nrofvars + delta;
	nrofLRTclauses = 0;
	
#ifdef CHAINPLUS
	_Cv = (int**) malloc( sizeof( int* ) * delta * 3 );
	_Clength = (int*) malloc( sizeof( int ) * delta * 3 );
#else
	_Cv = (int**) malloc( sizeof( int* ) * deltaC );
	_Clength = (int*) malloc( sizeof( int ) * deltaC );
#endif

	freq = (int*) malloc( sizeof( int ) * ( 2 * nrofLRTvars + 1 ) );
		
	/*
		Create a Vc for clauses longer than 3 literals.
	*/
        _VcTemp = (int*) malloc( sizeof( int ) * ( 2 * nrofLRTvars + 1 ) );
        for( i = 0; i <= 2 * nrofLRTvars; i++ ) _VcTemp[ i ] = 1;

        _Vc = (int**) malloc( sizeof( int* ) * ( 2 * nrofLRTvars + 1 ) );
        for( i = 0; i <= 2 * nrofLRTvars; i++ ) _Vc[ i ] = NULL;

        for( i = 0; i < nrofclauses; i++ )
		if( Clength[ i ] > 3 )
	                for( j = 0; j < Clength[ i ]; j++ )
        	                _VcTemp[ VARLRT( Cv[ i ][ j ] ) ]++;

       /* allocate space... */
	for( i = ( nrofLRTvars - nrofvars ); i <= ( nrofLRTvars + nrofvars ); i++ )
        {
                _Vc[ i ] = (int*) malloc( sizeof( int ) * _VcTemp[ i ] );
                _Vc[ i ][ 0 ] = _VcTemp[ i ] - 1;

                _VcTemp[ i ] = 1;
        }

        for( i = 0; i < nrofclauses; i++ )
		if( Clength[ i ] > 3 )
	                for( j = 0; j < Clength[ i ]; j++ )
        	        {
                	        lit = VARLRT( Cv[ i ][ j ] );
                        	_Vc[ lit ][ _VcTemp[ lit ] ] = i;
	                        _VcTemp[ lit ]++;
        	        }

	free( _VcTemp );

	do
	{
		/*	
			Find the most occuring literal.
		*/	
		max = 0;
		for( i = ( nrofLRTvars - nrofvars ); i <= ( nrofLRTvars + nrofvars ); i++ )
			if( _Vc[ i ][ 0 ] >= max )
			{
				max = _Vc[ i ][ 0 ];
				A = i - nrofLRTvars;
			}

		last = 0;
		B = A;
	
		do
		{
			last = A;
			A = B;

			for( i = 0; i <= 2 * nrofLRTvars; i++ )
				freq[ i ] = 0;

			for( i = 1; i <= _Vc[ VARLRT( A ) ][ 0 ]; i++ )
			{
				clsidx = _Vc[ VARLRT( A ) ][ i ];
				for( j = 0; j < Clength[ clsidx ]; j++ )
					freq[ VARLRT( Cv[ clsidx ][ j ] ) ]++;
			}

			/*
				Find the literal which occurs the most with literal A.
			*/
			maxPairs = 1;
			for( i = ( nrofLRTvars - nrofvars ); i <= ( nrofLRTvars + nrofvars ); i++ )
				if( ( i != VARLRT( A ) ) && ( freq[ i ] >= maxPairs ) )
				{
					maxPairs = freq[ i ];
					B = i - nrofLRTvars;
				}

		}
		while( last != B );
		
		if( maxPairs > MAXPAIR )
		{
			/*
				Create new variable.
			*/
			X = ++nrofvars;
			VA = VARLRT( A );
			VB = VARLRT( B );
			VX = VARLRT( X );

			_Vc[ VX ] = (int*) malloc( sizeof( int ) * ( maxPairs + 1 ) );
			_Vc[ VX ][ 0 ] = 0;

			_Vc[ VARLRT( -X ) ] = (int*) malloc( sizeof( int ) );
			_Vc[ VARLRT( -X ) ][ 0 ] = 0;

			for( i = 1; i <= _Vc[ VA ][ 0 ]; i++ )
			{
				clsidx = _Vc[ VA ][ i ];
				posA = posB = -1;
				for( j = 0; j < Clength[ clsidx ]; j++ )
				{
					if( Cv[ clsidx ][ j ] == A )
					{
						posA = j;
					}
					else if( Cv[ clsidx ][ j ] == B )
					{	
						posB = j;
					}
				}
				if( posB >= 0 )
				{
					Cv[ clsidx ][ posB ] = X;

					if( posB == ( Clength[ clsidx ] - 1 ) )
						posB = posA;

					Cv[ clsidx ][ posA ] = Cv[ clsidx ][ Clength[ clsidx ] - 1 ];
					Clength[ clsidx ]--;

					/*
						Swap clause clsidx out of _Vc[ VARLRT( A ) ].
					*/
					_Vc[ VA ][ i ] = _Vc[ VA ][ _Vc[ VA ][ 0 ]-- ];

					for( j = 1; j <= _Vc[ VB ][ 0 ]; j++ )
						if( _Vc[ VB ][ j ] == clsidx )
							break;

					/*
						Swap clause clsidx out of _Vc[ VARLRT( B ) ].
					*/
					_Vc[ VB ][ j ] = _Vc[ VB ][ _Vc[ VB ][ 0 ]-- ];
							

					if( Clength[ clsidx ] > 3 )
					{
						_Vc[ VX ][ ++_Vc[ VX ][ 0 ] ] = clsidx;
					}
					else
					{
						/*
							Remove the other two literals.
						*/
						lit = VARLRT( Cv[ clsidx ][ ( posB + 1 ) % 3 ] );
						for( j = 1; j <= _Vc[ lit ][ 0 ]; j++ )
                		                if( _Vc[ lit ][ j ] == clsidx )
                                		        break;
						_Vc[ lit ][ j ] = _Vc[ lit ][ _Vc[ lit ][ 0 ]-- ];

						lit = VARLRT( Cv[ clsidx ][ ( posB + 2 ) % 3 ] );
						for( j = 1; j <= _Vc[ lit ][ 0 ]; j++ )
                		                if( _Vc[ lit ][ j ] == clsidx )
                                		        break;
						_Vc[ lit ][ j ] = _Vc[ lit ][ _Vc[ lit ][ 0 ]-- ];
					}

					i--;
				}
			}

			_Cv[ nrofLRTclauses ] = (int*) malloc( sizeof( int ) * 3 );
			_Clength[ nrofLRTclauses ] = 3;
			_Cv[ nrofLRTclauses ][ 0 ] = A;
			_Cv[ nrofLRTclauses ][ 1 ] = B;
			_Cv[ nrofLRTclauses ][ 2 ] = -X;
			nrofLRTclauses++;
#ifdef CHAINPLUS
			_Cv[ nrofLRTclauses ] = (int*) malloc( sizeof( int ) * 2 );
			_Clength[ nrofLRTclauses ] = 2;
			_Cv[ nrofLRTclauses ][ 0 ] = X;
			_Cv[ nrofLRTclauses ][ 1 ] = -A;
			nrofLRTclauses++;

			_Cv[ nrofLRTclauses ] = (int*) malloc( sizeof( int ) * 2 );
			_Clength[ nrofLRTclauses ] = 2;
			_Cv[ nrofLRTclauses ][ 0 ] = X;
			_Cv[ nrofLRTclauses ][ 1 ] = -B;
			nrofLRTclauses++;
#endif
		}
	}
	while( maxPairs > MAXPAIR );

	Cv = (int**) realloc( Cv, sizeof( int* ) * ( nrofclauses + nrofLRTclauses ) );
	Clength = (int*) realloc( Clength, sizeof( int ) * ( nrofclauses + nrofLRTclauses ) );

	for( i = 0; i < nrofLRTclauses; i++ )
	{
		Cv     [ nrofclauses ] = _Cv     [ i ];
		Clength[ nrofclauses ] = _Clength[ i ];
		nrofclauses++;
	}
	
	for( i = 0; i <= ( 2 * nrofLRTvars ); i++ )
	{
		if( _Vc[ i ] != NULL ) free( _Vc[ i ] );
	}

	free( _Vc );

	free( _Cv );
	free( _Clength );
	free( freq );

	/*
		Resize global data structures.
	*/
        timeAssignments = (tstamp*) realloc( timeAssignments, sizeof( tstamp ) * ( nrofvars + 1 ) );
        timeValues = (int*) realloc( timeValues, sizeof( int ) * ( nrofvars + 1 ) );
	VeqDepends = (int*) realloc( VeqDepends, sizeof( int ) * ( nrofvars + 1 ) );

	for( i = nrofvarsIn + 1; i < ( nrofvars + 1 ); i++ )
	{
		timeAssignments[ i ] = 0;
		timeValues[ i ] = 0;
		//VeqDepends[ i ] = 0;
		VeqDepends[ i ] = ALL_VARS;
	}

//#ifdef EQ
	Veq = (int**) realloc( Veq, sizeof( int* ) * ( nrofvars + 1 ) );
	VeqLUT = (int**) realloc( VeqLUT, sizeof( int* ) * ( nrofvars + 1 ) );

	for( i = nrofvarsIn + 1; i < ( nrofvars + 1 ); i++ )
	{
		Veq[ i ] = (int*) malloc( sizeof( int ) * 2 );
		VeqLUT[ i ] = (int*) malloc( sizeof( int ) * 2 );
		Veq[ i ][ 0 ] = 1;
		VeqLUT[ i ][ 0 ] = 1;
	}
//#endif
	/*
		Completed resizing global datastructures.
	*/

	printf( "c lessRedundantTransformation():: the transformation yielded %i variables and %i clauses.\n", nrofvars, nrofclauses );
}	


int addResolvents()
{
        int i, j, varnr, addCv, clsidx, vinc, lit1, lit2;
        int **_Vc, **_VcLUT, *_VcTemp;
        int **_resCv, *_resLength;
        struct resolvent *resolvent;

        addCv = 0;

        _resCv = (int**) malloc( sizeof( int* ) * ( _CvSize + 1 ) );
        _resLength = (int*) malloc( sizeof(int) * ( _CvSize + 1 ) );

        resolvent = (struct resolvent*) malloc( sizeof( struct resolvent ) * ( 2 * nrofvars + 1 ) );
        for( i = 0; i <= 2 * nrofvars; i++ ) resolvent[ i ].stamp = -1;

        /* Global datastructure */
        _VcTemp = (int*) malloc( sizeof( int ) * ( 2 * nrofvars + 1 ) );
        for( i = 0; i < 2 * nrofvars + 1; i++ ) _VcTemp[ i ] = 1;

        _Vc = (int**) malloc( sizeof( int* ) * ( 2 * nrofvars + 1 ) );
        _VcLUT = (int**) malloc( sizeof( int* ) * ( 2 * nrofvars + 1 ) );

        for( i = 0; i < nrofclauses; i++ )
                for( j = 0; j < Clength[ i ]; j++ )
                        _VcTemp[ VAR( Cv[ i ][ j ] ) ]++;

        /* allocate space... */
        for( i = 0; i <= 2 * nrofvars; i++ )
        {
                _Vc[ i ] = (int*) malloc( sizeof( int ) * _VcTemp[ i ] );
                _Vc[ i ][ 0 ] = _VcTemp[ i ] - 1;

                _VcLUT[ i ] = (int*) malloc( sizeof( int ) * _VcTemp[ i ] );
                _VcLUT[ i ][ 0 ] = _VcTemp[ i ] - 1;

                _VcTemp[ i ] = 1;
        }

        for( i = 0; i < nrofclauses; i++ )
                for( j = 0; j < Clength[ i ]; j++ )
                {
                        varnr = VAR( Cv[ i ][ j ] );
                        _Vc[ varnr ][ _VcTemp[ varnr ] ] = i;
                        _VcLUT[ varnr ][ _VcTemp[ varnr ] ] = j;
                        _VcTemp[ varnr ]++;
                }

        free( _VcTemp );

        for( i = 0; i <= 2 * nrofvars; i++ )
        {
		if( addCv >= _CvSize ) break;
                for( j = 1; j < _Vc[ i ][ 0 ]; j++ )
                {
			if( addCv >= _CvSize ) break;
                        clsidx = _Vc[ i ][ j ];
                        vinc = _VcLUT[ i ][ j ];
                        if( Clength[ clsidx ] == 2 )
                        {
                                lit1 = Cv[ clsidx ][ 1 - vinc ];

                                resolvent[ VAR(lit1) ].stamp = i;
                                resolvent[ VAR(lit1) ].literal = 0;

                                if( resolvent[ VAR(-lit1) ].stamp == i )
                                {
                                        if( resolvent[ VAR(-lit1) ].literal )
                                        {
                                                _resCv[ addCv ] = (int*) malloc( sizeof( int ) * 2 );
                                                _resLength[ addCv ] = 2;
                                                _resCv[ addCv ][ 0 ] = i - nrofvars;
                                                _resCv[ addCv++ ][ 1 ] = resolvent[ VAR(-lit1) ].literal;
                                        }
                                }
                        }
                        else if( Clength[ clsidx ] == 3 )
                        {
                                lit1 = Cv[ clsidx ][ (vinc + 1) % 3 ];
                                lit2 = Cv[ clsidx ][ (vinc + 2) % 3 ];

                                resolvent[ VAR(lit1) ].stamp = i;
                                resolvent[ VAR(lit1) ].literal = lit2;
                                resolvent[ VAR(lit2) ].stamp = i;
                                resolvent[ VAR(lit2) ].literal = lit1;

                                if( resolvent[ VAR(-lit1) ].stamp == i &&
                                    resolvent[ VAR(-lit1) ].literal != -lit2 )
                                {
                                        if( !resolvent[ VAR(-lit1) ].literal )
                                        {
                                                _resCv[ addCv ] = (int*) malloc( sizeof( int ) * 2);
                                                _resLength[ addCv ] = 2;
                                                _resCv[ addCv ][ 0 ] = i - nrofvars;
                                                _resCv[ addCv++ ][ 1 ] = lit2;
                                        }
                                        else
                                        {
                                                _resCv[ addCv ] = (int*) malloc( sizeof( int ) * 3 );
                                                _resLength[ addCv ] = 3;
                                                _resCv[ addCv ][ 0 ] = i - nrofvars;
                                                _resCv[ addCv ][ 1 ] = lit2;
                                                _resCv[ addCv++ ][ 2 ] = resolvent[ VAR(-lit1) ].literal;
                                        }
                                }
                                if( resolvent[ VAR(-lit2) ].stamp == i &&
                                    resolvent[ VAR(-lit2) ].literal != -lit1 )
                                {
                                        if( !resolvent[ VAR(-lit2) ].literal )
                                        {
                                                _resCv[ addCv ] = (int*) malloc( sizeof( int ) * 2);
                                                _resLength[ addCv ] = 2;
                                                _resCv[ addCv ][ 0 ] = i - nrofvars;
                                                _resCv[ addCv++ ][ 1 ] = lit1;
                                        }
                                        else
                                        {
                                                _resCv[ addCv ] = (int*) malloc( sizeof( int ) * 3 );
                                                _resLength[ addCv ] = 3;
                                                _resCv[ addCv ][ 0 ] = i - nrofvars;
                                                _resCv[ addCv ][ 1 ] = lit1;
                                                _resCv[ addCv++ ][ 2 ] = resolvent[ VAR(-lit2) ].literal;
                                        }
                                }
                        }
                }
        }

        Cv = (int**) realloc( Cv, sizeof( int* ) * ( nrofclauses + addCv ) );
        Clength = (int*) realloc( Clength, sizeof( int ) * ( nrofclauses + addCv ));

        for( i = nrofclauses; i < nrofclauses + addCv; i++ )
        {
                Cv[ i ] = _resCv[ i - nrofclauses ];
                Clength[ i ] = _resLength[ i - nrofclauses ];
        }

        for( i = 0; i <= 2 * nrofvars; i++ )
        {
                free( _Vc   [ i ] );
                free( _VcLUT[ i ] );
        }

        free( _Vc );
        free( _VcLUT );
	free( _resCv );
	free( _resLength );

        nrofclauses += addCv;

	return  addCv;
}


void printCNF( int** _Cv )
{
	int i, j, _nrofclauses;

	_nrofclauses = 0;
	for( i = 0; i < nrofclauses; i++ )
        {
	        if( Clength[ i ] > 0 )
		{
			_nrofclauses++;
			for( j = 0; j < Clength[ i ]; j++ )
                        	printf( "%i ", _Cv[ i ][ j ] );
        		printf( "0\n" );
		}
	}
	printf( "p cnf %i %i\n", nrofvars, _nrofclauses );
}

#ifdef DEBUGGING

void printFormula( int** _Cv )
{
	int i, j;

        for( i = 0; i < nrofclauses; i++ )
        {
                DEBUG( PA, "clause %i ( %i ): ( ", i, Clength[ i ] );

                for( j = 0; j < Clength[ i ]; j++ )
                {
                        DEBUG( PA, "%i ", _Cv[ i ][ j ] );
                }
                DEBUG( PA, ")\n" );
        }
}

#endif
