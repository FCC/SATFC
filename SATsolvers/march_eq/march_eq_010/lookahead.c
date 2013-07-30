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
#include <math.h>

#include "common.h"
#include "lookahead.h"
#include "flads.h"
#include "debug.h"
#include "solver.h"

double binCount;
//double *VbinCount;
int toNodeStamp, nodeStampValue;
int *fixstack, *fixstackp, fixstackSize;
int *resstack, *resstackp, resstackSize;
int maxDiffVar;


tstamp timeGap;

/* globale lookahead variabeles */
int parent, VARparent;

#define DEATHMASK		1

#ifdef UNITRESOLVE_NA
   #define HANDLE_NA( a )	unitresolve( a )
#else
   #define HANDLE_NA( a )	impNA( a)
#endif 

#define EQSGN( a )		( ((a & DEATHMASK) > 0) ? -1: 1 )
//#define EQSGN( a )		( ((a & DEATHMASK) << 1) - 1 )

/* macro om aan te geven dat een variabele gezet gaat worden. */
#ifdef EQ
#define STAMP( __a ) \
{ \
	timeAssignments[ -__a ] = currentTimeStamp + 1; \
	timeAssignments[ __a ] = currentTimeStamp; \
	timeValues[ -__a ] = __a; \
	timeValues[ __a ] = __a; \
}
#else
#define STAMP( __a ) \
{ \
	timeAssignments[ -__a ] = currentTimeStamp + 1; \
	timeAssignments[ __a ] = currentTimeStamp; \
}
#endif

/* voegt de implicatie parent -> child toe... */
#define CHILD_IMPLICATION( __VARparent, __child ) \
{ \
	Ic[ __VARparent ][ ( Ic[ __VARparent ][ 0 ] )++ ] = __child; \
}

/*
	kijkt of -child al geNodeStamped is en doet dit zo nodig...
	voegt vervolgens de implicatie -child -> -parent toe.
*/
#define PARENT_IMPLICATION( __VARchildNEG, __parentNEG ) \
{ \
	if( nodeStamps[ __VARchildNEG ] != currentNodeStamp ) \
	{ \
		nodeStamps[ __VARchildNEG ] = currentNodeStamp ; \
		PUSH( imp, __VARchildNEG ); \
		PUSH( imp, Ic[ __VARchildNEG ][ 0 ] ); \
	} \
	Ic[ __VARchildNEG ][ ( Ic[ __VARchildNEG ][ 0 ] )++ ] = __parentNEG; \
}

/* STAMP alle clauses waar VAR positief in voorkomt. */

