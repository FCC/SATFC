/* MARCHII! Satisfiability Solver
 
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
#include "mar.h"
#include "debug.h"
#include "equivalence.h"
#include <math.h>
#include <stdlib.h>
#include <malloc.h>

/* MAR branchrule implementation */

/* filthy defines */
#define NEG( a )	( ( a << 1 ) - 1 )
#define W3 		1
#define W2 		11
//#define WEIGHT

/*

    Q2[ lit2 ][ Qinverse[ lit1 ][ index ]] += truth; \
    Q3[ lit2 ][ Qinverse[ lit1 ][ index ]] -= truth; \ 

    Q2[ lit1 ][ Qinverse[ lit2 ][ index ]] += truth; \
    Q3[ lit1 ][ Qinverse[ lit2 ][ index ]] -= truth; \ 

    Q3[ lit2 ][ Qinverse[ lit1 ][ index ]] += truth; \ 
    Q3[ lit1 ][ Qinverse[ lit2 ][ index ]] += truth; \ 


*/

/* obscure macros */
#define CHANGE_CLAUSE2( posneg ) \
{ \
    if(  lit1 > lit2 ) \
    { \
      for( i = 1; i <= Qlist[ lit1 ][ 0 ]; i++ ) \
        if( Qlist[ lit1 ][ i ] == lit2 ) { index = i; break; } \
      Q2[ lit1 ][ index ] +=  posneg; \
      Q3[ lit1 ][ index ] -=  posneg; \
    } \
    else \
    { \
      for( i = 1; i <= Qlist[ lit2 ][ 0 ]; i++ ) \
        if( Qlist[ lit2 ][ i ] == lit1 ) { index = i; break; } \
      Q2[ lit2 ][ index ] +=  posneg; \
      Q3[ lit2 ][ index ] -=  posneg; \
    } \
}

#define CHANGE_CLAUSE3( posneg ) \
{ \
    if( lit1 > lit2 ) \
    { \
      for( i = 1; i <= Qlist[ lit1 ][ 0 ]; i++ ) \
        if( Qlist[ lit1 ][ i ] == lit2 ) { index = i; break; } \
      Q3[ lit1 ][ index ] +=  posneg; \
    } \
    else \
    { \
      for( i = 1; i <= Qlist[ lit2 ][ 0 ]; i++ ) \
        if( Qlist[ lit2 ][ i ] == lit1 ) { index = i; break; } \
      Q3[ lit2 ][ index ] +=  posneg; \
    } \
}

/* variables */
int *Qinput;
int *Qhits;
int *Qlocation;
int *independLUT;
int **Qlist;
int **Qinverse;

double tot = 0.0;
double totn = 0.0;

/* weight for 2-variable clauses */
double w2 = W2;
double w3 = W3;
double w2Square;

#define NARROW_LISTARRAY \
{ \
        som = 0; \
        for( i = 0; i < lookaheadArrayLength; ) \
                if( ( (int) MARvalues[ lookaheadArray[ i ] ] ) >= gemiddelde ) \
                { \
                        som += (int) MARvalues[ lookaheadArray[ i ] ]; \
                        ++i; \
                } \
                else \
                        lookaheadArray[ i ] = lookaheadArray[ --lookaheadArrayLength ]; \
                if( !lookaheadArrayLength ) \
                        return; \
}

#define NARROW_LISTARRAY2 \
{ \
        som = 0; \
        for( i = 0; i < lookaheadArrayLength; ) \
                if( ( (int) ACTvalues[ lookaheadArray[ i ] ] ) >= gemiddelde ) \
                { \
                        som += (int) ACTvalues[ lookaheadArray[ i ] ]; \
                        ++i; \
                } \
                else \
                        lookaheadArray[ i ] = lookaheadArray[ --lookaheadArrayLength ]; \
                if( !lookaheadArrayLength ) \
                        return; \
}

