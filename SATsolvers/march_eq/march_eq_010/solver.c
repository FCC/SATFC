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
#include <assert.h>

#include "common.h"
#include "debug.h"
#include "solver.h"
#include "equivalence.h"
#include "mar.h"
#include "lookahead.h"
#include "progressBar.h"

#define VARINIT( a ) 	( a + nrofvars );

#define STAMP_IC( _var ) \
{ \
    int *loc = Ic[ _var ] + 1; \
    currentIcTS++; \
    for (i = loc[ -1 ] - 1; --i; ) \
	IcStamps[ loc[ i ] ] = currentIcTS; \
}

#define STAMP_IC2( _var ) \
{ \
    int *loc = Ic[ _var##idx ] + 1; \
    currentIcTS++; \
    IcStamps[ -_var ] = currentIcTS; \
    for (i = loc[ -1 ] - 1; --i; ) \
	IcStamps[ loc[ i ] ] = currentIcTS; \
}

#define SET_VARS( _var ) \
{ \
    nftmp = Ic[ _var ][ i ]; \
    nftmpvar = VAR(nftmp); \
    nftmpidx = VAR(-nftmp); \
}

#define CHECK_NODE_STAMP( _var ) \
{ \
    if( nodeStamps[ _var ] != currentNodeStamp ) \
    { \
	PUSH( imp, _var ); \
        PUSH( imp, Ic[ _var ][ 0 ] ); \
        nodeStamps[ _var ] = currentNodeStamp; \
    } \
}

#define ADD_CLAUSE( _var1, _var2 ) \
{ \
	Ic[ _var2##idx ][ ( Ic[ _var2##idx ][ 0 ] )++ ] = _var1; \
	Ic[ _var1##idx ][ ( Ic[ _var1##idx ][ 0 ] )++ ] = _var2; \
}



/*
	--------------------------------------------------------------------------------------------------------------
	-----------------------------------------[ initializing and freeing ]-----------------------------------------
	--------------------------------------------------------------------------------------------------------------
*/
int initSolver()
{
	int i, j, varnr;
	int *_VcTemp, _VlCount;

	/* allocate recursion stack */
	/* tree is max. nrofvars deep and we thus have max. nrofvars STACK_BLOCKS
		 -> 2 * nrofvars should be enough for everyone :)
	*/
	rstackSize = 2 * nrofvars + TO_BE_SURE_MARGIN;
	rstack = (int*) malloc( sizeof( int ) * rstackSize );
	rstackp = rstack;

	/* allocate implication botte jetser stack */
	impstackSize = TO_BE_SURE_MARGIN;
	impstack = (int*) malloc( sizeof( int ) * impstackSize );
	impstackp = impstack;

        bieqstackSize = 2 * nrofvars;
        bieqstack = (int*) malloc( sizeof( int ) * bieqstackSize );
        bieqstackp = bieqstack;

        newbistackSize = nrofceq;
        newbistack = (int*) malloc( sizeof( int ) * newbistackSize );
        newbistackp = newbistack;

        substackSize = 20 * nrofvars;
        substack = (int*) malloc( sizeof( int ) * substackSize );
        substackp = substack;

	satEquivalence = (int*) malloc( sizeof( int ) * nrofvars + 1 );
	for( i = 0; i <= nrofvars; i++ ) satEquivalence[ i ] = 0;

	/* temporary array to count #clauses containing var i. */
	_VcTemp = (int*) malloc( sizeof( int ) * ( 2 * nrofvars + 1 ) );
	for( i = 0; i < 2 * nrofvars + 1; i++ ) _VcTemp[ i ] = 1;

	/* Vc: in which clauses is var i contained? */
	Vc = (int**) malloc( sizeof( int* ) * ( 2 * nrofvars + 1 ) );
	VcLUT = (int**) malloc( sizeof( int* ) * ( 2 * nrofvars + 1 ) );

	/* nodeStamps: used to check if we've already pushed Ic[ VAR ][ 0 ] on the impstack */
	nodeStamps = (int*) malloc( sizeof( int ) * ( 2 * nrofvars + 1 ) );
	for( i = 0; i < 2 * nrofvars + 1; i++ ) nodeStamps[ i ] = 0;

	/* CvStamps: used to check if a clause is satisfied or reduced to a 2-clause. */
	CvStamps = (tstamp*) malloc( sizeof( tstamp ) * nrofclauses );
	for( i = 0; i < nrofclauses; i++ ) CvStamps[ i ] = 0;

	/* IcStamps */
	IcStamps = (int*) malloc( sizeof( int ) * ( 2 * nrofvars + 1 ) );
	for( i = 0; i < 2 * nrofvars + 1; i++ ) IcStamps[ i ] = 0;

	Ic_dead = (int*) malloc( sizeof( int ) * ( 2 * nrofvars + 1 ) );

	/* Vact */
	Vact = (int*) malloc( sizeof( int ) * ( 2 * nrofvars + 1 ) );

#ifdef COMPLEXDIFF
	/* diff */
	diff = (int*) malloc( sizeof( int ) * ( 2 * nrofvars + 1 ) );
	for( i = 0; i <= 2*nrofvars; i++ ) diff[ i ] = 0;
#endif

	/* ACTvalues */
	ACTvalues = (int*) malloc( sizeof( int ) * ( nrofvars + 1 ) );

	/* NNA */
	NNA = (int*) malloc( sizeof( int ) * ( nrofvars + 1 ) );

	/* nastack */
	nastack = (int*) malloc( sizeof( int ) * ( nrofvars + 1 ) );

        CeqSizes1 = (int*) malloc( sizeof( int ) * nrofceq );
        CeqValues1 = (int*) malloc( sizeof( int ) * nrofceq );

        for( i = 0; i < nrofceq; i++ )
        {
                CeqValues1[ i ] = CeqValues[ i ];
                CeqSizes1[ i ] = CeqSizes[ i ];
        }

#ifdef REPLACEDB
	_VlCount =0;

       /* temporary array to count #clauses containing var i. */
        _VcTemp = (int*) malloc( sizeof( int ) * ( 2 * nrofvars + 1 ) );
        for( i = 0; i < 2 * nrofvars + 1; i++ ) _VcTemp[ i ] = 0;

        /* lit list*/
        litlist = (int*) malloc( sizeof( int ) * 5 * nrofclauses );

        VlTable = (int*) malloc( sizeof(int) * ( 3 * nrofclauses ) );
        Vl = (int**) malloc( sizeof( int* ) * ( 2 * nrofvars + 1 ) );

        VcTable = (int*) malloc( sizeof(int) * ( 9 * nrofclauses ) );
        Lidx = (int**) malloc( sizeof( int* ) * ( 2 * nrofvars + 1 ) );
        Lloc = (int**) malloc( sizeof( int* ) * ( 2 * nrofvars + 1 ) );

        /* Vact */
        Vact = (int*) malloc( sizeof( int ) * ( 2 * nrofvars + 1 ) );
        tmpVact = (int*) malloc( sizeof( int ) * ( 2 * nrofvars + 1 ) );

        Vmax = (int*) malloc( sizeof( int ) * ( 2 * nrofvars + 1 ) );

        /* Cl */
        Cl = (int*) malloc( sizeof( int ) * nrofclauses * 3 );
#endif
	/* reset global timeStamp */
	currentNodeStamp = 1;
	lookDead = 0;
	mainDead = 0;

	for( i = 0; i < nrofclauses; i++ )
		for( j = 0; j < 3; j++ )
			_VcTemp[  Cv[ i ][ j ] + nrofvars ]++;

#ifdef REPLACEDB
        /* allocate space... */
        for( i = 0; i <= 2 * nrofvars; i++ )
        {
                Vl[ i ] = VlTable + _VlCount;
                Lidx[ i ] = VcTable + 3 * _VlCount;
                Lloc[ i ] = Lidx[ i ] + _VcTemp[ i ];
                _VlCount += _VcTemp[ i ];
                Vact[ i ] = _VcTemp[ i ];
                Vmax[ i ] = _VcTemp[ i ];

                _VcTemp[ i ] = 0;
                Ic_dead[ i ] = 2;
        }

        for( i = 0; i < nrofclauses; i++ )
        {
                for( j = 0; j < 3; j++ )
                {
                        varnr = Cv[ i ][ j ] + nrofvars;
                        Cl[ 3 * i + j ] = _VcTemp[ varnr ];
                        Vl[ varnr ][ _VcTemp[ varnr ] ] = 3 * i + j;
                        switch( j )
                        {
                                case 0: Vl[ varnr ][ _VcTemp[ varnr ] ] |= OFFSET1; break;
                                case 1: Vl[ varnr ][ _VcTemp[ varnr ] ] |= OFFSET2; break;
                                case 2: Vl[ varnr ][ _VcTemp[ varnr ] ] |= OFFSET3; break;
                        }
                        Lidx[ varnr ][ _VcTemp[ varnr ] ] = 2 * _VcTemp[ varnr ];
                        Lidx[ varnr ][ Vact[ varnr ] + 2 * _VcTemp[ varnr ]     ] = Cv[ i ][ (j + 1) % 3 ];
                        Lidx[ varnr ][ Vact[ varnr ] + 2 * _VcTemp[ varnr ] + 1 ] = Cv[ i ][ (j + 2) % 3 ];

                        _VcTemp[ varnr ]++;
                }
        }
#else
	/* allocate space... */
	for( i = 0; i <= 2 * nrofvars; i++ )
	{
		Vc[ i ] = (int*) malloc( sizeof( int ) * _VcTemp[ i ] );
		Vc[ i ][ 0 ] = _VcTemp[ i ] - 1;
		Vact[ i ] = _VcTemp[ i ] - 1;
		VcLUT[ i ] = (int*) malloc( sizeof( int ) * _VcTemp[ i ] );
		VcLUT[ i ][ 0 ] = _VcTemp[ i ] - 1;

		_VcTemp[ i ] = 1;
		Ic_dead[ i ] = 2;
	}

	for( i = 0; i < nrofclauses; i++ )
		for( j = 0; j < 3; j++ )
		{
			varnr = Cv[ i ][ j ] +nrofvars;
			Vc[ varnr ][ _VcTemp[ varnr ] ] = i;
			VcLUT[ varnr ][ _VcTemp[ varnr ] ] = j;
			_VcTemp[ varnr ]++;
		}
#endif
	initIndepend();

	free( _VcTemp );

	initLookahead();

	centerPtrs();

	//printCeq();

#ifdef EQ
/*
	for( i = 1; i <= nrofvars; i++ )	//WAAROM DIT SOORT NARE HACKS???
	{
		if( Veq[ i ][ 0 ] > 1 )
		{
			Vact[ i  ] += 2;
			Vact[ -i ] += 2;
		}
	}
*/
	for( i = 0; i < nrofceq ; i++ )
	{
		if( CeqSizes[ i ] == 2 )
                        add_ceq_imp( i );
		else if( CeqSizes[ i ] == 1 )				
		{
                 	if( !unitresolve( Ceq[ i ][ 0 ] * CeqValues[ i ] ) )
			{
				return 0;
			}
		}
	}
#else
	for( i = 0; i < nrofceq ; i++ )
	{
		if( CeqSizes[ i ] == 1 )				
                 	if( !unitresolve( Ceq[ i ][ 0 ] * CeqValues[ i ] ) )
				return 0;
	}
#endif

#ifdef ROOTLOOK
	PUSH( r, STACK_BLOCK );
	PUSH( imp, STACK_BLOCK );
        PUSH( bieq, STACK_BLOCK );
	currentNodeStamp++;

        return full_lookahead();
#else
	return 1;
#endif
}


int full_lookahead()
{
        int *_NA, _NAsize, i, j;
        //int *tmpVc, *tmpVl; //temporary not in use
        int _count;

        printf("c root full lookahead\n");
        printf("c freevars: %i\n", freevars);

	initACT();

        lookaheadArrayLength = 0;
        for( i = 0; i < freevars; i++ )
                lookaheadArray[ lookaheadArrayLength++ ] = independ[ i ];

	qsort( lookaheadArray, lookaheadArrayLength, sizeof( int ), actCompare );

        if( !intellook() )
                return UNSAT;

        get_NA( &_NA, &_NAsize );
        for( i = 0; i < _NAsize; i++ )
                if( !unitresolve( _NA[ i ] ) )
                        return UNSAT;

        _count = 0;
        for( i = 1; i <= nrofvars ; i++ )
                if( timeAssignments[ i ] < VARMAX )
			_count+=Vact[i]+Vact[-i];

        printf("c freevars: %i\n", freevars);
        printf("c Vc reduced from %i to %i\n", nrofclauses, _count / 3 );

	_count = 0;
	for(i=0; i <freevars; i++ )
	{
		j =independ[ i ] +1;
		if ( VeqDepends[ j ] == 0 )
			_count++;
		
		//if( (VeqDepends[ j ] != 0) && (VeqDepends[ j ] != ALL_VARS) )
			//printf("c bieq %i\n", j);
	}

        printf("c freevars: %i\n", freevars);
        printf("c Size of independent set reduced from %i to %i\n", nrofvars, _count );

        return 1;


}

void disposeSolver()
{
#ifndef REPLACEDB
	int i;
#endif
	destroyLookahead();

#ifdef COMPLEXDIFF
	diff -= nrofvars;
#endif

#ifdef REPLACEDB
        Vmax -= nrofvars;
        Vl -= nrofvars;
        Lidx -= nrofvars;
        Lloc -= nrofvars;
        tmpVact -= nrofvars;
#endif
	timeAssignments -= nrofvars;
	timeValues -= nrofvars;
	Vact -= nrofvars;
	eqV -= nrofvars;
	IcStamps -= nrofvars;
	Ic_dead -= nrofvars;
#ifndef REPLACEDB
	for( i = 0; i < 2 * nrofvars + 1; i++ )
	{
		free( Vc[ i ] );		//geeft problemen bij par8
		free( VcLUT[ i ] );
	}
#endif

#ifdef REPLACEDB
	free( IcStamps );
	free( Vact );
	free( nodeStamps );
	free( impstack );
	free( rstack );
#else
	free( Vc );
	free( VcLUT );
	free( CvStamps );
#endif
}

void centerPtrs()
{
	int i;

	timeAssignments = (tstamp*) realloc( timeAssignments, sizeof(tstamp) * (2 * nrofvars + 1) );
	timeValues = (int*) realloc( timeValues, sizeof(int) * (2 * nrofvars + 1) );

	for( i = nrofvars; i >= 0; i-- )
	{
		timeAssignments[ i + nrofvars ] = timeAssignments[ i ];
		timeValues[ i + nrofvars ] = timeValues[ i ];
	}


	for( i = 0; i <= nrofvars; i++ )
	{
		timeAssignments[ i ] = timeAssignments[ 2 * nrofvars - i ];
		timeValues[ i ] = timeValues[ 2 * nrofvars - i ];
	}

#ifdef COMPLEXDIFF
	diff += nrofvars;
#endif

#ifdef REPLACEDB
        Vmax += nrofvars;
        Vl += nrofvars;
        Lidx += nrofvars;
        Lloc += nrofvars;
        tmpVact += nrofvars;
#endif
	timeAssignments += nrofvars;
	timeValues += nrofvars;
	Vact += nrofvars;
	eqV += nrofvars;
	//diff += nrofvars;
	IcStamps += nrofvars;
	Ic_dead += nrofvars;
}


/*
	--------------------------------------------------------------------------------------------------------------
	--------------------------------------------[ recursive solving ]---------------------------------------------
	--------------------------------------------------------------------------------------------------------------
*/
int verifySolution()
{
	int i, j, satisfied, value;

#ifdef EQ
	do
	{
		fixDependedEquivalences();
	}while( dependantsExists() );
#endif

	for( i = preBieqSize - 1; i > 0; i -= 2 )
	{
		//printf("Fixing prebieq %i %i %i\n",SGN( preBieq[ i ] ), SGN( preBieq[ i - 1 ] ),  SGN( timeValues[ NR(preBieq[ i ]) ] ) );
		value = SGN( preBieq[ i ] ) * SGN( preBieq[ i - 1 ] ) * SGN( timeValues[ NR(preBieq[ i ]) ] );
		timeValues[ NR(preBieq[ i - 1 ]) ] = NR(preBieq[ i - 1 ]) * -value;
		//printf("Fixing prebieq %i\n",  NR(preBieq[ i - 1 ]) * value );
	}


	/* check all implications */
/*
	for( i = 1; i < ( nrofvars + 1 ); i++ )
	{
		//if( timeAssignments[ i ] != VARMAX )
		//	printf("c NOG VRIJ???\n");

		int _varlit = VAR( timeValues[ i ] );
		for( j = 2; j < Ic[ _varlit ][ 0 ]; j++ )
			if( timeValues[ Ic[ _varlit ][ j ] ] != Ic[ _varlit ][ j ] )
			{
				printf("c i: %i, j: %i, timeValues[ i ]: %i, Ic[ _varlit ][ 0 ]: %i  %i  %i\n", i, j, timeValues[ i ], Ic[ _varlit ][ 0 ], Ic[ _varlit ][ j ], timeValues[ Ic[ _varlit ][ j ] ] );
				printf("c # free variables: %i ( %i )\n", freevars, independ[ 0 ]);
				//return 0;
			}
	}
*/

	//printf("freevar = %i first= %i \n", freevars, getIndepend() );
	/* check all 3-clauses */

	for( i = 0; i < nrofclauses; i++ )
	{
		satisfied = 0;
		for( j = 0; j < 3; j++ )
			if( timeValues[ Cv[ i ][ j ] ] == Cv[ i ][ j ] ) satisfied = 1;
		if( !satisfied ) return 0;
	}

#ifdef EQ
	for( i = 0; i < nrofceq; i++ )
	{

//		if( CeqStamps[ i ] == CLSMAX ) continue;

		value = CeqValues[ i ];
		for( j = 0; j < CeqSizes[ i ]; j++ )
			value *= SGN( timeValues[ Ceq[ i ][ j ] ] );
		if( value == -1 )
		{
			printf("c eq-clause %i is not satisfied yet\n", i);
			return 0;
		}
	}
#endif

	return 1;
}

void printSolution( int orignrofvars )
{
	int i;

	//printCeq();

	printf( "v" );
	for( i = 1; i < ( orignrofvars + 1 ); i++ )
		if( timeAssignments[ i ] == VARMAX )
			printf( " %i", timeValues[ i ] );

	printf( " 0\n" );
}

int marsolverec()
{
	int *_NA, _NAsize, v, _result, i;

	nodeCount++;

	/* zoek een variabele om op te branchen */

	do
	{
		initACT();

		//percentmar( PERCENT_MAR_SWEEPS );
		percentact( PERCENT_MAR_SWEEPS );

		/* alle variabelen zijn gezet -> we hebben misschien een oplossing? */
		if( lookaheadArrayLength == 0 )
		{
			if( verifySolution() )
#ifdef UNS
				return UNSAT;
#else
				return SAT;
#endif
			else
				return UNKNOWN;
		}

#ifdef INTELLOOK
		if( !intellook() )
#else
		if( !lookahead() )
#endif
		{
			lookDead++;
			return UNSAT;
		}

		/* fix all necessary assignments */
	
		get_NA( &_NA, &_NAsize );
		for( i = 0; i < _NAsize; i++ )
		{
			//printf("NA %i\n", _NA[ i ] );
			if( !unitresolve( _NA[ i ] ) )
				return 0;
		}

		//printf("%i , %i ( %i ) \n", _NAsize, maxLookPlace, freevars );

	}
#ifdef INTELLOOK
	while( get_maxIntellook() == 0 );
#else
	while( get_maxDiffVar() == 0 );
#endif

	v = get_direction( get_maxDiffVar() );
	//v = -v; //later gehalen!!!

	/* alle variabelen zijn gezet -> we hebben misschien een oplossing? */
	if( v == 0 )
	{
		if( verifySolution() )
			return SAT;
		else
			return UNKNOWN;
	}

//	printf("%i ratio: %2f\n", v, maxRatio );

	/* STACK_BLOCK marks 'real' nodes */
	PUSH( r, STACK_BLOCK );
	PUSH( imp, STACK_BLOCK );
        PUSH( bieq, STACK_BLOCK );
	currentNodeStamp++;
#ifdef PROGRESS_BAR
	pb_descend();
#endif
	if( unitresolve( v ) )
	{
		depth++;
		_result = marsolverec();
		depth--;
		if( _result == SAT || _result == UNKNOWN ) return _result;
	}
	backtrack();
#ifdef PROGRESS_BAR
	pb_climb();
#endif

	currentLookStamp++;

	PUSH( r, STACK_BLOCK );
	PUSH( imp, STACK_BLOCK );
        PUSH( bieq, STACK_BLOCK );
	currentNodeStamp++;
#ifdef PROGRESS_BAR
	pb_descend();
#endif
	if( unitresolve( -v ) )
	{
		depth++;
		_result = marsolverec();
		depth--;
		if( _result == SAT || _result == UNKNOWN ) return _result;
	}
	backtrack();
#ifdef PROGRESS_BAR
	pb_climb();
#endif

	return UNSAT;
}

int unitresolve( int nrval )
{
	int i, varidx, current;

	currentIcTS = 1;
	nastackp = 0;
	nastackSize = 0;

	//printf("UNIT RESOLVE %i\n", nrval);

	for( i = 1; i <=  nrofvars; i++ )
	{
		NNA[ i ] = 0;
		IcStamps[ i ] = 0;
		IcStamps[ -i ] = 0;
	}

	if( !fixonevar( nrval ) )
	{
		mainDead++;
		return 0;
	}

	varidx = VAR( nrval );

	/* is deze routine overbodig??? */

	for( i = 2; i < Ic[ varidx ][ 0 ]; i++ )
		if( !fixonevar( Ic[ varidx ][ i ] ) )
		{
			mainDead++;
			return 0;
		}

	for( i = 0; i < nastackSize; i++ )
	{
		current = nastack[ i ];
		
		if (timeAssignments[ current ] < VARMAX ) 
		{
			if( !fixonevar( NNA[ current ] ) )
			{
				mainDead++;
			 	return 0;	
			}
		}
		else if( timeValues[ current ] != NNA[ current ] )
		{
			mainDead++;
			return 0;
		}
	}

	return 1;
}

void backtrack()
{
	int idx, ptr, var;

	while( !( *( rstackp - 1 ) == STACK_BLOCK ) )
	{
		POP_BACKTRACK_RECURSION_STACK
	}

	/* pop STACK_BLOCK from stack */
	POP_RECURSION_STACK_TO_DEV_NULL

        while( !( *( bieqstackp - 1 ) == STACK_BLOCK ) )
        {
                POP( bieq, var );
                VeqDepends[ var ] = 0;                  // let goed op als je VAR weg wilt halen!!!
        }

        /* pop STACK_BLOCK from bieq stack */
        bieqstackp--;

	while( !( *( impstackp - 1 ) == STACK_BLOCK ) )
	{
		POP( imp, ptr );
		POP( imp, idx );
		Ic[ idx ][ 0 ] = ptr;
	}

	/* pop STACK_BLOCK from imp stack */
	impstackp--;
}

int get_direction( int nr )
{
	return maxSite * nr;
}

/*
	--------------------------------------------------------------------------------------------------------------
	---------------------------------------[ variable fixing and unfixing ]---------------------------------------
	--------------------------------------------------------------------------------------------------------------
*/
int fixonevar( int nrval )
{
	int i, nr;
	int *imp;
	int deathFlag;
	int nf1, nf2, current;
#ifdef EQ
	int ceqidx;
#endif

#ifdef REPLACEDB
        int lit1, lit2, last1, last2;

        unsigned int *litl, *litl1, *litl2;
        int *litc;
        int *lloc;

        int cur1, cur2, tmp, index, index2;
#else
	int *cls, *idx;
	int clsidx, vinc;
#endif

	nr = NR( nrval );

	unitResolveCount++;

	/* check if this variable is already set... */
	if( timeAssignments[ nrval ] >= VARMAX )
		return ( timeValues[ nrval ] == nrval );

	if( NNA[ nr ] == -nrval )
		return 0;

	imp = Ic[ VAR(nrval) ] + 1;	

	for( i = imp[ -1 ] - 1 ; --i; )
	{
		current = imp[ i ];
		if( ( timeAssignments[ current ] >= VARMAX ) &&
		    ( timeValues[ current ] == -current ) )
			return 0;
		if( NNA[ NR(current) ] == -current )
			return 0;
		if( NNA[ NR(current) ] == 0 )
			if( !newNA( current ) )
				return 0;
	}

	/* this variable is fixed */
	timeAssignments[ nrval ] = VARMAX;
	timeValues[ nrval ] = nrval;
	timeAssignments[ -nrval ] = VARMAX;  // +1???
	timeValues[ -nrval ] = nrval;
	
	PUSH( r, nrval );

	//printf("FIXING %i\n", nrval );

	/* update Q */
	updateACT( TO_MAR( nr ) );

        imp = Ic[ VAR(-nrval) ] + 1;
        for( i = imp[ -1 ] - 1 ; --i; )
                Ic_dead[ -imp[ i ] ]++;

	/* remove all clauses containing nrval */
#ifdef REPLACEDB
        litl = Lidx[ nrval ];
        litc = Vl[ nrval ];
        lloc = Lloc[ nrval ];
        for( i = 0; i < Vact[ nrval ]; i++ )
        {
                index = litl[ i ];
                index2 = litc[ index >> 1 ];

                nf1 = (index2 & LITMASK) + (index2 >> 30);
                nf2 = nf1 + ((index2 << 2) >> 30);

                lit1 = lloc[ index ];
                lit2 = lloc[ index + 1 ];

                litl1 = Lidx[ lit1 ];
                litl2 = Lidx[ lit2 ];

                cur1 = Cl[ nf1 ];
                cur2 = Cl[ nf2 ];

                Cl[ nf1 ] = last1 = --Vact[ lit1 ];
                Cl[ nf2 ] = last2 = --Vact[ lit2 ];

                Cl[ Vl[ lit1 ][ litl1[ last1 ] >> 1 ] & LITMASK ] = cur1;
                Cl[ Vl[ lit2 ][ litl2[ last2 ] >> 1 ] & LITMASK ] = cur2;

                tmp = litl1[ last1 ]; litl1[ last1 ] = litl1[ cur1 ]; litl1[ cur1 ] = tmp;
                tmp = litl2[ last2 ]; litl2[ last2 ] = litl2[ cur2 ]; litl2[ cur2 ] = tmp;
        }
        tmpVact[ nrval ] = Vact[ nrval ];
        Vact[ nrval ] = 0;
#else
	cls = Vc   [ VAR(nrval) ];
	idx = VcLUT[ VAR(nrval) ];

	for( i = 1; i <= cls[ 0 ]; i++ )
	{
		clsidx = cls[ i ];

		/* if clause is dead do nothing */
		if( CvStamps[ clsidx ] > CLSMAX ) continue;

		/* set clause to dead */
		CvStamps[ clsidx ] = CLSMAX + idx[ i ] + 1;

		Vact[ Cv[ clsidx ][ 0 ] ]--;
		Vact[ Cv[ clsidx ][ 1 ] ]--;
		Vact[ Cv[ clsidx ][ 2 ] ]--;
	}
#endif
	/* Use deathFlag instead of returning ZERO because otherwise the algoritm will not work */
	deathFlag = 1;

	/* remove all literals ~nrval */
#ifdef REPLACEDB
        litl = Lidx[ -nrval ];
        litc = Vl[ -nrval ];
        lloc = Lloc[ -nrval ];
        for( i = 0; i < Vact[ -nrval ]; i++ )
        {
                index = litl[ i ];
                index2 = litc[ index >> 1 ];

                nf1 = (index2 & LITMASK) + (index2 >> 30);
                nf2 = nf1 + ((index2 << 2) >> 30);

                lit1 = lloc[ index ];
                lit2 = lloc[ index + 1 ];

                litl1 = Lidx[ lit1 ];
                litl2 = Lidx[ lit2 ];

                cur1 = Cl[ nf1 ];
                cur2 = Cl[ nf2 ];

                Cl[ nf1 ] = last1 = --Vact[ lit1 ];
                Cl[ nf2 ] = last2 = --Vact[ lit2 ];

                Cl[ Vl[ lit1 ][ litl1[ last1 ] >> 1 ] & LITMASK ] = cur1;
                Cl[ Vl[ lit2 ][ litl2[ last2 ] >> 1 ] & LITMASK ] = cur2;

                tmp = litl1[ last1 ]; litl1[ last1 ] = litl1[ cur1 ]; litl1[ cur1 ] = tmp;
                tmp = litl2[ last2 ]; litl2[ last2 ] = litl2[ cur2 ]; litl2[ cur2 ] = tmp;

                if( !addImplication( lit1, lit2 ) )
                        deathFlag = 0;
        }
        tmpVact[ -nrval ] = Vact[ -nrval ];
        Vact[ -nrval ] = 0;
#else
	cls = Vc   [ VAR( -nrval ) ];
	idx = VcLUT[ VAR( -nrval ) ];

	for( i = 1; i <= cls[ 0 ]; i++ )
	{
		clsidx = cls[ i ];

		/* if clause is dead do nothing */
		if( CvStamps[ clsidx ] > CLSMAX ) continue;

		/* place of var in clause */
		vinc = idx[ i ];

		/* clause is now a 2-clause */
		CvStamps[ clsidx ] = CLSMAX + vinc + 1;

		/* update MAR */
		nf1 = Cv[ clsidx ][ vinc + 1 ];
		nf2 = Cv[ clsidx ][ vinc + 2 ];

		Vact[ Cv[ clsidx ][ 0 ] ]--;
		Vact[ Cv[ clsidx ][ 1 ] ]--;
		Vact[ Cv[ clsidx ][ 2 ] ]--;

		if( !addImplication( nf1, nf2 ) ) 
			deathFlag = 0;
	}
#endif

#ifdef EQ
        PUSH( sub, STACK_BLOCK );
        while( Veq[ nr ][ 0 ] > 1 )
        {
                ceqidx = Veq[ nr ][ 1 ];

                fixEq( nr, 1, SGN(nrval));
                removeEq( nr, 1 );
                PUSH( sub, nrval );

                if( CeqSizes1[ ceqidx ] == 2 )
		{
                        if ( !add_ceq_imp( ceqidx ) )
                                return 0;
                                //deathFlag = 0;
		}
		else if( CeqSizes1[ ceqidx ] == 1 )
                {
                        if( !newNA(Ceq[ ceqidx ][ 0 ] * CeqValues1[ ceqidx ] ) )
                                return 0;
                                //deathFlag = 0;
                }
        }

        while( newbistackp != newbistack )
        {
                POP( newbi, ceqidx );
                if( CeqSizes1[ ceqidx ] == 2 )
                {
                        if ( !add_ceq_imp( ceqidx ) )
                                return 0;
                                //deathFlag = 0;
                }
        }
#endif

	return deathFlag;
}

int add_ceq_imp( int ceqidx )
{
        int i, j, ceqsubst;
        int nf1, nf2;
        int value;

        nf1 = Ceq[ ceqidx ][ 0 ];
        nf2 = Ceq[ ceqidx ][ 1 ];
        value = CeqValues1[ ceqidx ];

        for( i = 1; i < Veq[ nf1 ][ 0 ]; i++ )
        {
                ceqsubst = Veq[ nf1 ][ i ];
                for( j = 1; j < Veq[ nf2 ][ 0 ]; j++ )
                {
                        if( (ceqsubst == Veq[ nf2 ][ j ]) )
                        {
                                fixEq( nf1, i, 1);
                                removeEq( nf1, i );
                                PUSH( sub, nf1 );

                                fixEq( nf2, j, value);
                                removeEq( nf2, j );
                                PUSH( sub, nf2 * value );

                                if( CeqSizes1[ ceqsubst ] == 0 )
                                        if (CeqValues1[ ceqsubst ] == -1 )
                                                return 0;

                                if( CeqSizes1[ ceqsubst ] == 1 )
                                        if( !newNA( Ceq[ ceqsubst ][ 0 ] * CeqValues1[ ceqsubst ]) )
                                                return 0;
	
                                if( CeqSizes1[ ceqsubst ] == 2 )
                                        PUSH( newbi, ceqsubst );

				i--;
                                break;
                        }
                }
        }

        if( (addImplication( nf1, -nf2 * value ) && addImplication( -nf1, nf2 * value )) == 0 )
                return 0;

        return 1;
}

int addImplication( int nf1, int nf2 )
{
	int i;
	int nftmp, nf1idx, nf2idx, nftmpidx;

        if( (NNA[ NR(nf1) ] != 0) || (NNA[ NR(nf2) ] != 0) )
        {
                if( (NNA[ NR(nf1) ] == nf1) || (NNA[ NR(nf2) ] == nf2) ) return 1;
                if( NNA[ NR(nf1) ] == 0 )  return newNA(nf1);
                if( NNA[ NR(nf2) ] == 0 )  return newNA(nf2);
                return 0;
        }

#ifdef BIEQ
	while( (VeqDepends[ NR(nf1) ] != 0) && (VeqDepends[ NR(nf1) ] != ALL_VARS) )
		nf1 = VeqDepends[ NR(nf1) ] * SGN(nf1);

	while( (VeqDepends[ NR(nf2) ] != 0) && (VeqDepends[ NR(nf2) ] != ALL_VARS) )
		nf2 = VeqDepends[ NR(nf2) ] * SGN(nf2);

	if( nf1 == -nf2 ) return 1;
	if( nf1 == nf2 ) return newNA(nf1);
#endif

	nf1idx = VAR(-nf1);
	nf2idx = VAR(-nf2);

	STAMP_IC2( nf1 );

	if( IcStamps[ -nf2 ] == currentIcTS )	return newNA( nf1 );
	else if( (IcStamps[ nf2 ] != currentIcTS)  && (timeAssignments[ nf2 ] < VARMAX) )
	{
		int *loc;

		CHECK_NODE_STAMP( nf1idx );
		CHECK_NODE_STAMP( nf2idx );
		CHECK_IC_UPPERBOUND( nf1idx, Ic[ VAR(nf2) ][ 0 ] );
		CHECK_IC_UPPERBOUND( nf2idx, Ic[ VAR(nf1) ][ 0 ] );
		
		IcStamps[ Ic[ nf1idx ][ Ic[ nf1idx][ 0 ] - 1] ] = currentIcTS;

		loc = Ic[ VAR(nf2) ] + 1;
		for (i = loc[ -1 ] - 1; --i; )
		{
			nftmp = loc[ i ]; 
			if( timeAssignments[ nftmp ] == VARMAX ) continue;

			nftmpidx = VAR(-nftmp); 

			if( IcStamps[ -nftmp ] == currentIcTS )	return newNA( nf1 );
			else if( IcStamps[ nftmp ] != currentIcTS )
			{
				CHECK_NODE_STAMP( nftmpidx );
                     		CHECK_IC_BOUND( nftmpidx );
				ADD_CLAUSE( nftmp, nf1 );
			}
#ifdef BIEQ
			else if( nftmp == -nf1 )
			{
/*
				int bieq, nr;			

				if( VeqDepends[ NR(nf1) ] == 0 )
				{
					bieq = NR(nf2);
					nr = NR(nf1);
				}
				else if( VeqDepends[ NR(nf1) ] == 0 )
				{
					bieq = NR(nf1);
					nr = NR(nf2);
				}
				else continue;

                                while( VeqDepends[ bieq ] != 0 )
                                {
                                        bieq = VeqDepends[ bieq ];
                                        if( bieq == nr ) break;
                                }

                                if( (bieq != ALL_VARS) && (bieq != nr) )
                                {
                                        VeqDepends[ bieq ] = nr;
                                        PUSH( bieq, bieq );
					//printf("new bieq %i %i\n", nf1, -nf2);
                                }
*/
			}
#endif
		}

		STAMP_IC2( nf2 );

		loc = Ic[ VAR(nf1) ] + 1;
		for(i = loc[ -1 ] - 1; --i; )
		{
			nftmp = loc[ i ]; 
			if( timeAssignments[ nftmp ] == VARMAX ) continue;

			nftmpidx = VAR(-nftmp); 

			if( IcStamps[ -nftmp ] == currentIcTS )	return newNA( nf2 );
			else if( IcStamps[ nftmp ] != currentIcTS ) 
			{
				CHECK_NODE_STAMP( nftmpidx );
                       		CHECK_IC_BOUND( nftmpidx );
				ADD_CLAUSE( nftmp, nf2 );
			}
		}
		ADD_CLAUSE( nf1, nf2 );	//kan deze later?

	}
	return 1;
}

int newNA( int nrval )
{
	int i, nr, var;

	nr = NR(nrval);
	var = VAR(nrval);

	if( timeAssignments[ nr ] >= VARMAX )
		return( timeValues[ nr ] == nrval );

	if( NNA[ nr ] == -nrval ) 
		return 0;

	else if( NNA[ nr ] == 0 )
	{
		nastack[ nastackSize++ ] = nr;	
		NNA[ nr ] = nrval;
		for( i = 2; i < Ic[ var ][ 0 ]; i++ )
		   if( timeAssignments[ Ic[ var ][ i ] ] < VARMAX )
			if( !newNA( Ic[ var ][ i ] ) )
				return 0;
	}

	return 1;
}

void unfixonevar( int nrval )
{
	int i, nr, *imp;
#ifdef EQ
	int ceqsubst, var;
#endif
#ifdef REPLACEDB
        int *litl, *lloc;
#else
	int *cls, *idx;
	int clsidx;
#endif
	nr = NR( nrval );

	if( satEquivalence[ nr ] )
	{
		add_sat_equivalence( Veq[ nr ][ 1 ] );
		satEquivalence[ nr ] = 0;
		return;
	}

	//printf(" UNFIXING %i\n", nrval);

	/* this variable is unfixed */
	timeAssignments[ nrval ] = 0;
	timeAssignments[ -nrval ] = 0;

#ifdef EQ
	while( !( *( substackp - 1 ) == STACK_BLOCK ) )
	{
		POP( sub, var );
		ceqsubst = Veq[ NR(var) ][ Veq[ NR(var) ][ 0 ]++ ];
		CeqValues1[ ceqsubst ] *= SGN(var);
		CeqSizes1[ ceqsubst ]++; 
        }
        substackp--;
#endif

	/* restore all literals that were removed due to fixing of nrval in backwards order */
#ifdef REPLACEDB
        litl = Lidx[ -nrval ];
        lloc = Lloc[ -nrval ];
        for( i = Vact[ -nrval ] = tmpVact[ -nrval ]; i-- ; )
        {
                Vact[ lloc[ litl[i]     ] ]++;
                Vact[ lloc[ litl[i] + 1 ] ]++;
        }
#else
	cls = Vc   [ VAR(-nrval) ];
	idx = VcLUT[ VAR(-nrval) ];

	for( i = cls[ 0 ]; i > 0; i-- )
	{
		clsidx = cls[ i ];

		/* check if this is the literal that affacted this clause */
		if( CvStamps[ clsidx ] == (CLSMAX + idx[ i ] + 1) )
		{
			Vact[ Cv[ clsidx ][ 0 ] ]++;
			Vact[ Cv[ clsidx ][ 1 ] ]++;
			Vact[ Cv[ clsidx ][ 2 ] ]++;

			CvStamps[ clsidx ] = 0;
		}
	}
#endif
	/* restore all clauses that were removed due to fixing of nrval in backwards order */
#ifdef REPLACEDB
        litl = Lidx[ nrval ];
        lloc = Lloc[ nrval ];
        for( i = Vact[ nrval ] = tmpVact[ nrval ]; i--; )
        {
                Vact[ lloc[ litl[i]     ] ]++;
                Vact[ lloc[ litl[i] + 1 ] ]++;
        }
#else
	cls = Vc   [ VAR(nrval) ];
	idx = VcLUT[ VAR(nrval) ];

	for( i = cls[ 0 ]; i > 0; i-- )
	{
		clsidx = cls[ i ];

		/* check if this is the literal that affacted this clause */
		if( CvStamps[ clsidx ] == CLSMAX + idx[ i ] + 1 )
		{
			Vact[ Cv[ clsidx ][ 0 ] ]++;
			Vact[ Cv[ clsidx ][ 1 ] ]++;
			Vact[ Cv[ clsidx ][ 2 ] ]++;

			CvStamps[ clsidx ] = 0;
		}
	}
#endif
        imp = Ic[ VAR(-nrval) ] + 1;
        for( i = imp[ -1 ] - 1 ; --i; )
                Ic_dead[ -imp[ i ] ]--;

	/* restore Q */
	restoreACT( TO_MAR( nr ) );
}


#ifdef DEBUGGING
/*
	--------------------------------------------------------------------------------------------------------------
	----------------------------------------[ debug information printing ]----------------------------------------
	--------------------------------------------------------------------------------------------------------------
*/

void printCv()
{
	int i, j;

	DEBUG( RC, "Cv\n" );
	DEBUG( RC, "--\n" );

	for( i = 0; i < nrofclauses; i++ )
	{
		DEBUG( RC, "clause %i ( " , i );
	
		for( j = 0; j < 3; j++ )
		{
			DEBUG( RC,  "%i ", Cv[ i ][ j ] );
		}
		DEBUG( RC, ")\n" );
	}
}

/*
void printCvII()
{
	int i, j;

	DEBUG( RC, "CvII\n" );
	DEBUG( RC, "----\n" );

	for( i = 0; i < nrofclauses; i++ )
	{
		DEBUG( RC, "clause %i: TS = %li, 3->2 = %li, ( ", i, CvII[ i ][ 0 ], CvII[ i ][ 1 ] );
	
		for( j = 2; j < 5; j++ )
		{
			DEBUG( RC, "%li ", CvII[ i ][ j ] );
		}
		DEBUG( RC, ")\n" );
	}
}
*/

void printVc()
{
	int i, j;

	DEBUG( RC, "Vc\n" );
	DEBUG( RC, "--\n" );

	for( i = 0; i < 2 * nrofvars + 1; i++ )
	{
		DEBUG( RC, "var %i #%i ( %s ", i - nrofvars, Vc[ i ][ 0 ], ( timeAssignments[ abs( i - nrofvars ) ] == 0 ? "FREE" : "fixed" ) );
		if( timeAssignments[ abs( i - nrofvars ) ] != 0 )
		{
			DEBUG( RC, "on %s ): ( ", ( timeAssignments[ abs( i - nrofvars ) ] < 0 ? "false" : "true" ) ); 
		}
		else
		{
			DEBUG( RC, "): ( " );
		}

		for( j = 0; j < Vc[ i ][ 0 ]; j++ )
		{
			DEBUG( RC, "%i ( %i ) ", Vc[ i ][ j + 1 ], VcLUT[ i ][ j + 1 ] );
		}
		DEBUG( RC, ")\n" );
	}
}

void printIc()
{
	int i, j;

	DEBUG( RC, "Ic\n" );
	DEBUG( RC, "--\n" );

	for( i = 0; i < 2 * nrofvars + 1; i++ )
	{
		DEBUG( RC, "var %i ( ptr = %i, #orig 2-clauses = %i ): ", i - nrofvars, Ic[ i ][ 0 ], Ic[ i ][ 1 ] );
		for( j = 2; j < Ic[ i ][ 0 ]; j++ ) 
		{
			DEBUG( RC, "%i ", Ic[ i ][ j ] );
		}
		DEBUG( RC, "\n" );
	}
}

/*
void printIIc()
{
	int i, j;

	DEBUG( RC, "IIc\n" );
	DEBUG( RC, "---\n" );

	for( i = 0; i < 2 * nrofvars + 1; i++ )
	{
		DEBUG( RC, "var %i ( TS = %li, ptr = %i ): ", i - nrofvars, IIcStamps[ i ], IIc[ i ][ 0 ] );
		for( j = 1; j < IIc[ i ][ 0 ]; j++ )
		{
			DEBUG( RC, "%i ", IIc[ i ][ j ] );
		} 
		DEBUG( RC, "\n" );
	}
}


void printStack()
{
	int *tmp,i;
	
	tmp = impstackp;
	
	while( tmp > impstack )
	{
//		POP( r, i );
		i = *( --tmp );
		if( i == 0 )
		{		
			DEBUG( RC, "STACK_BLOCK\n" );
		}
		else
		{
			DEBUG( RC, "%i\n", i );
			i = *( --tmp );
			DEBUG( RC, "%i\n", i - nrofvars );
		}
	}
	
//	impstackp = tmp;
}

void printFixStack()
{
	int *tmp, i;
	
	tmp = fstackp;
	
	while( fstackp > fstack )
	{
		POP( f, i );		
		DEBUG( RC, "%i\n", i );
	}
	
	fstackp = tmp;
}
	
void printChecksum()
{
	int i, sum;

	sum = 0;
	for( i = 0; i < nrofclauses; i++ ) sum += Cv[ i ][ 0 ];
	for( i = 0; i < nrofvars + 1; i++ ) sum += isFixed[ i ];
	DEBUG( RC, "CHECKSUM: %i\n", sum );
}


void printDeHeleHandel()
{
	printVc();
	DEBUG( RC,"\n\n" );
	printCv();
	DEBUG( RC,"\n\n" );
	printCvII();
	DEBUG( RC,"\n\n" );
	printIc();
	DEBUG( RC,"\n\n" );
	printIIc();
	DEBUG( RC,"\n\n" );
	printStack();
}
*/

#endif
