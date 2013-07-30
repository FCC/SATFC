#include <stdio.h>
#include <stdlib.h>
#include <malloc.h>
#include <math.h>
#include <assert.h>

#include "equivalence.h"
#include "memory.h"
#include "solver.h"
#include "common.h"

int *eq_found;


int dependantsExists() 
{
	int i, j, varnr;

	for( i = 0; i < nrofceq; i++ )
       	   for( j = 0; j < CeqSizes[ i ]; j++ )
           	if( timeAssignments[ Ceq[ i ][ j ] ] < VARMAX )
                {
			varnr = Ceq[ i ][ j ];
			if(VeqDepends[ varnr ] > 0 ) continue;
			//if( CeqDepends[ i ] == varnr) continue;
	
			//unitresolve( varnr );
			timeAssignments[ varnr ] = VARMAX;
			timeValues[ varnr ] = varnr;
			return 1;
                }

	return 0;
}

void init_equivalence()
{
	int i;


        Ceq = (int**) malloc( sizeof( int* ) *  nrofclauses );
        Veq = (int**) malloc( sizeof( int* ) * ( nrofvars + 1 ) );
        VeqLUT = (int**) malloc( sizeof( int* ) * ( nrofvars + 1 ) );
        CeqValues = (int*) malloc( sizeof( int ) * nrofclauses );
        CeqDepends = (int*) malloc( sizeof( int ) * nrofclauses );

        VeqLength = (int*) malloc( sizeof( int ) * ( nrofvars + 1 ) );

        CeqSizes = (int*) malloc( sizeof( int ) * nrofclauses );
        CeqStamps = (tstamp*) malloc( sizeof( tstamp ) * nrofclauses );

        for( i = 0; i <= nrofvars; i++ )
        {
                Veq[ i ] = (int*) malloc( sizeof( int ) * 3 );
                VeqLUT[ i ] = (int*) malloc( sizeof( int ) * 3 );
                VeqLength[ i ] = 2;
                Veq[ i ][ 0 ] = 1;
                VeqLUT[ i ][ 0 ] = 1;
        }

        for( i = 0; i < nrofclauses; i++ )
                CeqDepends[ i ] = 0;

	eq_found = (int*) malloc( sizeof(int) * 100 );

	for( i = 0; i < 100; i++ )
		eq_found[ i ] = 0;

}

void reduceEquivalence()
{
	int i, j, reduced, iterCounter, fourplus;
	int **_Vc, **_VcLUT;
	
	allocateVc( &_Vc, &_VcLUT );
	_Vc += nrofvars;


do
{
	iterCounter = 0;	

	for( i = 1; i <= nrofvars; i++ )
		if(  ((_Vc[ i ][ 0 ] + _Vc[ -i ][ 0 ]) == 0) && (Veq[ i ][ 0 ] == 2) )
		{
			CeqStamps[ Veq[ i ][ 1 ] ] = CLSMAX;
			iterCounter++;
		}


	for( i = 1; i <= nrofvars; i++ )
		for( j = 1; j < Veq[ i ][ 0 ]; j++ )
			if( CeqStamps[ Veq[ i ][ j ] ] == CLSMAX )
			{
				Veq   [ i ][ j ] = Veq   [ i ][ --Veq   [ i ][ 0 ] ];
				VeqLUT[ i ][ j ] = VeqLUT[ i ][ --VeqLUT[ i ][ 0 ] ];
				j--;
			}
}
while( iterCounter );

	reduced = 0;
	fourplus = 0;
	for( i = 0; i < nrofceq; i++ )
		if( CeqStamps[ i ] < CLSMAX )
		{
			reduced++;
			if( CeqSizes[ i ] > 4 )
				fourplus++;
		}

	printf("c reduceEquivalence():: equivalence clauses reduced from %i to %i (%i)\n", 
			nrofceq, reduced, fourplus );
}