void initACT()
{
	int i, index, j, *loc;
	int pos, neg;
#ifdef EQ
	double tmp;
#endif

/*
        for( j = 0; j < freevars; j++ )
        {
                index = independ[ j ] + 1;

		if( Veq[ index ][ 0 ] == 2 )
			if( (CeqStamps[ Veq[ index ][ 1 ] ] < CLSMAX ) &&
			    (Vact[ index ] + Vact[ -index ] == 0) &&
			    (Ic[ VAR( index) ][ 0 ] == Ic_dead[  index ] ) &&
			    (Ic[ VAR(-index) ][ 0 ] == Ic_dead[ -index ] ) )
			{
	                        PUSH( r, index );
        	                satEquivalence[ index ] = 1;
                	        remove_sat_equivalence( Veq[ index ][ 1 ] );
				continue;
			}

		if( VeqDepends[ index ] != 0 )
			continue;

                pos = Vact[ -index ] + Ic[ VAR(index) ][ 0 ] - Ic_dead[ index ];
                neg = Vact[ index ] + Ic[ VAR(-index) ][ 0 ] - Ic_dead[ -index ];

		if( (pos == 0) && (Veq[ index ][ 0 ] == 1) && (neg > 0) ) { fixonevar(index); j=-1; continue; }
                else if( (neg == 0) && (Veq[ index ][ 0 ] == 1) && (pos > 0) ) { fixonevar(-index); j=-1; continue; }
        }
*/

#ifdef EQ
//     if( nrofceq != 0)
	for(j = 1; j <= nrofvars; j++ )
	{
		eqV[ j ] = 0;
		eqV[ -j ] = 0;
	}
#endif

        for( j = 0; j < freevars; j++ )
        {
	  	index = independ[ j ] + 1;
#ifdef EQ
		tmp = 0;
	 
		if( Veq[ index ][ 0 ] > 1 )
		        for( i = Veq[ index ][ 0 ] - 1; i > 0; i-- )
				tmp += lengthWeight[ CeqSizes1[ Veq[ index ][ i ] ] - 1 ];

		eqV[ index ] = Vact[ index ] + (int) tmp;
		eqV[ -index ] = Vact[ -index ] + (int) tmp;
#endif
#ifdef COMPLEXDIFF
                diff[ index ] = (int)(Vact[ -index ] +
                        4 * (Ic[ VAR(index) ][ 0 ] - Ic_dead[ index ]) );

                diff[ -index ] = (int)(Vact[ index ] +
                        4 * (Ic[ VAR(-index) ][ 0 ] - Ic_dead[ -index ]) );
#endif
	}		

	//if( (PERCENT == 100) && (nodeCount != 0) )
	//{
	        //for( j = 0; j < freevars; j++ )
		//{
		// 	index = independ[ j ];
		//	ACTvalues[ index ] = (int) sqrt(VbinCount[ index ] * VbinCount[ -index ]);
		//}
	//	return;
	//}

        for( j = 0; j < freevars; j++ )
	{
	  	index = independ[ j ] + 1;

		if( VeqDepends[ index ] != 0 ) continue;
#ifdef EQ
		pos = eqV[ index ] + 1;
		neg = eqV[ -index ] + 1;
#else
		pos = Vact[ index ] + 1;
		neg = Vact[ -index ] + 1;
#endif

		loc = Ic[ VAR(index) ] + 1;
		for(i = loc[ -1 ] - 1; --i; )
#ifdef EQ
		pos += eqV[ -loc[ i ] ];
#else
		pos += Vact[ -loc[ i ] ];
#endif

		loc = Ic[ VAR(-index) ] + 1;
		for(i = loc[ -1 ] - 1; --i; )
#ifdef EQ
			neg += eqV[ -loc[ i  ] ];
#else
			neg += Vact[ -loc[ i  ] ];
#endif
			
		if ( pos > 0x00004FFFF ) pos = 0x00004FFFF;
		if ( neg > 0x00004FFFF ) neg = 0x00004FFFF;

		ACTvalues[ index - 1 ] = pos * neg;
//		ACTvalues[ index - 1 ] = (int) sqrt(pos * neg);

//		if( lookStamps[ index ] == currentLookStamp )
//			ACTvalues[ index - 1 ] = (int) ACTvalues[ index - 1 ] * 0.8;

		//assert( ACTvalues[ index -1 ] > 0 );
	}
}

void percentmar( int aantalSlagen )
{
        int i, j;
	int som;
	double gemiddelde;

	initACT();

//	if( depth < 9 )
//	{

#ifdef WEIGHT
		setWeight();
		calcMARvalues();

#endif
//	}

/*
	  printf("q2\n==\n\n");
	  for( i = 0; i < nrofvars; i++ )
	  {
		printf("%i ", q2[ i ]);

	  }
*/
        lookaheadArrayLength = 0;

        // count unfixed variables and their average mar value
        for( j = 0, som = 0; j < freevars; j++ )
	{
	  	i = independ[ j ] ;
	        som += (int) MARvalues[ i ];
                lookaheadArray[ lookaheadArrayLength++ ] = i;
	}

        if( !lookaheadArrayLength || ( ( lookaheadArrayLength / 2 ) < lookvars ) )
	{
		//qsort( lookaheadArray, lookaheadArrayLength, sizeof( int ), marCompare ); 
		
	//	if( lookaheadArrayLength > lookvars )
	//		lookaheadArrayLength = lookvars;

        //        return;
	}

        gemiddelde = som / lookaheadArrayLength - 1;

        // in-place narrow listarray a number of times
        for( j = 0; j < aantalSlagen; j++ )
        {
                NARROW_LISTARRAY;
                if( ( lookaheadArrayLength / 2 ) < lookvars )
                   break;
                gemiddelde = som / lookaheadArrayLength - 1;
        }

	qsort( lookaheadArray, lookaheadArrayLength, sizeof( int ), marCompare ); 

	if( lookaheadArrayLength > lookvars )
		lookaheadArrayLength = lookvars;

	lookvars = nrofvars * PERCENT / 100;
}

