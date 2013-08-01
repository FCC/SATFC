#!/bin/sh

export MROOT=$PWD/code

rm -rf binary/glueminisat-simp
make -C code/core clean 
make -C code/simp clean 
 