int substitude_equivalences( )
{
        int i, j;
        int index, tmp;
        int *VeqValues, depMin;

        VeqValues = (int*) malloc( sizeof( int ) * ( nrofvars + 1 ) );

        for( i = 0; i <= nrofvars; i++ )
                VeqValues[ i ] = 0;

        for( i = 0; i < nrofclauses; i++ )
        {
            if( (Clength[ i ] == 2) && !VeqDepends[ NR(Cv[ i ][ 0 ]) ] )
                for( j = 0; j < Clength[ i ]; j++ )
                   VeqValues[ NR( Cv[ i ][ j ] ) ] += 11;
            else if(  Clength[ i ] > 2 )
                for( j = 0; j < Clength[ i ]; j++ )
                   VeqValues[ NR( Cv[ i ][ j ] ) ] += 1;
        }
/*
        for( i = 0; i < nrofceq; i++ )
	{
                for( j = 0; j < CeqSizes[ i ]; j++ )
		{
			tmp = Ceq[ i ][ j ];
			if( VeqValues[ tmp ] == 0 )
			//if( (Veq[ tmp ][ 0 ] == 2) && (VeqValues[ tmp ] == 0) )
			{
	                        substitude_equivalence( i, j );
				CeqStamps[ i ] = CLSMAX;
			}
		}
	}

        for( i = 0; i < nrofceq; i++ )
	{
                for( j = 0; j < CeqSizes[ i ]; j++ )
		{
			tmp = Ceq[ i ][ j ];
			if( Veq[ tmp ][ 0 ] == 2 )
	                        substitude_equivalence( i, j );
		}
	}
*/	
        for( i = 0; i < nrofceq; i++ )
        //for( i = nrofceq - 1; i >= 0; i-- )
        {
            if( CeqStamps[ i ] == CLSMAX ) continue;
            if( CeqDepends[ i ] != 0 ) continue;

            if( CeqSizes[ i ] == 0 )
            {
               if( CeqValues[ i ] == -1 )
               {
                  printf("c eqProduct():: invalid equivalence found!\n");
		  return UNSAT;
               }
            }
            else if( CeqSizes[ i ] >= 1 )
            {
                depMin = 100000;
                index = CeqSizes[ i ];
		
                for( j = 0; j < CeqSizes[ i ]; j++ )
                {
				tmp = VeqValues[Ceq[ i ][ j ]];
				if( tmp  < depMin )
                        	{
                                	index = j;
	                                depMin = tmp;
        	                }
                }

                if( index < CeqSizes[ i ] )
		{
                        substitude_equivalence( i, index );
			if( depMin == 0 )			//moet nog goed naar gekeken worden.
				CeqStamps[ i ] = CLSMAX;
		}
            }
        }

        subst_tri_to_bieq();
        propagate_bieq();

	return 1;
}

/*******************************************************

	sustitude a literal form a equivalence-clause
	in all other equivalence clauses where it occurs.

*******************************************************/

void substitude_equivalence( int clause, int index )
{
        int i, var;

        var = Ceq[ clause ][ index ];
#ifdef EQ
        VeqDepends[ var ] = ALL_VARS;
#endif
        CeqDepends[ clause ] = var;

        for( i = Veq[ var ][ 0 ] - 1; i > 0; i-- )
                if( Veq[ var ][ i ] != clause )
                {
                        int ceqidx = Veq[ var ][ i ];

			if( CeqStamps[ ceqidx ] != CLSMAX ) 
				substitude_ceq( ceqidx, var, clause );
             	}
}

void substitude_ceq( int ceqidx, int var, int ceqsubst )
{
	int k, l, m, last;

        Ceq[ ceqidx ] = (int*) realloc( Ceq[ ceqidx ], sizeof( int ) * (CeqSizes[ ceqidx ] + CeqSizes[ ceqsubst ]) );
        CeqValues[ ceqidx ] *= CeqValues[ ceqsubst ];

        for( k = CeqSizes[ ceqsubst ] - 1; k >= 0; k-- )
        {
        	int current = Ceq[ ceqsubst ][ k ];

                for( l = CeqSizes[ ceqidx ] - 1; l >= 0; l-- ) 	
		   if( Ceq[ ceqidx ][ l ] == current) 
                   {    // place the last element on the place of the current
                        last = Ceq[ ceqidx ][ --CeqSizes[ ceqidx ] ];
                        Ceq[ ceqidx ][ l ] = last;

                        for( m = 1; Veq[ last ][ m ] != ceqidx; m++ ) {}
                           VeqLUT[ last ][ m ] = l;

                        for( m = 1; Veq[ current ][ m ] != ceqidx; m++ ) {} 
                           Veq   [ current ][ m ] = Veq   [ current ][ --Veq   [ current ][ 0 ] ];
                           VeqLUT[ current ][ m ] = VeqLUT[ current ][ --VeqLUT[ current ][ 0 ] ];

			goto varsubst;
		    }

	        CHECK_VEQ_BOUND( current );

        	Ceq[ ceqidx ][ CeqSizes[ ceqidx ] ] = current;
	        Veq   [ current ][ Veq   [ current ][ 0 ]++ ] = ceqidx;
		VeqLUT[ current ][ VeqLUT[ current ][ 0 ]++ ] = CeqSizes[ ceqidx ]++;

		varsubst:
       	}
}


