#include "common.h"


struct assignment {
    struct assignment *parent;
    int parent_index;

    struct assignment **incoming;
    int incoming_size;

    int nr_incoming;

    int tree_stamp;
    int tree_depth;
    int tree_size;

    int pos;

    int varnr, truth;
    int bread_crumb;
    struct assignment *complement;
};

struct assignment *assignment_array;
struct assignment **assignment_ptrs;
int node_stamp, tree_stamp, changed;

int treebased_lookahead();
void create_tree_rec( struct assignment *assignment, int depth );
void printtree_rec( struct assignment *assignment, int depth );
void printlist_rec( struct assignment *assignment );
int looklist_rec( struct assignment *assignment );
void create_incoming( struct assignment *assignment );
int checkna_rec( struct assignment *assignment );

int nrgiven, treeindex;
int minpos;
int lastCTS;
int *NAtree;
int NAtreeSize;

int complement_value;
int impgiven;

#define ADD_INCOMING( __parent, __incoming ) \
{ \
	if( __parent->nr_incoming == __parent->incoming_size ) \
	{\
	    __parent->incoming_size *= 2; \
	    __parent->incoming = (struct assignment **)realloc(__parent->incoming, __parent->incoming_size*sizeof(void *));\
\
	}\
	__parent->incoming[__parent->nr_incoming++] = __incoming; \
}

