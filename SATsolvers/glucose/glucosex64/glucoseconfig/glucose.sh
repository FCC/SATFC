#!/bin/bash
echo "c arguments: $@"
if [ "x$1" = "x" ]; then
  echo "USAGE: glucose.sh [OPTIONS] <input CNF>"
  exit 1
fi


# to set in evaluation environment
#mypath=.

# To set in a normal envirnement
mypath=.
TMPDIR=/tmp
GLUCOSE_OPTS=""
PREPRO_OPTS=""
TMP=$TMPDIR/glucose_$$ #set this to the location of temporary files
SE=$mypath/SatELite_release           #set this to the executable of SatELite
RS=$mypath/glucose_static              #set this to the executable of RSat
PRE=1;
while  [ $# -ne 0 ]; do
if [ $1 == "--no-preprocess" ]; then 
	PRE=0
elif [[ $1 == -pre* ]]; then
	PREPRO_OPTS="${PREPRO_OPTS} ${1:4} "
elif [[ $1 == -glu* ]]; then
	GLUCOSE_OPTS="${GLUCOSE_OPTS} ${1:4} "
else
	INPUT=$1;
	break;
fi;
shift 
done

echo "c Input File: ${INPUT}"

if [ $PRE -ne 0 ]; then 
echo "c SatElite Options: ${PREPRO_OPTS}"
echo "c Glucose Options: ${GLUCOSE_OPTS}"
echo "c"
echo "c Starting SatElite Preprocessing"
echo "c"
$SE $PREPRO_OPTS $INPUT $TMP.cnf $TMP.vmap $TMP.elim
X=$?
else
echo "c SatElite Disabled"
echo "c Glucose Options: ${GLUCOSE_OPTS}"
X=11;
fi
echo "c"
echo "c Starting glucose"
echo "c"
if [ $X == 0 ]; then
  #SatElite terminated correctly
    $RS $GLUCOSE_OPTS $TMP.cnf $TMP.result "$@" 
    #more $TMP.result
  X=$?
  if [ $X == 20 ]; then
    echo "s UNSATISFIABLE"
    rm -f $TMP.cnf $TMP.vmap $TMP.elim $TMP.result
    exit 20
    #Don't call SatElite for model extension.
  elif [ $X != 10 ]; then
    #timeout/unknown, nothing to do, just clean up and exit.
    rm -f $TMP.cnf $TMP.vmap $TMP.elim $TMP.result
    exit $X
  fi 
  #SATISFIABLE, call SatElite for model extension
  $SE $PREPRO_OPTS +ext $INPUT $TMP.result $TMP.vmap $TMP.elim  "$@"
  X=$?
elif [ $X == 11 ]; then
  #SatElite died or skipped, glucose must take care of the rest
    $RS $GLUCOSE_OPTS $INPUT #but we must force glucose to print out result here!!!
  X=$?
elif [ $X == 12 ]; then
  #SatElite prints out usage message
  #There is nothing to do here.
  X=0
fi

rm -f $TMP.cnf $TMP.vmap $TMP.elim $TMP.result
exit $X