/***********************************************************

	find_equivalence returns 0 if no equivalence clause 
	is found, otherwise it will return the domain

*************************************************************/

int find_equivalence( int clause )
{
	int i, j;
	
	int size = Clength[ clause ];
	int domain = pow( 2, size - 1);
	int lit[ size ];

	int sign, tmp;

	if( (clause + domain) > nrofclauses ) return 0;
	
	for( i = 1; i < domain; i++ )	if( Clength[ clause + i ] != size ) return 0;
	for( i = 0; i < size;   i++ )	if( NR(Cv[ clause ][ i ]) != NR(Cv[ clause + domain - 1 ][ i ]) ) return 0;

	sign = 1;
	for( i = 0; i < size; i++ )	sign *= SGN( Cv[ clause ][ i ] );

	for( i = 1; i < domain; i++ )
	{
		tmp = 1;
		for( j = 0; j < size; j++ )	tmp *= SGN( Cv[ clause + i ][ j ]);
		if( tmp != sign )	return 0;
	}

//	printf("found a %i-equivalence clause starting at %i\n", size, clause );  

        Ceq[ nrofceq ] = (int*) malloc( sizeof( int ) * size );
        CeqSizes[ nrofceq ] = size;
        CeqValues[ nrofceq ] = pow( -1, size + 1 );

        for( i = 0; i < size; i++ )
        {
        	lit[ i ] = abs( Cv[ clause ][ i ] );
                Ceq[ nrofceq ][ i ] = lit[ i ];
                CeqValues[ nrofceq ] *= SGN( Cv[ clause ][ i ] );

                CHECK_VEQ_BOUND( lit[ i ] );
                Veq[ lit[ i ] ][ Veq[ lit[ i ] ][ 0 ]++ ] = nrofceq;
                VeqLUT[ lit[ i ] ][ VeqLUT[ lit[ i ] ][ 0 ]++ ] = i;
        }

#ifdef EQ
	for( i = 0; i < domain; i ++ )	Clength[ clause + i ] = 0;
#endif

	eq_found[ size ]++;
	nrofceq++;
	
	return domain - 1;
}