void percentact( int aantalSlagen )
{
        int i, j;
	int som;
	double gemiddelde;

	//initACT();

	lookvars = (nrofvars * PERCENT) / 100;
	if( lookvars < 15 ) lookvars = 15;

        lookaheadArrayLength = 0;

        // count unfixed variables and their average mar value
        for( j = 0, som = 0; j < freevars; j++ )
	{
	     	i = independ[ j ] ;

		if( !VeqDepends[ i + 1 ] )
		{
		   if( ACTvalues[ i ] > 0 )
		   {
	        	som += ACTvalues[ i ];
                	lookaheadArray[ lookaheadArrayLength++ ] = i;
			//printf("%i ", i + 1);
                   }
		}
		//else
		//	printf("skipped %i\n", i + 1);
		
	}
	//printf("\n");

	if( lookaheadArrayLength == 0 )
	{
          for( j = 0, som = 0; j < freevars; j++ )
	  {
	     	i = independ[ j ] ;
        	som += ACTvalues[ i ];
		if( !VeqDepends[ i + 1 ] )
	               	lookaheadArray[ lookaheadArrayLength++ ] = i;

	  }
	  return;
	}

        if( !lookaheadArrayLength || ( ( lookaheadArrayLength / 2 ) < lookvars ) )
	{
		qsort( lookaheadArray, lookaheadArrayLength, sizeof( int ), actCompare ); 
		
		if( lookaheadArrayLength > lookvars )
			lookaheadArrayLength = lookvars;

                return;
	}

        gemiddelde = som / lookaheadArrayLength - 1;

        // in-place narrow listarray a number of times
        for( j = 0; j < aantalSlagen; j++ )
        {
                NARROW_LISTARRAY2;
                if( ( lookaheadArrayLength / 2 ) < lookvars )
                   break;
                gemiddelde = som / lookaheadArrayLength - 1;
        }

	qsort( lookaheadArray, lookaheadArrayLength, sizeof( int ), actCompare ); 

	if( lookaheadArrayLength > lookvars )
		lookaheadArrayLength = lookvars;
}

int marCompare(const void *ptrA, const void *ptrB)
{
	return ( MARvalues[ *(int *)ptrA ] - MARvalues[ *(int *)ptrB ] ) > 0 ? -1 : 1;
}

int actCompare(const void *ptrA, const void *ptrB)
{
	return ( ACTvalues[ *(int *)ptrA ] - ACTvalues[ *(int *)ptrB ] ) > 0 ? -1 : 1;
}

void calcMARvalues() {
  int i, j, index;
  double subValue, current;
  double value;

  DEBUG( MAR, "CALC\n" );

  for( i = 0;  i < nrofvars; i++ ) {
    value = w2 * Q2[ i ][ 0 ] + w3 * Q3[ i ][ 0 ];

    if( ( timeAssignments[ i + 1 ] < VARMAX ) && ( value > 0 ) )
    {
      for( j = 1; j <= Qlist[ i ][ 0 ]; j++ ) 
      {
        index = Qlist[ i ][ j ];
	if( timeAssignments[ index + 1 ] < VARMAX )   
        {  
           current = w2 * Q2[ index ][ 0 ] + w3 * Q3[ index ][ 0 ];
           if (current > 0)
           {
             if (i < index)  subValue = w2 * Q2[ i ][ j ] + 
                                       w3 * Q3[ i ][ j ]; 
             else 	     subValue = w2 * Q2[ index ][ Qinverse[ i ][ j ]] + 
                                       w3 * Q3[ index ][ Qinverse[ i ][ j ]] ; 
             value -= (double)( subValue * subValue ) / current ;
           }
        }
      }
    }
    else value = -1;
    MARvalues[ i ] = value;
  }	
}

void updateACT(int fixed_nr)
{
	int tmp;

	tmp = independ[ freevars -1 ];

   	independ[ independLUT[ fixed_nr ] ] = tmp;
   	independ[ freevars -1 ] = fixed_nr;

   	independLUT[ tmp ] = independLUT[ fixed_nr ];
   	independLUT[ fixed_nr ] = freevars - 1;

   	freevars--;

}

void restoreACT(int fixed_nr)
{
        freevars++;
}

