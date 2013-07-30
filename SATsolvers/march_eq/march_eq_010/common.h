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

#ifndef __COMMON_H__
#define __COMMON_H__

/*
        --------------------------------------------------------------------------------------------------------------
        -------------------------------------------------[ defines ]--------------------------------------------------
        --------------------------------------------------------------------------------------------------------------
*/
/* main... */
#define EXIT_CODE_UNKNOWN	0
#define EXIT_CODE_SAT		10
#define EXIT_CODE_UNSAT		20
#define EXIT_CODE_ERROR		1

/* array's */
#define INITIAL_ARRAY_SIZE	3

/* solving... */
/* percentmar parameter */
#define PERCENT_MAR_SWEEPS	20			/* == +- 12.5 % */
#define PERCENT_MAR_VARS	250			/* == +- 12.5 % */
#define PERCENT			10
#define STACK_BLOCK		0
#define ABS_MIN_DIFF_SCORE	-1
#define TO_BE_SURE_MARGIN	10      		/* just to be sure... */

#define SAT			1
#define UNSAT			0
#define UNKNOWN			-1

#define MAX			0x40000000
#define ALL_VARS		0x40000000
//#define ALL_VARS		nrofvars + 1
#define VARMAX			0x40000000
#define CLSMAX			0x40000000
#define CLSMAX1			0x40000001
#define CLSMAX2			0x40000002
#define CLSMAX3			0x40000003

#define SGN( __a )		( __a < 0 ? -1 : 1 )
#define VAR( __a )		( __a + nrofvars )
#define NR( __a )		( abs( __a ) )

#define OFFSET1                 0x50000000
#define OFFSET2                 0x60000000
#define OFFSET3                 0x90000000
#define LITMASK                 0x0FFFFFFF

#define REPLACEDB
#define EQ

#ifndef EQ
	#define AUTARKY
	#define COMPLEXDIFF
#else
	#define QXCONST		11
	#define QXBASE		25
#endif

//#define LOCALSUBST

#define BIEQ

//#define FIND_EQ
//#define PRINT_FORMULA

#define ROOTLOOK

#define CHAINPLUS
//#define QVECTOR
#define RESOLVENT

//#define ACCURATE

#define CYCLEPARSER

#define INTELLOOK
//#define PROP

//#define UNITRESOLVE_NA

/* obscure macro's om te interfacen met mar.c */
#define FROM_MAR( __a )		( __a + 1 )
#define TO_MAR( __a )		( __a - 1 )

/* lookahead */
#define ITERATE_LOOKAHEAD

/* miscellaneous */
//#define DEBUGGING
#define PROGRESS_BAR

/*
        --------------------------------------------------------------------------------------------------------------
        -------------------------------------------------[ typedef's ]------------------------------------------------
        --------------------------------------------------------------------------------------------------------------
*/

struct timeAssignment
{
    int stamp;
    int value;

};

typedef unsigned int tstamp;

struct resolvent
{
    int stamp;
    int literal;
};

struct treeNode
{
    int literal;
    int gap;

};

/*
        --------------------------------------------------------------------------------------------------------------
        -------------------------------------------------[ MACRO'S ]--------------------------------------------------
        --------------------------------------------------------------------------------------------------------------
*/