void shorten_equivalence( )
{
	int i, j, k, l,  nf1, nf2, doublenf, iterCounter;
	int current, cmp1, cmp2, dep, ceqidx, counter;

#ifdef LOCALSUBST
	int var;
	int _ceq_stamp[ nrofceq ];
#endif

	int sum, members;

	sum = 0; members = 0;
	
        for( i = 0; i < nrofceq; i++ )
		if( CeqStamps[ i ] != CLSMAX )
		{
			sum += CeqSizes[ i ];
			members++;
		}

	printf("c shorten_equivalence(): average equivalence length reduced from %f ", sum / (double) members);

    	do
	{
	   iterCounter = 0;

           for( i = 0; i < nrofceq; i++ )
	   //for( i = nrofceq - 1; i >= 0; i-- )
           {
              if( (CeqStamps[ i ] == CLSMAX ) ||
                    (CeqDepends[ i ] == 0) )
		 	continue;
              if( CeqSizes[ i ] == 3 )
	      {

                for( j = 0; j < 3; j++ )
                        if( VeqDepends[ Ceq[ i ][ j ] ] == ALL_VARS )  //checken als we geen equivalentie clauses gebruiken
                                break;

		if( j == 3 ) continue;

                nf1 = Ceq[ i ][ (j + 1) %3 ];
                nf2 = Ceq[ i ][ (j + 2) %3 ];

                doublenf = 0;

                for( k = 1; k < Veq[ nf1 ][ 0 ]; k++ )
                        for( l = 1; l < Veq[ nf2 ][ 0 ]; l++ )
                                if( Veq[ nf1 ][ k ] == Veq[ nf2 ][ l ] )
                                        doublenf++;

                if( Veq[ nf1 ][ 0 ] <= Veq[ nf2 ][ 0 ] )
                {
                        if( Veq[ nf1 ][ 0 ] < (2*doublenf) )
                        {
                                VeqDepends[ Ceq[ i ][ j ] ] = 0;
                                VeqDepends[ nf1           ] = ALL_VARS;
				CeqDepends[ i 		  ] = nf1;
                                substitude_equivalence( i, (j + 1) %3 );
				iterCounter++;
                        }
                }
                else
                {
                        if( Veq[ nf2 ][ 0 ] < (2*doublenf) )
                        {
                                VeqDepends[ Ceq[ i ][ j ] ] = 0;
                                VeqDepends[ nf2           ] = ALL_VARS;
				CeqDepends[ i 		  ] = nf2;
                                substitude_equivalence( i, (j + 2) %3 );
				iterCounter++;
                        }
                }
	     }
	     else if(CeqSizes[ i ] == 4)
	     {
	  	for( dep = 0; Ceq[ i ][ dep ] != CeqDepends[ i ]; dep++ ) {}

	     	for( j = 0; j < 4; j++ )
	     	{
			if( j == dep )	continue;
		
			cmp1 = (j+1) % 4;	if(cmp1 == dep)	cmp1 = (j+3) % 4;
			cmp2 = (j+2) % 4;	if(cmp2 == dep) cmp2 = (j+3) % 4;

			current = Ceq[ i ][ j ];
			cmp1    = Ceq[ i ][ cmp1 ];
			cmp2    = Ceq[ i ][ cmp2 ];
			counter = 2 - Veq[ current ][ 0 ];		

			for( k = 1; k < Veq[ current ][ 0 ]; k++ )
			{
				ceqidx = Veq[ current ][ k ];
				if( ceqidx == i ) continue;
				for( l = 0; l < CeqSizes[ ceqidx ]; l++ )
					if( (Ceq[ ceqidx ][ l ] == cmp1) || (Ceq[ ceqidx ][ l ] == cmp2) )
						counter++;
			}

			if( counter > 0 )
			{
				VeqDepends[ CeqDepends[ i ] ] = 0;
	                        VeqDepends[ current         ] = ALL_VARS;
	                        CeqDepends[ i               ] = current;

	                        substitude_equivalence( i, j );
				iterCounter++;
				//printf("variable %i must substitude clause %i\n", current, i);
				break;
			}
		     }
 	    	}
           }
	}
	while( iterCounter );

#ifdef LOCALSUBST
	for( i = 0; i < nrofceq; i++ )
		_ceq_stamp[ i ] = 0;
do
{
	iterCounter = 0;

	for( i = 0; i < nrofceq; i++ )
        {
              	if( (CeqStamps[ i ] == CLSMAX ) || (CeqSizes[ i ] == 2 ) )
		 	continue;

		for( k = 0; k < CeqSizes[ i ]; k++ )
		{
			var = Ceq[ i ][ k ];
			for( l = 1; l < Veq[ var ][ 0 ]; l++ )
				_ceq_stamp[ Veq[ var ][ l ] ]++;
		}
			
		//counter = CeqSizes[ i ] - 1;
		counter = CeqSizes[ i ] / 2 + 1;

		for( j = 0; j < nrofceq; j++ )
		{
			if( (_ceq_stamp[ j ] >= counter) &&  (j != i) )
			{
				substitude_ceq( j, 0, i);
				if( CeqDepends[ i ] > 0 )
				{
					VeqDepends[ CeqDepends[ i ] ] = 0;
					CeqDepends[ i ] = 0;
				}
				iterCounter++;
				//printf("clause %i can substitude %i\n", i, j);
			}
			_ceq_stamp[ j ] = 0;
		}
	}
}
while( iterCounter > 0 );
#endif
	sum = 0; members = 0;
	
        for( i = 0; i < nrofceq; i++ )
		if( CeqStamps[ i ] != CLSMAX )
		{
			sum += CeqSizes[ i ];
			members++;
		}

	printf("to %f\n", sum / (double) members);

}

void subst_tri_to_bieq()
{
	int i, j, h, var1, var2, current;

	for( i = 0; i < nrofceq; i++ )
	   if( (CeqSizes[ i ] == 3) && (CeqDepends[ i ]> 0) )
	   {
		for( j = 0; Ceq[ i ][ j ] != CeqDepends[ i ]; j++ );

		var1 = Ceq[ i ][ (j+1) % 3 ];
		var2 = Ceq[ i ][ (j+2) % 3 ];

		for( j = 1; j < Veq[ var1 ][ 0 ]; j++ )
		{
			current = Veq[ var1 ][ j ];
		
			if( current == i )			continue;
			if( CeqSizes[ current ] != 3 )		continue;
			if( CeqDepends[ current ] == 0 ) 	continue;

			//printf("testing %i\n", current );

			for( h = 1; h < Veq[ var2 ][ 0 ]; h++ )
				if( Veq[ var2 ][ h ] == current )
					substitude_ceq( current, 0, i );
		}
	   }
}

