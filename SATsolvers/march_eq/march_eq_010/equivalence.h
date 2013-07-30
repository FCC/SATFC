void init_equivalence();
void reduceEquivalence();
void fixDependedEquivalences();
void find_bieq();

int substitude_equivalences( );
void substitude_equivalence( int clause, int index );
void substitude_ceq( int ceqidx, int var, int ceqsubst );


void shorten_equivalence( );

int find_equivalence( int clause );

void subst_tri_to_bieq();
void propagate_bieq();
void replace_bieq( int lit1, int lit2, int ***__Vc, int ***__VcLUT );

int dependantsExists();

void add_sat_equivalence( int ceqidx );
void remove_sat_equivalence( int ceqidx );

int fixEq( int nr, int pos, int value );
void removeEq( int nr, int pos );

void printCeq();
void printNrofEq();