#define PUSH( stk, value ) \
	{ \
	if( stk##stackp >= ( stk##stack + stk##stackSize ) ) \
	{ \
		int __tmp; \
		stk##stackSize *= 2; \
		__tmp = stk##stackp - stk##stack; \
		stk##stack = (int *) realloc( stk##stack, stk##stackSize*sizeof(int)); \
		stk##stackp = stk##stack + __tmp; \
	} \
        *stk##stackp = value; \
        stk##stackp++; \
	}

#define POP( stack, value ) \
	{ \
        stack##stackp--;        \
        value = ( *stack##stackp ); \
	}

#define POP_RECURSION_STACK_TO_DEV_NULL \
	{ \
        rstackp--;      \
	}

#define POP_IMPLICATION_STACK_TO_DEV_NULL \
	{ \
        istackp--;      \
	}

#define POP_BACKTRACK_RECURSION_STACK \
	{ \
        rstackp--;      \
        unfixonevar( *rstackp ); \
	}

#define POP_BACKTRACK_LOOKAHEAD_STACK \
	{ \
        lstackp--;      \
        unfixonevarLookahead( *lstackp ); \
	}

#define CHECK_IC_BOUND( ic ) \
{ \
        if( IcLength[ ic ] <= Ic[ ic ][ 0 ] ) \
        { \
             Ic[ ic ] = (int*) realloc( Ic[ ic ], sizeof( int ) * 2 * ( IcLength[ ic ] + 1 ) ); \
             IcLength[ ic ] += IcLength[ ic ] + 1; \
        }\
}

#define CHECK_IC_UPPERBOUND( ic, size ) \
{ \
        if( IcLength[ ic ] <= (Ic[ ic ][ 0 ] + size) ) \
        { \
             Ic[ ic ] = (int*) realloc( Ic[ ic ], sizeof( int ) * (2 * IcLength[ ic ] + size + 1) ); \
             IcLength[ ic ] += IcLength[ ic ] + 1 + size; \
        }\
}

#define CHECK_VEQ_BOUND( vq ) \
{ \
        if( VeqLength[ vq ] <= Veq[ vq ][ 0 ] ) \
        { \
             VeqLength[ vq ] += VeqLength[ vq ] + 1; \
             Veq[ vq ] = (int*) realloc( Veq[ vq ], sizeof( int ) * VeqLength[ vq ]  ); \
             VeqLUT[ vq ] = (int*) realloc( VeqLUT[ vq ], sizeof( int ) * VeqLength[ vq ]  ); \
        }\
}


/*
        --------------------------------------------------------------------------------------------------------------
        ------------------------------------------------[ variables ]-------------------------------------------------
        --------------------------------------------------------------------------------------------------------------
*/

#ifdef REPLACEDB
int *VlTable;
int **Vl;

int *VcTable;
int **Lidx, **Lloc;

int *Vmax;
double *VbinCount;
int *litlist;

int *Cl;
int *tmpVact;

int **Cidx;
#endif

/* fil */
int *fil, *filold, nroffils;
int *filArray, *filCount, filArrayLength;

int *preBieq, preBieqSize;

int *diff;

int *eqV;
double *lengthWeight;

int *Ic_dead;

int sateqCounter;

/* mar */
double *MARvalues;
int *lookaheadArray, lookaheadArrayLength;
int *ACTvalues;
int currentIcTS, *IcStamps;
int currentVar;

int freeNrofvars;
int _CvSize;

/* statistics */
int nrofvars, nrofclauses, nrofceq, nroforigvars;
int originalNrofvars, originalNrofclauses;
int freevars;
int depth;
int NAflag;

int **Ceq, **Veq, **VeqLUT, *CeqValues, *CeqSizes, *CeqSizes2, *CeqSizesNA;
tstamp *CeqStamps;
int *CeqDepends, *VeqDepends;
int *VeqLength;
int *CeqSizes1, *CeqValues1;

int *satEquivalence;

/* weight */
int *neg2;
int *q2, *q3, S2, S3;
int **Q2, **Q3;
int *qLarge;
int qSize;

/* dependency */
int andDepend;
int *andDepends;
int *andchk;
int *independ;

/* data structure */
tstamp *timeAssignments, *nodeStamps, *CvStamps;
int **Cv, *Clength, **Vc, **Ic, **VcLUT, *timeValues, *IcLength, *Vact;
int *VcStamps, *VcValues, *NNA;

int **CvLong, **VcLong, *CvLongSizes, *CvLongSizes1, *CvLongSizes2;
tstamp *CvLongStamps;

/* various stacks */
int *rstack, *rstackp, rstackSize;
int *newbistack, *newbistackp, newbistackSize;
int *impstack, *impstackp, impstackSize;
int *lstack, *lstackp, *lstackp_save, lstackSize;
int addImpStackPtr, *addImpStack;
int *nastack, *nastackp, nastackSize;
int *bieqstack, *bieqstackp, bieqstackSize;
int *substack, *substackp, substackSize;

/* lookahead */
tstamp currentNodeStamp;
int maxLookPlace;
int maxSite;
int lookvars;
double maxRatio;
int *NA, NAsize;
tstamp currentTimeStamp, NAmax;
int iterCounter;

struct treeNode *treeArray;

int currentLookStamp;
int *lookStamps;

/* accounting */
int nodeCount;
int lookAheadCount;
int unitResolveCount;
int naCounter, naCounter2;
int lookDead, mainDead;

#endif
