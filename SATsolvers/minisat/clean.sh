#!/bin/sh

export MROOT=$PWD

rm -rf minisat
make -C core clean 
make -C simp clean 
 