void updateQ(int fixed_nr)
{
   int i, index, Q2temp, Q3temp, tmp;
   double divider;

//    printf("independLUT[ %i ] = %i ", fixed_nr, independLUT[ fixed_nr ] );
//   exit(0);


//   if( independLUT[ fixed_nr ] != -1 )
//   {




	tmp = independ[ freevars -1];

   	independ[ independLUT[ fixed_nr ] ] = tmp;
   	independ[ freevars -1] = fixed_nr;

   	independLUT[ tmp ] = independLUT[ fixed_nr ];
   	independLUT[ fixed_nr ] = freevars - 1;

   	freevars--;
//    }

//   printf("UPDATE %i, %i \n", fixed_nr, independ[ freevars +1] );

   
//   DEBUG( MAR, "UPD VAR %i\n", fixed_nr );

   if ( (Q3[ fixed_nr ][ 0 ] + Q2[ fixed_nr ][ 0 ]) > 0 )
   {
     divider = sqrt ( w3 * Q3[ fixed_nr ][ 0 ] + w2 * Q2[ fixed_nr ][ 0 ] );
     for( i = 1 ;  i <=  Qlist[ fixed_nr ][ 0 ]; i++ ) 
     {
        index = Qlist[ fixed_nr ][ i ];
	if ( timeAssignments[ index + 1 ] < VARMAX )   
	{
            if (index < fixed_nr)
            {
               Q2temp = Q2[ fixed_nr ][ i ]; 
               Q3temp = Q3[ fixed_nr ][ i ]; 
            }
            else
            {
               Q2temp = Q2[ index ][ Qinverse[ fixed_nr ][ i ]]; 
               Q3temp = Q3[ index ][ Qinverse[ fixed_nr ][ i ]]; 
            }  
            Q2[ index ][ 0 ]   -= Q2temp;
            Q3[ index ][ 0 ]   -= Q3temp;
            MARvalues[ index ] -= w2 * (Q2temp - (double) Q2temp / divider) + 
                                  w3 * (Q3temp - (double) Q3temp / divider) ;            
        }            
     }
   }
}	

void restoreQ(int fixed_nr)
{
   int i, index, Q2temp, Q3temp;
   double divider;

//   if( independLUT[ fixed_nr ] != -1 )
	   freevars++;

//   printf("RESTORE %i, %i\n", fixed_nr, independ[ freevars ] );

//   DEBUG( MAR, "RES VAR %i\n", fixed_nr );

   if ( (Q3[ fixed_nr ][ 0 ] + Q2[ fixed_nr ][ 0 ]) > 0 )
   {
     divider = sqrt ( w3 * Q3[ fixed_nr ][ 0 ] + w2 * Q2[ fixed_nr ][ 0 ] );
     for( i = 1;  i <=  Qlist[ fixed_nr ][ 0 ]; i++ ) 
     {
        index = Qlist[ fixed_nr ][ i ];
        if( timeAssignments[ index + 1 ] < VARMAX ) 
        {   
            if (index < fixed_nr)
            {
               Q2temp = Q2[ fixed_nr ][ i ]; 
               Q3temp = Q3[ fixed_nr ][ i ]; 
            }
            else
            {
               Q2temp = Q2[ index ][ Qinverse[ fixed_nr ][ i ]]; 
               Q3temp = Q3[ index ][ Qinverse[ fixed_nr ][ i ]]; 
            }  
            Q2[ index ][ 0 ]   += Q2temp;
            Q3[ index ][ 0 ]   += Q3temp;
            MARvalues[ index ] += w2  * (Q2temp - (double) Q2temp / divider) + 
                                  w3 * (Q3temp - (double) Q3temp / divider) ;            
        }
     }
   }
}	

int getMaxCeqVar()
{
	int i, j , minceq, maxValue, maxVar, current, value;

	minceq = 1000;
	maxValue = -1;
	maxVar = 0;
/*
        printf("CEQ\n---\n\n");
        for( i = 0; i < nrofceq; i++ )
        {
                printf("%i ( %i ): %i ", i, CeqDepends[ i ], Ceq[ i ][ 0 ] );
                for( j = 1; j < CeqSizes1[ i ]; j++ )
		    if( timeAssignments[ Ceq[ i ][ j ] ] < VARMAX )
                        printf("* %i ", Ceq[ i ][ j ] );
                printf("= %i\n", CeqValues[ i ] );
        }
*/


	for( i = 0; i < nrofceq; i++ )
	{
		if( CeqSizes1[ i ] < minceq && CeqSizes1[ i ] >= 2 )
			minceq = CeqSizes1[ i ];
		if( minceq == 2 )
			break;
	}

	for( i = 0; i < lookaheadArrayLength; i++ )
	{
		value = 0;
		current = lookaheadArray[ i ] + 1; 
		if( timeAssignments[ current ] < VARMAX )
		{
			for( j = 1; j < Veq[ current ][ 0 ]; j++ )
				if( CeqSizes1[ Veq[ current ][ j ] ] == minceq )
					value++;
			if( value > maxValue )
			{
				maxValue = value;
				maxVar = current;
			}
		}
	}
	return maxVar;
}



