#include <stdlib.h>

#include "progressBar.h"
#include "common.h"
#include "debug.h"

int pb_count, pb_granularity, pb_currentDepth, pb_branchCounted;

void pb_init( int granularity )
{
  int i;

  if( granularity < 1 || granularity > 6 )
  {
//    DEBUG( "c initProgressBar():: invalid granularity ( 0 < granurality < 7 ).\n" );
    exit( EXIT_CODE_ERROR );
  }

  pb_granularity = granularity - 1;
  pb_currentDepth = 0;
  pb_branchCounted = 0;
  pb_count = 0;

  printf( "c |" );
  for( i = 0; i < ( 1 << granularity ); i++ )
  	printf( "-" );
  printf( "|" );  
  fflush( stdout );
}


void pb_dispose()
{
  pb_update();
  printf( "\nc\n" );
}


void pb_update()
{
  int i;

  printf( "\rc |" );
  for( i = 0; i < pb_count; i++ )
  	printf( "*" );
  fflush( stdout );
}


void pb_descend() {
  pb_branchCounted = 0;
  pb_currentDepth++;
}


void pb_climb() {
  pb_currentDepth--;

  if( ( pb_currentDepth == pb_granularity ) || ( pb_currentDepth < pb_granularity && !pb_branchCounted ) )
  {
    pb_branchCounted = 1;
    pb_count += ( 1 << ( pb_granularity - pb_currentDepth ) );
    pb_update();
  } 
}