#define ADD_IMP( __child ) \
{ \
	toNodeStamp = 1; \
 \
	STAMP( __child ); \
	PUSH( fix, __child ); \
 \
	CHECK_IC_BOUND( VARparent ); \
	CHECK_IC_BOUND( VAR##__child##NEG ); \
 \
	CHILD_IMPLICATION( VARparent, __child ); \
	PARENT_IMPLICATION( VAR##__child##NEG, -parent ); \
}

void initLookahead()
{
	int i, j;

	currentTimeStamp = 0;
	currentLookStamp = 0;
	lookvars = nrofvars; //???
	timeGap = nrofvars * 4;
	NAmax = timeGap;
	
	NA     = (int*) malloc( ( nrofvars ) * sizeof ( int ) );
	NAtree = (int*) malloc( ( nrofvars ) * sizeof ( int ) );

	//treeArray = (struct treeNode* ) malloc( ( (int)( nrofvars * PERCENT / 50 ) ) * sizeof ( struct treeNode ) );
	treeArray = (struct treeNode* ) malloc( ( (int)( nrofvars * 2 ) ) * sizeof ( struct treeNode ) );

        VbinCount = (double *) malloc( sizeof( double ) * ( 2 * nrofvars + 1 ) );
	for( i = 0; i < ( 2 * nrofvars + 1 ); i++ )	VbinCount[ i ] = 0;

       	lengthWeight = (double *) malloc( sizeof( double ) * ( 100 ) );
	for( i = 0; i < 99; i++ )	lengthWeight[ i ] = 0;

       	eqV = (int *) malloc( sizeof( int ) * ( 2 * nrofvars + 1 ) );
	for( i = 0; i < ( 2 * nrofvars + 1 ); i++ )	eqV[ i ] = 0;

	/* allocate fix stack */
	/* there can be max. nrofvars literals on the fixstack... */
	fixstackSize = nrofvars + TO_BE_SURE_MARGIN;
	fixstack = (int*) malloc( sizeof( int ) * fixstackSize );
	fixstackp = fixstack;

	resstackSize = nrofvars + TO_BE_SURE_MARGIN;
	resstack = (int*) malloc( sizeof( int ) * resstackSize );
	resstackp = resstack;

	lookStamps = (int *) malloc( sizeof( int ) * ( 2 * nrofvars + 1 ) );
	for( i = 0; i < ( 2 * nrofvars + 1 ); i++ )
		lookStamps[ i ] = 0;
	lookStamps += nrofvars;

        /* flads zooi */
        assignment_array = (struct assignment *)malloc(sizeof(struct assignment)*(nrofvars+1)*2);
        assignment_ptrs = (struct assignment **)malloc(sizeof(void *)*(nrofvars+1)*2);

        assignment_array += nrofvars;

        for( i = 1; i < nrofvars+1; i++ )
            for( j = 0; j < 2; j++ )
        {
	    int varnr;	
	    struct assignment *assignment;

	    varnr = i * ( (2 * j) - 1 ); 
	    assignment = &assignment_array[ varnr ];

            assignment->varnr = varnr;
            assignment->truth = j;
            assignment->tree_stamp = -1;
	    assignment->incoming_size = 16;

	    assignment->bread_crumb = 0;
	    assignment->complement = &assignment_array[ -varnr ];

	    assignment->incoming = (struct assignment **)malloc( 16 * sizeof(void *));
        }
        tree_stamp = 0;


#ifdef QXBASE
	for( i = 2; i < 100; i++ )
		lengthWeight[ i ] = QXCONST * 0.5 * pow( 0.6 + QXBASE*0.01, i );
#else
	for( i = 2; i < 100; i++ )
		lengthWeight[ i ] = 0;
#endif


	printf("c lengthWeight[ 2 ] = %.2f : %.2f\n", lengthWeight[ 2 ], lengthWeight[ 2]/lengthWeight[ 2 ] );
	printf("c lengthWeight[ 3 ] = %.2f : %.2f\n", lengthWeight[ 3 ], lengthWeight[ 3]/lengthWeight[ 2 ] );
	printf("c lengthWeight[ 4 ] = %.2f : %.2f\n", lengthWeight[ 4 ], lengthWeight[ 4]/lengthWeight[ 2 ] );
	printf("c lengthWeight[ 5 ] = %.2f : %.2f\n", lengthWeight[ 5 ], lengthWeight[ 5]/lengthWeight[ 2 ] );
}

void destroyLookahead()
{
	free( fixstack );
	free( NA );
}

int propCeq( int varnr )
{
        int i, j, nr, ceqidx;
	int var, value;

        nr = NR( varnr );

        for( i = Veq[ nr ][ 0 ] - 1; i > 0; i-- )
        {
                ceqidx = Veq[ nr ][ i ];

		//if( CeqSizes1[ ceqidx ] < 3 )	continue;

		if( CeqStamps[ ceqidx ] == CLSMAX )	// Kan dit weg? of later?
			continue;

		var = 0;
		value = 1; 

	        for( j = CeqSizes1[ ceqidx ] - 1; j >= 0; j-- )
		{
		    	if( timeAssignments[ Ceq[ ceqidx ][ j ] ] >= currentTimeStamp )
				value *= EQSGN( timeAssignments[ Ceq[ ceqidx ][ j ] ] );
		    	else if( !var )
  		               	var = Ceq[ ceqidx ][ j ];
			else
			{
                        	//binCount += lengthWeight[ CeqSizes1[ ceqidx ] ];
                        	binCount += lengthWeight[ j + 2 ];
				goto ceqend;
			}
		}

		if( var )
		{
		        if( propImp( var * value * CeqValues1[ ceqidx ] ) == 0 ) return 0;
		}
		else if( value == -CeqValues1[ ceqidx ] )
			return 0;

		ceqend :
        }
	return 1;
}

int impParent( int nrval )
{
	int i, child, *children;
        int *_fixstackp;
	int stackSize;

	parent = nrval;
	VARparent = VAR( parent );

	binCount = 0;

	//printf("PARENT %i\n", parent);

	toNodeStamp = 0;
	nodeStampValue = Ic[ VARparent ][ 0 ];

	/* reset stacks */
	fixstackp = fixstack;
	resstackp = resstack;

	/* STAMP parent om aan te geven dat ie gezet gaat worden */
	STAMP( parent );

        children = Ic[ VARparent ] + 1;
        for( i = children[ -1 ] - 1; --i; )
        {
                child = children[ i ];

		/* is child al gezet? */
		if( timeAssignments[ child ] < currentTimeStamp )
		{
			STAMP( child );
                        *( fixstackp++ ) = child;
                }
        }

        _fixstackp = fixstack;
        while( _fixstackp < fixstackp )
                if( !impChild( *( _fixstackp++ ) ) )
                        return 0;

#ifdef EQ
        if( !propCeq( parent ) ) return 0;
#endif
        if( !prop3Neg( parent ) ) return 0;

        _fixstackp = fixstack;
        while( _fixstackp < fixstackp )
        {
#ifdef EQ
                if( !propCeq( *( _fixstackp ) ) )
                        return 0;
#endif
                if( !prop3Neg( *( _fixstackp++ ) ) )
                        return 0;
        }

        /* als er implicaties zijn toegevoegd bij de parent, dan moet ie genodestampt worden! */
        if( (resstackp > resstack) && ( nodeStamps[ VARparent ] != currentNodeStamp ) )
        {
                nodeStamps[ VARparent ] = currentNodeStamp;
                PUSH( imp, VARparent );
                PUSH( imp, Ic[ VARparent ][ 0 ] );
        }

        stackSize = (int) (resstackp - resstack);
        CHECK_IC_UPPERBOUND( VARparent, stackSize );
        while( resstackp > resstack )
        {
                int lit = *( --resstackp );
                int VARlitNEG = VAR(-lit);

                CHECK_IC_BOUND( VARlitNEG );

                CHILD_IMPLICATION( VARparent, lit );
                PARENT_IMPLICATION( VARlitNEG, -parent );
        }
        return 1;
}


int impChild( int child )
{
        int i, phoet, *phoetlings;

        phoetlings = Ic[ VAR(child) ] + 1;

        for( i = phoetlings[ -1 ] - 1; --i; )
        {
                phoet = phoetlings[ i ];

		/* is phoet al gezet? */
		if( timeAssignments[ phoet ] >= currentTimeStamp )
		{
			/* zo ja, is ie tegengesteld gezet? */
			if( timeAssignments[ phoet ] & DEATHMASK )
				return 0;
		}
		else
		{
			STAMP( phoet );
                        *( fixstackp++ ) = phoet;
		}
	}
	return 1;
}

int propImp( const int lit )
{
        int *_fixstackp = fixstackp;

        STAMP( lit );
        *( fixstackp++ ) = lit;
        *( resstackp++ ) = lit;

        while( _fixstackp < fixstackp )
                if( !impChild( *( _fixstackp++ ) ) )
                        return 0;

#ifdef ACCURATE
		iterCounter++;
#endif

        return 1;
}


int prop3Neg( const int nrval )
{
        int i, lit1, lit2;

#ifdef REPLACEDB
        const int *lidx = Lidx[ -nrval ];
        const int *lloc = Lloc[ -nrval ];
#else
        /* prop3Neg kijkt naar de clauses waarin nrval negatief voorkomt. */
        const int *cls = Vc   [ VAR(-nrval) ];
        const int *idx = VcLUT[ VAR(-nrval) ];
#endif
#ifdef REPLACEDB
        for( i = Vact[ -nrval ]; i--; )
        {
           lit1 = lloc[ lidx[ i ]     ];
           lit2 = lloc[ lidx[ i ] + 1 ];
#else
        for( i = cls[ 0 ] + 1; --i; )
        {
                /* ligt deze clause al uit de formule? */
           if( CvStamps[ cls[ i ] ] < currentTimeStamp )
           {
                /* bereken de twee andere literals in deze clause. */
                const int *loc = Cv[ cls[ i ] ] + idx[ i ] + 1 ;

                lit1 = loc[ 0 ];
                lit2 = loc[ 1 ];
#endif
		/* is lit1 al gezet? */
		if( timeAssignments[ lit1 ] >= currentTimeStamp )
		{
		   if( timeAssignments[ lit1 ] & DEATHMASK )
		   {
			/*
				als lit2 ook al is gezet, is er een conflict!
			*/
			if( timeAssignments[ lit2 ] >= currentTimeStamp )
			{
				if( timeAssignments[ lit2 ] & DEATHMASK )
					return 0;
			}
			else
			{
			/*
				als lit2 nog niet gezet is, dan is dit een 1-clause en moet lit2 gezet worden.
				bovendien impliceert parent dan dus lit2 en andersom.
			*/
				if( propImp( lit2 ) == 0 ) return 0;
			}
		   }
		}
		else if( timeAssignments[ lit2 ] >= currentTimeStamp )
		{
		   if( timeAssignments[ lit2 ] & DEATHMASK )
		   {
			/*
				als lit1 nog niet gezet is, dan is dit een 1-clause en moet lit1 gezet worden.
				bovendien impliceert parent dan dus lit1 en andersom.
			*/
			if( propImp( lit1 ) == 0 ) return 0;
		  }
		}
		else    /* we have a new 2-clause! */
		{
#ifdef COMPLEXDIFF
			binCount += diff[ lit1 ] * diff[ lit2 ];
#else
			binCount += 1;
#endif
		}
#ifndef REPLACEDB
	   }
#endif
	}

	return 1;
}

int impCurrent( int nrval )
{
	/* standaard begin van lookahead op een variabele */
	currentTimeStamp += 2;

	return impParent( nrval );
}


int impNA( int nrval )
{
	unsigned long long _cts;
	int _retval;

	/*
		deze variabele toekenning blijft de gehele lookahead geldig.
		als de lookahead niet doodloopt wordt deze variabele ook
		zondermeer op het gekozen pad gezet.
	*/
	NA[ NAsize++ ] = nrval;

//	printf("NA %i\n", nrval);

	iterCounter++;

	_cts = currentTimeStamp;

	currentTimeStamp = NAmax;

	_retval = impParent( nrval );

	currentTimeStamp = _cts;

	return _retval;
}

int intellook()
{
        int i, iterTmp, lastChanged;

	if( NAmax + 2 * timeGap >= VARMAX )
		cleanFormula(); 

        NAsize = 0;
        currentTimeStamp = NAmax + 6;
        currentLookStamp++;
        NAmax += timeGap;
	lastChanged = 0;

        for( i = 0; i < lookaheadArrayLength ; i++ )
	{
                lookStamps[ lookaheadArray[i] + 1 ] = currentLookStamp;
                lookStamps[ -lookaheadArray[i] - 1 ] = currentLookStamp;
	}

	treebased_lookahead();

	for( i =0; i < NAtreeSize; i++ )
		if( !(HANDLE_NA(NAtree[ i ])) ) return 0;
		

#ifdef ITERATE_LOOKAHEAD
        do
        {
		if( ( currentTimeStamp + 4 * lookaheadArrayLength + 4 ) >= NAmax )
		{
			cleanLookahead();
			lastChanged = 0;
		}

                iterCounter = 0;
                for( i = 0; i < 2 * lookaheadArrayLength; i++ )
                {
                        if( treeArray[ i ].literal == lastChanged ) break;
                        iterTmp = iterCounter;

                        currentTimeStamp += treeArray[ i ].gap;
                        if( !treelookvar( treeArray[ i ].literal ) )	return 0;
                        currentTimeStamp -= treeArray[ i ].gap;

                        if( iterTmp > iterCounter ) lastChanged = treeArray[ i ].literal;
                }
                currentTimeStamp += 4 * lookaheadArrayLength + 2;
        }
        while( iterCounter );
#else
                for( i = 0; i < 2 * lookaheadArrayLength; i++ )
                {
                        currentTimeStamp += treeArray[ i ].gap;
                        if( !treelookvar( treeArray[ i ].literal ) )
                        {
                                currentTimeStamp -= treeArray[ i ].gap;
                                return 0;
                        }
                        currentTimeStamp -= treeArray[ i ].gap;
                }
#endif
        return 1;
}

int treelookvar( const int nrval )
{
        int i;
	const int *loc;

        VbinCount[ VAR(nrval) ] = 0;

        lookAheadCount++;

        if( timeAssignments[ nrval ] >= currentTimeStamp )	//blijven checken
	{
		if( (timeAssignments[ nrval ] < NAmax) && (timeAssignments[ nrval ] & DEATHMASK) )
	                if( !(HANDLE_NA(-nrval)) ) return 0;
		return 1;
	}

        if( !impParent(nrval) )
        {
                if( !(HANDLE_NA(-nrval)) ) return 0;
        }
        else
        {
#ifdef AUTARKY
		if( binCount == 0 )
		{
			if( assignment_array[ nrval ].parent == 0 )
			{
		                 HANDLE_NA(nrval);
				 return 1;
				 //printf("Autarky %i!!!\n", nrval);
			}

			else
			{
				int imp = assignment_array[ nrval ].parent->varnr;
				int varNEG = VAR(-nrval);
				int impVAR = VAR(imp);

				if( timeAssignments[ imp ] == NAmax )
				{
					HANDLE_NA(nrval);
					return 1;
				}



				CHECK_IC_BOUND(varNEG);
				CHECK_IC_BOUND(impVAR);

				PARENT_IMPLICATION(impVAR, nrval);
				PARENT_IMPLICATION(varNEG, -imp);

				//printf("Binary equivalence %i <-> %i\n", nrval, imp);
			}
#ifdef ACCURATE
			iterCounter++;
#endif
		}
#endif
		loc = Ic[ VAR(-nrval) ];
                for( i = 2; i < loc[ 0 ]; i++ )
                {
                        const int imp = loc[ i ];
                        if( (timeAssignments[ imp ] >= currentTimeStamp) &&
                            (timeAssignments[ imp ] < NAmax) )
			{
 			    if( !(timeAssignments[ imp ] & DEATHMASK) )
                            {
                        	naCounter2++;
                                if( !(HANDLE_NA(imp)) ) return 0;
				loc = Ic[ VAR(-nrval) ];
                            }
                            else
                            {
#ifdef BIEQ
	                        int bieq = -imp;
				if( VeqDepends[ NR(nrval) ] != 0 ) return 1;
				//printf("VeqDepends[ %i ] =  %i\n", NR(bieq), VeqDepends[ NR(bieq) ]);

                                while( VeqDepends[ NR(bieq) ] != 0 )
				{
					if( VeqDepends[ NR(bieq) ] != ALL_VARS )
	                                        bieq = VeqDepends[ NR(bieq) ] * SGN(bieq);
					else
						bieq = ALL_VARS;
					if( bieq == nrval ) break;
					//printf("VeqDepends[ %i ] =  %i\n", NR(bieq), VeqDepends[ NR(bieq) ]);
				}

                                if( (bieq != ALL_VARS) && (bieq != nrval) )
                                {
                                        VeqDepends[ NR(bieq) ] = nrval * SGN(bieq);
                                        PUSH( bieq, NR(bieq) );
					//printf("%i is equal to %i\n", nrval, bieq);
					//printf("  VeqDepends[ %i ] =  %i\n", NR(bieq), VeqDepends[ NR(bieq) ]);
		                }
#endif
			     }
			}
                }
                VbinCount[ VAR(nrval) ] = binCount;
        }
        return 1;
}

int get_maxIntellook()
{
        int i, j, current;
	double currentCount;
        double maxDiffScore, diffScore;
        struct assignment *assignment;

        /* set maxDiffScore to unreachable minimum */
        maxDiffScore = -1;
        maxDiffVar = 0;
        maxLookPlace = -1;

        for( i = 0; i < 2 * lookaheadArrayLength; i++ )
        {
                current = treeArray[ i ].literal;
                currentCount = VbinCount[ VAR(current) ];
                assignment = &assignment_array[ current ];
                for( j = 0; j < assignment->nr_incoming; j++ )
                        VbinCount[ VAR(assignment->incoming[ j ]->varnr) ] += currentCount;
        }

        for( i = 0; i < lookaheadArrayLength; i++ )
        {
           current = lookaheadArray[ i ] + 1;
           if( timeAssignments[ current ] < NAmax )
           {
		if( nodeCount < 5 )
			diffScore = VbinCount[ VAR(current) ] * VbinCount[ VAR(-current) ] +
	                            5*VbinCount[ VAR(current) ] + 5*VbinCount[ VAR(-current) ];
		else
	                diffScore = VbinCount[ VAR(current) ] * VbinCount[ VAR(-current) ] * 1024 +
	                            VbinCount[ VAR(current) ] + VbinCount[ VAR(-current) ];


                if( diffScore > maxDiffScore )
                {
                        maxDiffScore = diffScore;
                        maxDiffVar = current;
                        maxLookPlace = i;
                        maxSite = (VbinCount[ VAR(current) ] > VbinCount[ VAR(-current) ])? -1 : 1;
                }
           }
        }
        return maxDiffVar;
}

void cleanLookahead()
{
	int i;

//	printf("c cleaning lookahead\n");

	for( i = 0; i <= nrofvars; i++ )
		if( timeAssignments[ i ] < NAmax )
		{ 
			timeAssignments[ i ] = 0;
			timeAssignments[ -i ] = 0;
		}

	currentTimeStamp = 0;
	timeGap += 2 * nrofvars;

#ifdef EQ
	for( i = 0; i < nrofceq; i++ )
		if( CeqStamps[ i ] < NAmax )
			CeqStamps[ i ] = 0;
#endif

}

void cleanFormula()
{
	int i;

//	printf("c cleaning formula\n");

	for( i = 0; i <= nrofvars; i++ )
		if( timeAssignments[ i ] < VARMAX )
		{ 
			timeAssignments[ i ] = 0;
			timeAssignments[ -i ] = 0;
		}

	for( i = 0; i < nrofclauses; i++ )
		if( CvStamps[ i ] < CLSMAX )
			CvStamps[ i ] = 0;
#ifdef EQ
	for( i = 0; i < nrofceq; i++ )
		if( CeqStamps[ i ] < CLSMAX )
			CeqStamps[ i ] = 0;
#endif
	currentTimeStamp = 0;
	NAmax = 0;
}

int lookahead()
{
	int i, j, v, _tmpl, _tmpr, var, imp, lastChanged, iterTmp;
	double binCountLeft, binCountRight;
	double maxDiffScore, diffScore;

	/* set maxDiffScore to unreachable minimum */
	maxDiffScore = -1;
	maxDiffVar = 0;
	maxLookPlace = -1;

	if( NAmax + 4 * timeGap + 2 >= VARMAX )
		cleanFormula(); 

	lastChanged = -1;

//	printf("\nNEW LOOKAHEAD: Size = %i\n", lookaheadArrayLength);


	NAsize = 0;
	currentTimeStamp = NAmax + 2;
	NAmax += timeGap;


#ifdef ITERATE_LOOKAHEAD
	do
	{
#endif
		if( ( currentTimeStamp + 4 * lookaheadArrayLength ) >= NAmax )
		{
			cleanLookahead();
			lastChanged = 0;
		}

		iterCounter = 0;

		/* lookaheadArray and lookaheadArrayLength defined in common.h */
		for( i = 0; i < lookaheadArrayLength; i++ )
		{
			iterTmp = iterCounter;

			v = FROM_MAR( lookaheadArray[ i ] );

			if( v == lastChanged ) break;

			lookAheadCount++;

			if( timeAssignments[ NR( v ) ] >= NAmax )
				continue;

			binCountLeft = 0;
			binCountRight = 0;

			//printf("current Lookaheadvar = %i\n", v);
			//printf("%i : ", v);

			_tmpl = impCurrent( v );

			if( _tmpl )
			{

				//printf("\n");
	
				if( (int) binCount )
					binCountLeft = binCount;

				//printf("%i : ", -v);

				_tmpr = impCurrent( -v );

				//printf("\n");


//				if( !binCount || !binCountLeft )
//					printf("AUTARKIES %i\n", v );

#ifdef AUTARKIES
				if( !_tmpr || !binCountLeft )	 	//NA of autarkies?
				{	if( !(HANDLE_NA( v )) ) return 0; }
				else if( !binCount ) 			//autarkies?
				{	if( !(HANDLE_NA( v )) ) return 0; }
#else
				if( !_tmpr ) 
				{	if( !(HANDLE_NA( v )) ) return 0; }
#endif
				else
					if( binCount )
						binCountRight = binCount;
			}
			else
			{
				//	printf("current Lookaheadvar = %i\n", -v);

				_tmpr = HANDLE_NA( -v );

				if( _tmpr )
						binCountRight = binCount;
				else
					return 0;
			}

			if ( iterCounter > iterTmp )	
				lastChanged = v;

			if( _tmpl && _tmpr )
			{
				/*
					necessary assignments in de vorm A = 1, A = 1
					de Ic van v bevat alle literals die in de linker tak gelden.
					als een variabele die bij een van deze literals hoort de huidige timestamp heeft
					en bovendien op dezelfde waarde gezet zou worden, dan is dit een NA.
				*/
				var = VAR( v );
				for( j = 2; j < Ic[ var ][ 0 ]; j++ )
				{
					imp = Ic[ var ][ j ];
					if( timeAssignments[ imp ] == currentTimeStamp )	//NB ==
					{
						naCounter2++;
						if( !(HANDLE_NA( imp )) ) return 0;			
					}
				}

				if( nodeCount < 5 )
					diffScore = binCountLeft * binCountRight + 5*binCountLeft + 5*binCountRight;
				else
					diffScore = binCountLeft * binCountRight * 1024 + binCountLeft + binCountRight;

				//printf("diffScore %i = %i %i %i\n", v, (int)diffScore, 
				//	(int)binCountLeft, (int) binCountRight );

				if( diffScore > maxDiffScore )
				{
					maxDiffScore = diffScore;
					maxDiffVar = v;
					maxLookPlace = i;
					maxSite = (binCountLeft > binCountRight)? -1 : 1;
				}
			}

		}
#ifdef ITERATE_LOOKAHEAD
	}
	while( iterCounter );
#endif

#ifdef DEBUGGING
	if( maxDiffVar != 0 )
	{
		DEBUG( LA, "lookahead:: diff chose variable %i which has score %2f.\n", maxDiffVar , maxDiffScore );
	}
	else
	{
		DEBUG( LA, "lookahead:: NO SUITABLE VARIABLE FOUND!!\n" );
		DEBUG( LA, "lookahead:: LookaheadArray: " );
		for( i = 0; i < lookaheadArrayLength; i++ )
			DEBUG( LA, "%i ", FROM_MAR( lookaheadArray[ i ] ) );
		DEBUG( LA, "\n" );
		DEBUG( LA, "lookahead:: NA: " );
		for( i = 0; i < NAsize; i++ )
			DEBUG( LA, "%i ", NA[ i ] );
		DEBUG( LA, "\n" );
	}
#endif
	//printf("\n");
	return 1;
}


void get_NA( int **_NA, int *_NAsize )
{
	*_NA = NA;
	*_NAsize = NAsize;
}

int get_maxDiffVar()
{
	return maxDiffVar;
}

/*
void setTimeStamp( int nrval )
{
	timeAssignments[ nrval ] = (unsigned long int) MAX;
	timeValues[ nrval ] = nrval;
	timeAssignments[ -nrval ] = (unsigned long int) MAX;
	timeValues[ -nrval ] = nrval;
}

void unsetTimeStamp( int nrval )
{
	timeAssignments[ nrval ] = 0;
	timeAssignments[ -nrval ] = 0;
}

void setClause( int clidx )
{
	CvII[ clidx ][ 0 ] = (unsigned long int) MAX;
}

void unsetClause( int clidx )
{
	CvII[ clidx ][ 0 ] = 0;
}

*/
