/* tree-based lookahead */

#include <stdio.h>
#include <malloc.h>
#include "flads.h"
#include "lookahead.h"

int treebased_lookahead()
{
    int i, j;

    nrgiven = 0; 
    treeindex = 0;
    //treeindex = 2 * lookaheadArrayLength;
    NAtreeSize = 0;

    node_stamp = tree_stamp;
//    printf( "nodestamp = %i\n", node_stamp );

    for( i = 0; i < lookaheadArrayLength; i++ )
	for( j = 0; j < 2; j++ )
	{
	    int varnr = FROM_MAR(lookaheadArray[i])*((2*j) - 1);
    	    //assignment_ptrs[2*i+j] = &assignment_array[2*FROM_MAR(lookaheadArray[i])+j];
    	    assignment_ptrs[2*i+j] = &assignment_array[ varnr ];
	    assignment_ptrs[2*i+j]->pos = 2*i+j;
	}

 //   dead = 0;

    for( i = 0; i < 2*lookaheadArrayLength; i++ )
    {
        struct assignment *assignment = assignment_ptrs[i];
	minpos = i;

	if( assignment->tree_stamp < node_stamp )
	{
	    assignment->parent = 0;
	    create_tree_rec( assignment, 0 );

	    if( minpos < i )
	    {
	        assignment_ptrs[minpos] = assignment_ptrs[i];
		assignment_ptrs[minpos]->pos = minpos;
		assignment_ptrs[i] = 0;
	    }
	}

	++tree_stamp;
    }

    //if( dead )
     //    printf( "dead node\n" );

 //   dead = 0;

    for( i = 0; i < 2*lookaheadArrayLength; i++ )
    {
        struct assignment *assignment = assignment_ptrs[i];

	if( assignment && !assignment->parent )
	{
                //printf( "tree->\n" );
                //printtree_rec( assignment, 0 );

                impgiven = assignment->tree_size;
                complement_value = impgiven + 1;
                checkna_rec( assignment );

                looklist_rec( assignment );
        }

        ++tree_stamp;
/*
		checktree_rec( assignment );
		looklist_rec( assignment ); 
	}
*/
    }

  //  if( dead )
   //     printf( "dead node\n" );

    return 1;
}

void create_tree_rec( struct assignment *parent, int depth )
{
    int i;
    struct assignment *incoming;

    parent->tree_size = 1;
    parent->tree_stamp = tree_stamp;

    parent->nr_incoming = 0;

    //if( parent->complement->bread_crumb ) 
//	dead = 1;

    parent->bread_crumb = 1;

    create_incoming( parent );

    /* recursively visit children */

    for( i = 0; i < parent->nr_incoming; i++ )
    {
        incoming = parent->incoming[i];

        /* occurs already in tree? */

	if( incoming->tree_stamp >= node_stamp )
	{
	    if( (incoming->tree_stamp < tree_stamp) && !incoming->parent ) 
	    {
	        /* root of previously generated tree: cut and paste */

		ADD_INCOMING( parent, incoming );

		parent->tree_size += incoming->tree_size;
		incoming->parent = parent;	

		assignment_ptrs[incoming->pos] = 0;

		/* if incorporated tree position further to left: move there */

		if( incoming->pos < minpos ) 
		    minpos = incoming->pos;
 	    }
	    else
	        /* tree already incorporated: remove incoming */

	        parent->incoming[i--] = parent->incoming[--parent->nr_incoming];

	    continue;
	}

	incoming->parent = parent;
	create_tree_rec( incoming, depth+1 );

	parent->tree_size += incoming->tree_size;
    }
     
    parent->bread_crumb = 0;
}

void printlist_rec( struct assignment *assignment )
{
    int i, current;

    current = assignment->tree_size-1+nrgiven; 

    for( i = 0; i < assignment->nr_incoming; i++ )
        printlist_rec( assignment->incoming[i] );
 
    if( current == nrgiven )
        nrgiven++;
}

int looklist_rec( struct assignment *assignment )
{
    int i, current;

    current = assignment->tree_size - 1 + nrgiven; 

    treeArray[ treeindex   ].literal = assignment->varnr;
    treeArray[ treeindex++ ].gap = 2 * current;

    for( i = 0; i < assignment->nr_incoming; i++ )
        looklist_rec( assignment->incoming[i] );

    if( current == nrgiven )
        nrgiven++;

    return 1;
}


void printtree_rec( struct assignment *assignment, int depth )
{
    int i;

    for( i = -1; i < depth; i++ )
        printf( "    " );
    printf( "%s%d (%i)\n", assignment->truth?"":"-", assignment->varnr, assignment->tree_size );

    for( i = 0; i < assignment->nr_incoming; i++ )
        printtree_rec( assignment->incoming[i], depth+1 );
}

void create_incoming( struct assignment *assignment ) 
{
    int j;
    const int *loc = Ic[ VAR(-assignment->varnr) ];
    
    for( j = 2; j < loc[ 0 ]; j++ )
    //for( j = loc[ 0 ] - 1; j > 1; j-- )
    {
        if( lookStamps[ loc[ j ] ] == currentLookStamp )
	{
	    ADD_INCOMING( assignment, &assignment_array[ -loc[ j ] ] );
	}
    }
}

void checktree_rec( struct assignment *assignment ) 
{
    int i;
    struct assignment *complement;

    assignment->bread_crumb = 1;
    complement = &assignment_array[ -assignment->varnr ];

    if( complement->bread_crumb )
	NAtree[ NAtreeSize++ ] = complement->varnr;

    for( i = 0; i < assignment->nr_incoming; i++ )
        checktree_rec( assignment->incoming[i] );

    assignment->bread_crumb = 0;
}

int checkna_rec( struct assignment *assignment )
{
    int i;
    int implied, implied_incoming;

    struct assignment *complement;

    implied = implied_incoming = 0;

    assignment->bread_crumb = impgiven--;
    assignment->tree_stamp = tree_stamp;

    complement = &assignment_array[ -assignment->varnr ];

    if( complement->tree_stamp == tree_stamp )
    {
        //printf( "complement in tree: %d and %d\n", assignment->varnr, -assignment->varnr );

        if( complement->bread_crumb < complement_value )
                complement_value = complement->bread_crumb;
    }

    for( i = 0; i < assignment->nr_incoming; i++ )
    {
        if( checkna_rec( assignment->incoming[i] ) )
            implied_incoming = 1;

        if( complement_value <= assignment->bread_crumb )
            implied = 1;
    }

    if( implied && !implied_incoming )
        NAtree[ NAtreeSize++ ] = assignment->varnr;
            //printf( "imply %d\n", assignment->varnr );

    return implied;
}