void mar_add_lit( int lit1, int lit2, int truth )
{
    int i, index = 0;

    CHANGE_CLAUSE2( -1 );

    MARvalues[ lit1 ] -= w2 - w2Square / (w2 * Q2[ lit2 ][ 0 ] + w3 * Q3[ lit2 ][ 0 ]);
    MARvalues[ lit2 ] -= w2 - w2Square / (w2 * Q2[ lit1 ][ 0 ] + w3 * Q3[ lit1 ][ 0 ]);

    Q2[ lit1 ][ 0 ] -= 1;
    Q2[ lit2 ][ 0 ] -= 1;
}

void mar_remove_lit( int lit1, int lit2, int truth )
{
    int i, index = 0;

    CHANGE_CLAUSE2( 1 );

    Q2[ lit1 ][ 0 ] += 1;
    Q2[ lit2 ][ 0 ] += 1;

    MARvalues[ lit1 ] += w2 - w2Square / (w2 * Q2[ lit2 ][ 0 ] + w3 * Q3[ lit2 ][ 0 ]);
    MARvalues[ lit2 ] += w2 - w2Square / (w2 * Q2[ lit1 ][ 0 ] + w3 * Q3[ lit1 ][ 0 ]);
}

void mar_add_cls( int lit1, int lit2, int truth )
{
    int i, index = 0;

    CHANGE_CLAUSE3( 1 );
}

void mar_remove_cls( int lit1, int lit2, int truth )
{
    int i, index = 0;

    CHANGE_CLAUSE3( -1 );
}

int getQij( int var1, int var2 )
{
   int i;

   for( i = 1; i <= Qlist[ var1 ][ 0 ]; i++ )
     if( Qlist[ var1 ][ i ] == var2 ) return i;

   return 0; 
} 


void setWeight()
{
	int i, j, iter2;
	double a, b, hwk, wk, wk1;
	double q2q2, q2q3, qq, qqq, q3wq2; 

	wk1 = 0;
	iter2 = 0;

	a = 3.0 * S3 / ( freevars + 1.0 );
	b = 2.0 * S2 / ( freevars + 1.0 );

	q2q2 = 0;
	for( i = 0; i < nrofvars; i++ )
		q2q2 += pow( q2[ i ], 2 );

	q2q3 = 0;
	for( i = 0; i < nrofvars; i++ )
		q2q3 += q2[ i ] * q3[ i ];

	
/* TEST TEST */
	if( q2q2 == 0.0 )
	{
		printf( "q2 == 0\n" );
		w2 = 100.0;
		w3 = 1.0;
		return;
	}

	j = 0;
	for( i = 0; i < nrofvars; i++ )
		if( q3[ i ] > 0 ) j = 1;

	if( !j )
	{
		printf( "q3 == 0\n" );
		w2 = 0.0;
		w3 = 1.0;
		return;
	}

/* TEST TEST */
//	printf("a: %f, b: %f, q2q2: %f, q2q3: %f \n", a, b, q2q2, q2q3 );

	do
	{
		iter2++;
		wk = wk1;

		q3wq2 = 0;		
		for( i = 0; i < nrofvars; i++ )
			q3wq2 += pow( q3[ i ] + wk * q2[ i ], 2 );
		q3wq2 = sqrt( q3wq2 );		

		qq = ( q2q3 + wk * q2q2 ) / q3wq2;
		qqq = (q2q2 / q3wq2) - pow( q2q3 + wk * q2q2, 2 ) / pow( q3wq2, 3 );

		hwk = a + q3wq2;

		//printf("q3wq2: %f, qq : %f, qqq : %f, hwk: %f \n", q3wq2, qq, qqq, hwk );

		wk1 = wk - (( qq * hwk + ( freevars + 1.0 ) * qq * b * wk - freevars * b * hwk ) /
		      (( freevars * ( freevars + 1.0 ) * pow( qq * b * wk - b * hwk, 2 ) / (hwk * ( hwk + b * wk )) )
		      + (freevars + 1) * qqq * b * wk + qqq * hwk ));

		//printf("Wk = %f, delta = %f\n", wk1, wk1-wk );
	}
	while( (( wk1 - wk ) > 0.000001) && wk < 1000 );

	wk = wk1;
	printf( "c setWeight():: D: %i, %f, iter2: %i, q2q2: %f, q2q3: %f a:%f b:%f\n", depth, wk1, iter2, q2q2, q2q3, a, b );

	tot += wk1;
	totn += 1.0;

	w2 = wk1;
	w3 = 1.0;
} 

void resetDummyBranch()
{
	int i;

	for( i = nroforigvars; i < nrofvars; i++ )
		VeqDepends[ i ] = 1;
}