void propagate_bieq()
{
	int i;
	int **_Vc, **_VcLUT;
	
	allocateVc( &_Vc, &_VcLUT );

	for( i = 0; i < nrofceq; i++ )
		if( (CeqSizes[ i ] == 2) && (CeqStamps[ i ] < CLSMAX) )
		{
		   if( CeqDepends[ i ] == Ceq[ i ][ 0 ] )
		   {
			//printf("c replacing %i by %i\n", Ceq[ i ][ 0 ], Ceq[ i ][ 1 ] * CeqValues[ i ] );
			replace_bieq( Ceq[ i ][ 0 ], -Ceq[ i ][ 1 ] * CeqValues[ i ], &_Vc, &_VcLUT );
		   }
		   else
		   {
			//printf("c replacing %i by %i\n", Ceq[ i ][ 1 ], Ceq[ i ][ 0 ] * CeqValues[ i ] );
			replace_bieq( Ceq[ i ][ 1 ], -Ceq[ i ][ 0 ] * CeqValues[ i ], &_Vc, &_VcLUT );
		   }

			CeqStamps[ i ] = CLSMAX;
		}

	reduceEquivalence();
}


//replace lit1 by lit2

void replace_bieq( int lit1, int lit2, int ***__Vc, int ***__VcLUT )
{
	int j, sign, VARfrom, VARto, clsidx;
	int **_Vc, **_VcLUT;

	_Vc    = *__Vc + nrofvars;
	_VcLUT = *__VcLUT + nrofvars;

	//printf("found bieq %i %i\n", lit1, lit2);

	VeqDepends[ NR( lit1 ) ] = ALL_VARS;
        sign = SGN( lit1 ) * SGN( lit2 );

        VARfrom = NR( lit1 );
        VARto = sign * -NR( lit2 );

        /* kan in principe nog twee van af... eerst dit maar eens testen */
        _Vc[ VARto ] = (int*) realloc( _Vc[ VARto ],
        	( _Vc[ VARfrom ][ 0 ] + _Vc[ VARto ][ 0 ] + 10 ) * sizeof( int ) );

        _VcLUT[ VARto ] = (int*) realloc( _VcLUT[ VARto ],
        	( _VcLUT[ VARfrom ][ 0 ] + _VcLUT[ VARto ][ 0 ] + 10 ) * sizeof( int ) );

        /* alle literals veranderen */
        for( j = 1; j <= _Vc[ VARfrom ][ 0 ]; j++ )
        {
        	clsidx = _Vc[ VARfrom ][ j ];
                if ( Clength[ clsidx ] != 0 )
                {
                	Cv[ clsidx ][ _VcLUT[ VARfrom ][ j ] ] = VARto;
                        _Vc[ VARto ][ ++_Vc[ VARto ][ 0 ] ] = clsidx;
                        _VcLUT[ VARto ][ ++_VcLUT[ VARto ][ 0 ] ] = _VcLUT[ VARfrom ][ j ];
                        _Vc[ VARfrom ][ j ] = _Vc[ VARfrom ][ _Vc[ VARfrom ][ 0 ]-- ];
                        _VcLUT[ VARfrom ][ j ] = _VcLUT[ VARfrom ][ _VcLUT[ VARfrom ][ 0 ]-- ];
                        j--;
                }
   	}

        VARfrom = -NR( lit1 );
        VARto = sign * NR( lit2 );

        /* kan in principe nog twee van af... eerst dit maar eens testen */
        _Vc[ VARto ] = (int*) realloc( _Vc[ VARto ],
        	( _Vc[ VARfrom ][ 0 ] + _Vc[ VARto ][ 0 ] + 10 ) * sizeof( int ) );

        _VcLUT[ VARto ] = (int*) realloc( _VcLUT[ VARto ],
        	( _VcLUT[ VARfrom ][ 0 ] + _VcLUT[ VARto ][ 0 ] + 10 ) * sizeof( int ) );

        /* alle literals veranderen */
        for( j = 1; j <= _Vc[ VARfrom ][ 0 ]; j++ )
        {
        	clsidx = _Vc[ VARfrom ][ j ];
                if ( Clength[ clsidx ] != 0 )
                {
                	Cv[ clsidx ][ _VcLUT[ VARfrom ][ j ] ] = VARto;
                        _Vc[ VARto ][ ++_Vc[ VARto ][ 0 ] ] = clsidx;
                        _VcLUT[ VARto ][ ++_VcLUT[ VARto ][ 0 ] ] = _VcLUT[ VARfrom ][ j ];
                        _Vc[ VARfrom ][ j ] = _Vc[ VARfrom ][ _Vc[ VARfrom ][ 0 ]-- ];
                        _VcLUT[ VARfrom ][ j ] = _VcLUT[ VARfrom ][ _VcLUT[ VARfrom ][ 0 ]-- ];
                        j--;
               	}
     	}
}

