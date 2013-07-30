#include <stdio.h>
#include <stdlib.h>
#include <malloc.h>

#include "memory.h"
#include "common.h"

void allocateVc( int ***_Vc, int ***_VcLUT )
{
        int i, j, varnr, *__VcTemp;
        int **__Vc, **__VcLUT;

        /*
                Create temporary Vc and VcLUT.
        */
        __Vc = (int**) malloc( sizeof( int* ) * ( 2 * nrofvars + 1 ) );
        __VcLUT = (int**) malloc( sizeof( int* ) * ( 2 * nrofvars + 1 ) );

        __VcTemp = (int*) malloc( sizeof( int ) * ( 2 * nrofvars + 1 ) );
        for( i = 0; i < ( 2 * nrofvars + 1 ); i++ )     __VcTemp[ i ] = 1;

        for( i = 0; i < nrofclauses; i++ )
                for( j = 0; j < Clength[ i ]; j++ )
                        __VcTemp[ Cv[ i ][ j ] + nrofvars ]++;

        /*
                Allocate space.
        */
        for( i = 0; i < 2 * nrofvars + 1; i++ )
        {
                __Vc[ i ] = (int*) malloc( sizeof( int ) * __VcTemp[ i ] );
                __Vc[ i ][ 0 ] = __VcTemp[ i ] - 1;

                __VcLUT[ i ] = (int*) malloc( sizeof( int ) * __VcTemp[ i ] );
                __VcLUT[ i ][ 0 ] = __VcTemp[ i ] - 1;

                __VcTemp[ i ] = 1;
        }

        for( i = 0; i < nrofclauses; i++ )
                for( j = 0; j < Clength[ i ]; j++ )
                {
                        varnr = Cv[ i ][ j ] + nrofvars;
                        __Vc[ varnr ][ __VcTemp[ varnr ] ] = i;
                        __VcLUT[ varnr ][ __VcTemp[ varnr ] ] = j;
                        __VcTemp[ varnr ]++;
                }

        free( __VcTemp );

        *_Vc = __Vc;
        *_VcLUT = __VcLUT;
}
