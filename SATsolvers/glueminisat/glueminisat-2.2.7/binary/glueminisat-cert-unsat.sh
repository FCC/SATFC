#!/bin/sh

# Help
if [ $# -eq 0 ]; then
  echo "usage: $0 BENCHNAME"
  exit 1
fi

# Make a temporary file
TMPFILE=`mktemp` || exit 1
trap 'echo "c trapped."; rm -f ${TMPFILE}; exit 1' 1 2 3 15

# Exec glueminisat
BIN_DIR=`dirname $0`
${BIN_DIR}/glueminisat-simp -compe -rup -no-lazy-bin-shrink -lazy-lrn-skip=0 $1 ${TMPFILE}

# Store the exit code
ret=$?

# Output the result to stdout
if  [ "$ret" -eq 20 ]; then
    echo "o proof DRUP"
    cat ${TMPFILE}
fi

rm -f ${TMPFILE}
exit $ret