void find_bieq()
{
	int i, var, var1, var2, ceqidx, posidx;
	int *bi_var, *bi_cls;

        int **_Vc, **_VcLUT;
        allocateVc( &_Vc, &_VcLUT );

	/*
		Wellicht voor de snelheid een Veq aanmaken met alleen alle 3 equivalenties erin
	*/

	bi_var = (int*) malloc( sizeof(int) * nrofvars + 1 );
	bi_cls = (int*) malloc( sizeof(int) * nrofvars + 1 );

	for( var = 1; var <= nrofvars; var++ )
	{
	   if( Veq[ var ][ 0 ] == 1 ) continue;

	   for( i = 1; i <= nrofvars; i++ )
	   {
		bi_var[ i ] = 0;
		bi_cls[ i ] = 0;
	   }

	   for( i = 1; i < Veq[ var ][ 0 ]; i++ )
	   {
		ceqidx = Veq   [ var ][ i ];

		if( CeqStamps[ ceqidx ] == CLSMAX ) continue;
		if( CeqSizes[ ceqidx ] != 3 ) continue;

		posidx = VeqLUT[ var ][ i ];
		var1 = Ceq[ ceqidx ][ (posidx + 1) %3 ];
		var2 = Ceq[ ceqidx ][ (posidx + 2) %3 ];
		if( bi_var[ var1 ] == 0 )
		{
			bi_var[ var1 ] = var2 * CeqValues[ ceqidx ];
			bi_cls[ var1 ] = ceqidx;
		}
		else
		{
			printf("%i == %i by clause %i\n", bi_var[ var1 ], var2 * CeqValues[ ceqidx ], ceqidx);

			substitude_ceq( ceqidx, var, bi_cls[ var1 ] );
			substitude_equivalence( ceqidx, 0 );
			replace_bieq( Ceq[ ceqidx ][ 0 ], -Ceq[ ceqidx ][ 1 ] * CeqValues[ ceqidx ], &_Vc, &_VcLUT );
			continue;

		}
		if( bi_var[ var2 ] == 0 )
		{
			bi_var[ var2 ] = var1 * CeqValues[ ceqidx ];
			bi_cls[ var2 ] = ceqidx;
		}
		else
		{
			printf("%i == %i by clause %i\n", bi_var[ var2 ], var1 * CeqValues[ ceqidx ], ceqidx);

			substitude_ceq( ceqidx, var, bi_cls[ var2 ] );
			substitude_equivalence( ceqidx, 0 );
			replace_bieq( Ceq[ ceqidx ][ 0 ], -Ceq[ ceqidx ][ 1 ] * CeqValues[ ceqidx ], &_Vc, &_VcLUT );
			continue;
		}
	   }
	}
}
/******************************************************

	March found a satisfying assignment.
	Fix all variables that haven's been set yet.

*******************************************************/

void fixDependedEquivalences()
{
	int i, j, varnr, sign, iterCounter;

	do
	{
	   iterCounter = 0;
	   for( i = 0; i < nrofceq; i++ )
	   {
		varnr = 0;
		for( j = 0; j < CeqSizes[ i ]; j++ )
		{
			if( timeAssignments[ Ceq[ i ][ j ] ] < VARMAX )
			{
				if( varnr == 0 )
				{
					varnr =  Ceq[ i ][ j ]; 
				}
				else
				{
					goto fixend;
				}
			}

		}

		if( varnr == 0 ) continue;
		
		iterCounter++;	

		sign = CeqValues[ i ];
		for( j = 0; j < CeqSizes[ i ]; j++ )
			if( Ceq[ i ][ j ] != varnr )
				sign *= SGN( timeValues[ Ceq[ i ][ j ] ] );

		//unitresolve( sign* varnr );
		timeAssignments[ varnr ] = VARMAX;
		timeValues[ varnr ] = sign * varnr;

		fixend:

	   }

	}
	while( iterCounter );
}