void initIndepend()
{
    int i, pos, neg;
    int nrofindvars = 0;

    independ     = (int*) malloc( sizeof( int ) * nrofvars );
    independLUT  = (int*) malloc( sizeof( int ) * nrofvars );

    for( i = 0; i < nrofvars; i++ )
    {
        independLUT[ i ] = -1; 

	pos = VAR( i + 1 );
	neg = VAR( -i - 1 );

	if( (Vact[ pos ] + Vact[ neg ] == 0) &&
	    (Ic[ pos ][ 0 ] == Ic_dead[ pos ]) &&
	    (Ic[ neg ][ 0 ] == Ic_dead[ neg ]) &&
	    (VeqDepends[ i + 1 ] == 0 ) && 
	    (Veq[ i + 1 ][ 0 ] == 1 ) )
	{
		continue;
		//printf("Dependand %i\n", i+ 1);
	}
	
	if( (timeAssignments[ i + 1 ] < VARMAX) )
	{
		independ[ nrofindvars  ] = i;
	        independLUT[ i ] = nrofindvars++;
	}
    }
    freevars = nrofindvars;
}


void initMAR( ) {
  int i, j, k, l, current, temp, index;
  int l1,l2,l3,v1,v2,v3;
  int clidx;
      
  /* allocate space for AtA (Q), MAR value array and the array of non-zero's in Q. */
  MARvalues  = (double*) malloc( sizeof( double ) * nrofvars );
  Q2         = (int**) malloc( sizeof( int* ) * nrofvars );
  Q3         = (int**) malloc( sizeof( int* ) * nrofvars );
  Qlocation  = (int*) malloc( sizeof( int ) * nrofvars );
  Qinput     = (int*) malloc( sizeof( int ) * nrofvars );
  Qhits      = (int*) malloc( sizeof( int ) * nrofvars );
  Qlist      = (int**) malloc( sizeof( int* ) * nrofvars );
  Qinverse   = (int**) malloc( sizeof( int* ) * nrofvars );

  /* q2 and q3 for the weight */
  q2 = (int *)  malloc( sizeof( int ) * nrofvars  );
  q3 = (int *)  malloc( sizeof( int ) * nrofvars  );
    
  for (i = 0; i < nrofvars; i ++ )
  {
        Qhits[i] = 0;

        for (j = 0; j < nrofvars; j++)
          Qinput[j] = 0;

        for (j = 1; j <= Vc[ i + nrofvars + 1 ][ 0 ]; j++)
        {
           clidx = Vc[ i + nrofvars + 1 ][ j ];
           for( k = 0; k < 3; k++ )
                   Qinput[ abs( Cv[ clidx ][ k ] ) - 1 ]++;
        }

        for (j = 1; j <= Vc[ nrofvars - i - 1 ][ 0 ]; j++)
        {
           clidx = Vc[ nrofvars - i - 1 ][ j ];
           for( k = 0; k < 3; k++ )
                   Qinput[ abs( Cv[ clidx ][ k ] ) - 1 ]++;
        }

        for (j = 2; j < Ic[ i + nrofvars + 1 ][ 0 ]; j++)
        {
	   Qinput[ i ]++;
	   Qinput[ abs( Ic[ i + nrofvars + 1 ][ j ] ) - 1 ]++;
        }

        for (j = 2; j < Ic[ nrofvars - i - 1 ][ 0 ]; j++)
        {
	   Qinput[ i ]++;
	   Qinput[ abs( Ic[ nrofvars - i -  1 ][ j ] ) - 1 ]++;
        }

        for (j = 0; j < nrofvars; j++)
          if ( Qinput[j] > 0)
            Qhits[ i ]++;
  }
  free( Qinput );


  for (i = 0; i < nrofvars; i++ )
  {
     Q2 [ i ]        = (int*) malloc( sizeof( int ) * ( Qhits[i] ) );
     Q3 [ i ]        = (int*) malloc( sizeof( int ) * ( Qhits[i] ) );
     Qlist    [ i ]  = (int*) malloc( sizeof( int ) * ( Qhits[i] ) );
     Qinverse [ i ]  = (int*) malloc( sizeof( int ) * ( Qhits[i] ) );
     Q2       [ i ][ 0 ] = 0;
     Q3       [ i ][ 0 ] = 0;
     Qlist    [ i ][ 0 ] = Qhits[ i ] - 1;
     Qinverse [ i ][ 0 ] = 0;
     for (j = 1; j <= Qlist[ i ][ 0 ]; j++)  
     { 
       Q2   [ i ][ j ] =  0; 
       Q3   [ i ][ j ] =  0; 
       Qlist[ i ][ j ] = -1;  
     }

     //independLUT[ i ] = -1; 

  //   if( Qhits[ i ] > 0 )
    //if( timeAssignments[ i + 1 ] < VARMAX ) 
    //{
    //	independ[ nrofindvars  ] = i;
    //	independLUT[ i ] = nrofindvars++; 
    //}

  }  
  free( Qhits );

  /* just a HACK */

/*
  for (i = 0; i < nrofvars; i++ )
	if( (Vc[ nrofvars - i - 1 ][ 0 ] + Vc[ nrofvars + i + 1 ][ 0]  )== 8 )
	{
		printf("%i\n", i + 1);
		andchk[ i + 1 ] = 1;
	}
*/

/*
  for (i = 0; i < andDepend; i++ )
  {

	current = andDepends[ i ];
   	temp = independ[ nrofindvars - 1];

   	independ[ independLUT[ current ] ] = temp;

   	independLUT[ temp ] = independLUT[ current ];
   	independLUT[ current ] = -1;

   	nrofindvars--;
  }
*/
/*
  printf("c ");
  for (i = 0; i < nrofindvars; i++ )
  {
	printf("%i ", independ[ i ]);


  }
  printf("\n");
*/
 

/*
  for (i = 0; i < nrofindvars; i++ )
	printf("%i ( %i )\n", independ[ i ], independLUT[ independ[ i ] ]);
*/

  /*Fill the lists*/
  for (i = 0; i < nrofclauses; i++)
  {
    for (j = 0; j < 3; j++)
    {
       current = abs( Cv[ i ][ j ] ) - 1;
       for (k = 0; k < 3; k++)
         if (k != j)
         {
           temp = abs( Cv[ i ][ k ] ) - 1;
           for (l=1; l <= Qlist[ temp ][ 0 ]; l++)
           {
              if (Qlist[ temp ] [ l ] == current ) { break;}
              else if(Qlist[ temp ][ l ] == -1 ) { Qlist[ temp ] [ l ] = current; break;}
           }
         }
    }
  }


  for( i = 0; i <= 2 * nrofvars; i++ )
  {
	current = abs( i - nrofvars ) - 1;
	if( current == -1 ) continue;

	for( j = 2; j < Ic[ i ][ 0 ]; j++ )
	{
		temp = abs( Ic[ i ][ j ] ) - 1;
		for( l = 1; l <= Qlist[ temp ][ 0 ]; l++ )
			if( Qlist[ temp ][ l ] == current ) break;
			else if( Qlist[ temp ][ l ] == -1 )
			{
				Qlist[ temp ][ l ] = current;
				break;
			}
	}
  }


  /*Order the lists*/
  for (i = 0; i < nrofvars; i ++ )
  {
     for (j = 0; j < Qlist[ i ][ 0 ] - 1 ; j++)
       for (k = 1; k < Qlist[ i ][ 0 ] - j; k++) 
         if (Qlist[ i ][ k ] > Qlist[ i ][ k + 1])
         {
             temp = Qlist[ i ][ k ];
             Qlist[ i ][ k ] = Qlist[ i ][ k+1 ];
             Qlist[ i ][ k+1 ] = temp;
         }
  }
 

  /*Initialise Qinverse*/
  for( i = 0; i < nrofvars; i++ ) {
     for( j = 1; j <= Qlist[ i ][ 0 ]; j++ ) {
        Qinverse [ Qlist [i][j] ] [ 0 ] ++;    	
        Qinverse [ Qlist [i][j] ] [Qinverse [ Qlist [i][j] ] [ 0 ] ] = j;
     }
  }
 

  /*Initialise Qlocation*/
  for( i = 0; i < nrofvars; i++ ) 
  {
     if (Qlist[ i ][Qlist[ i ][ 0 ]] < i ) 
         Qlocation[ i ] = Qlist[ i ][ 0 ];
     else
       for( j = 1; j <= Qlist[ i ][ 0 ]; j++ ) 
       {
	if( Qlist[ i ][ j ] > i )
	  { Qlocation[ i ] = j; break; }
       }
  }


  /*Fill Q*/
  for( i = 0; i < nrofclauses; i++ )
  {
      l1 = abs( Cv[ i ][ 0 ] ) - 1;
      l2 = abs( Cv[ i ][ 1 ] ) - 1;
      l3 = abs( Cv[ i ][ 2 ] ) - 1;

      Q3[ l1 ][ 0 ] += 1;
      Q3[ l2 ][ 0 ] += 1;
      Q3[ l3 ][ 0 ] += 1;

      q3[ l1 ] += SGN( Cv[ i ][ 0 ] );
      q3[ l2 ] += SGN( Cv[ i ][ 1 ] );
      q3[ l3 ] += SGN( Cv[ i ][ 2 ] );

      v1 = SGN( Cv[ i ][ 0 ] * Cv[ i ][ 1 ] );
      v2 = SGN( Cv[ i ][ 0 ] * Cv[ i ][ 2 ] );
      v3 = SGN( Cv[ i ][ 1 ] * Cv[ i ][ 2 ] );

      if (l1 < l2)  {   index = getQij(l2, l1);
                        Q3[ l1 ][ Qinverse[ l2 ][ index ]] += v1; Q3[ l2 ][ index ] += 1; }
      else          {   index = getQij(l1, l2);
                        Q3[ l1 ][ index ] += 1; Q3[ l2 ][ Qinverse[ l1 ][ index ]] += v1; }

      if (l1 < l3)  {   index = getQij(l3, l1);
                        Q3[ l1 ][ Qinverse[ l3 ][ index ]] += v2; Q3[ l3 ][ index ] += 1; }
      else          {   index = getQij(l1, l3);
                        Q3[ l1 ][ index ] += 1; Q3[ l3 ][ Qinverse[ l1 ][ index ]] += v2; }

      if (l2 < l3)  {   index = getQij(l3, l2);
                        Q3[ l2 ][ Qinverse[ l3 ][ index ]] += v3; Q3[ l3 ][ index ] += 1; }
      else          {   index = getQij(l2, l3);
                        Q3[ l2 ][ index ] += 1; Q3[ l3 ][ Qinverse[ l2 ][ index ]] += v3; }

 }

  for( i = 0; i <= 2 * nrofvars; i++ )
  {
    /* skip variable 0 which doesn't exist outside the mar code */
    if( i == nrofvars ) continue;

    l1 = abs( i - nrofvars ) - 1;

    for( j = 2; j < Ic[ i ][ 0 ]; j++ )
    {
      l2 = abs( Ic[ i ][ j ] ) - 1;

      q2[ l2 ] += SGN( Ic[ i ][ j ] );

      neg2[ VAR( Ic[ i ][ j ] ) ]++;

      if( l1 < l2 )
      {
        Q2[ l1 ][ 0 ] += 1;
        Q2[ l2 ][ 0 ] += 1;

        v1 = SGN( ( i - nrofvars )  * Ic[ i ][ j ] );
	//Cv[ i ][ 2 ] );

        if (l1 < l2)  {   index = getQij(l2, l1);
                          Q2[ l1 ][ Qinverse[ l2 ][ index ]] += v1; Q2[ l2 ][ index ] += 1; }
        else          {   index = getQij(l1, l2);
                          Q2[ l1 ][ index ] += 1; Q2[ l2 ][ Qinverse[ l1 ][ index ]] += v1; }
      }
    }
  }
/*
  printf("q2\n==\n\n");
  for( i = 0; i < nrofvars; i++ )
  {
	printf("%i ", q2[ i ]);

  }
*/
/*
  qLarge = (int*) malloc( sizeof( int ) * nrofvars );
  qSize = 0;

  printf("\n\nq3\n==\n\n");
  for( i = 0; i < nrofvars; i++ )
  {
	if(abs(q3[ i ]) > 7 )
	{
		printf("%i: %i %i ", i+1, q3[ i ], Vc[VAR( -1*(i + 1) * SGN(q3[ i ]))][ 0 ]);
		qLarge[ qSize++ ] = (i + 1) * SGN( q3[ i ]);
	}
  }
  printf("\n\n");
*/
  #ifdef WEIGHT
     setWeight();
  #endif

//  printf( "c initMAR():: MAR weight set to: %.2f / %.2f\n", w2, w3 );

  w2Square    = ( w2 * w2 );
  calcMARvalues();
  
  printMARvalues();
  
}
     
void destroyMAR() {
  int i;

  //printf( "tot: %f, totn: %f, average weight: %f\n", tot, totn, tot / totn );

  for ( i = 0; i < Qlist[ i ][ 0 ]; i++ ) 
  {
    free( Q2[ i ] );
    free( Q3[ i ] );
    free( Qlist[ i ] );
    free( Qinverse[ i ] );
  }
  free( Q2 );
  free( Q3 );
  free( Qlist );
  free( Qinverse );
  free( Qlocation );
  free( MARvalues );
}

int getIndepend()
{
	return independ[ 0 ] + 1;
}

void printQlist() {
  int i,j;
    
  DEBUG( MAR, "Qlist:\n" );
  for( i = 0; i < nrofvars; i++ ) {
    for( j = 0; j < Qlist[ i ][ 0 ] + 1; j++ ) {
      DEBUG( MAR, "%i ", Qlist[ i ][ j ] );
    }
    DEBUG( MAR, "\n" );
  }
}

void printQinverse() {
  int i,j;
    
  DEBUG( MAR, "Qlist:\n" );
  for( i = 0; i < nrofvars; i++ ) {
    for( j = 0; j < Qinverse[ i ][ 0 ]; j++ ) {
      DEBUG( MAR, "%i ", Qinverse[ i ][ j ] );
    }
    DEBUG( MAR, "\n" );
  }
}

void printMARvalues() {
  int i, c;
  c = 0;
   
  DEBUG( MAR, "MARvalues:  " );
  for( i = 0; i < nrofvars; i++ ) {
     c++;  
     DEBUG( MAR, "%f ", MARvalues[ i ] );
     if(c == 24)
     {
        DEBUG( MAR, "\n            ");
        c =0;
     }
  }
  DEBUG( MAR, "\n" );
}

void printQlocation() {
  int i;
    
  DEBUG( MAR, "Qlocation:  " );
  for( i = 0; i < nrofvars; i++ ) {
      DEBUG( MAR, "%i ", Qlocation[ i ] );
  }
  DEBUG( MAR, "\n" );
}