void add_sat_equivalence( int ceqidx )
{
	int i, current;

	//printf("adding satisfied equivalence clause %i [ %i ]\n", ceqidx, CeqSizes1[ ceqidx ] ); 
	
	for( i = 0; i < CeqSizes1[ ceqidx ]; i++ )
	{
		current = Ceq[ ceqidx ][ i ];
		//printf("enlarging Veq[ %i ]\n", current);
		//if( Veq[ current ][ Veq[ current ][ 0 ] ] != ceqidx ) break;
		assert( Veq[ current ][ Veq[ current ][ 0 ] ] == ceqidx );
		Veq   [ current ][ 0 ]++;
		VeqLUT[ current ][ 0 ]++;
	}

	CeqStamps[ ceqidx ] = 0;
}

void remove_sat_equivalence( int ceqidx )
{
	int i, j, current;

	sateqCounter++;

	for( i = 0; i < CeqSizes1[ ceqidx ]; i++ )
	{
		current = Ceq[ ceqidx ][ i ];
		for( j = 1; Veq[ current ][ j ] != ceqidx; j++ ) {}

		Veq   [ current ][ 0 ]--;
		VeqLUT[ current ][ 0 ]--;

		if( Veq[ current ][ 0 ] == 1 ) continue;
		//if( Veq[ current ][ 0 ] == j - 1 ) continue;

		Veq   [ current ][ j ] = Veq   [ current ][ Veq   [ current ][ 0 ] ];
		VeqLUT[ current ][ j ] = VeqLUT[ current ][ VeqLUT[ current ][ 0 ] ];

		Veq   [ current ][ Veq   [ current ][ 0 ] ] = ceqidx;
		VeqLUT[ current ][ VeqLUT[ current ][ 0 ] ] = i;
	}

	CeqStamps[ ceqidx ] = CLSMAX;
}

int fixEq( int nr, int pos, int value )
{
		int i;
                int ceqidx = Veq   [ nr ][ pos ];
                int ceqloc = VeqLUT[ nr ][ pos ];
                int nf = Ceq[ ceqidx ][ --CeqSizes1[ ceqidx ] ];

                VeqLUT[ nr ][ pos ] = CeqSizes1[ ceqidx ];

                for( i = 1; Veq[ nf ][ i ] != ceqidx; i++ );
                VeqLUT[ nf ][ i ] = ceqloc;

                Ceq[ ceqidx ][ ceqloc ] = nf;
                Ceq[ ceqidx ][ CeqSizes1[ ceqidx ] ] = nr;

                CeqValues1[ ceqidx ] *= value;

		return i;
}

void removeEq( int nr, int pos )
{
	int tmp, last;

	last = --Veq[ nr ][ 0 ];
	tmp = Veq[ nr ][ pos ];

	//printf("removing ceq %i from %i\n", Veq[ nr ][ last ], nr );

	Veq[ nr ][ pos ] = Veq[ nr ][ last ];
	Veq[ nr ][ last ] = tmp;

	tmp = VeqLUT[ nr ][ pos ];
	VeqLUT[ nr ][ pos ] = VeqLUT[ nr ][ last ];
	VeqLUT[ nr ][ last ] = tmp;
}

void printCeq()
{
	int i, j;

        printf("CEQ\n---\n\n");
        for( i = 0; i < nrofceq; i++ )
        {
		//if( CeqStamps[ i ] == CLSMAX ) continue;
		if( CeqStamps[ i ] == CLSMAX ) printf("SAT ");

                printf("%i ( %i ): %i ", i, CeqDepends[ i ], Ceq[ i ][ 0 ] );
                for( j = 1; j < CeqSizes[ i ]; j++ )
                        printf("* %i ", Ceq[ i ][ j ] );
                printf("= %i\n", CeqValues[ i ] );
        }
}

void printNrofEq()
{
	int i;

	for( i = 0; i < 100; i++ )
		if( eq_found[ i ] > 0 )
			printf("c find_equivalence():: found %i %i-equivalences\n", eq_found[ i ], i );

}
		
