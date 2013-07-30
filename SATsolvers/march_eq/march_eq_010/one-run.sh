#!/bin/tcsh -f
# PLEASE COPY THIS FILE TO YOUR DIRECTORY AND MODIFY IT
##################################################################
# Script file for launching one execution. 
# This script has been adaptated from satex scripts
# and can change for the competition. Provided AS IS.

# Only one argument: the name of the cnf file 

# The number seed is taken from `/home/public/scripts/get-seed`

onintr interupt
##################################################################
# MODIFY HERE
###################################################################
# PLEASE COPY THIS FILE TO YOUR DIRECTORY AND MODIFY HERE !
#
# PLEASE UNCOMMENT AND MODIFY ONE LINE:
#set path_exec = '/home/login/mysolver FILECNF RANDOMSEED'
#
# if no RANDOMSEED is needed, the launch command is like:
set path_exec = '/home/submit18/march_eq_010/solve FILECNF'
#
# You can also play on the two environment variables to see how
# your solver behave with signals (this values are small (30 seconds!) 
# for testing purpose).
setenv SATTIMEOUT 30
setenv SATRAM 50
#
#
###################################################################


# Now, solvers should be stopped by controllers. Not by xcpusignal.
# However, having a limit on cpu time is a guarantee... If controller
# bugs...
#limit cputime $largecpulimit
#limit datasize $SATRAM M
limit cputime 1000 # To be modified...

set ftmp = ~/tmp-one-run.$$
set ftmperr = ~/tmp-one-run-err.$$
set nommachine = `uname -n`
set whereErrors = "~"
setenv WHERECONTEST /home/public

set idlancement = -1
set idmachine = -1
set idbench = -1
set idprog = -1
set returnValue = 1
set numberseed = `/home/public/scripts/get-seed`
set fichiercnf = $1
#set tempslimite = $SATTIMEOUT
#set memlimite = $SATRAM

#limit cputime $tempslimite
#limit memoryuse $memlimite

set chemin_out = '~'

set Nom = `echo $fichiercnf | sed 's/\//\\\//g'`

set commande = `echo "$path_exec" | sed "s/FILECNF/$Nom/g;s/RANDOMSEED/$numberseed/g"`
set program = $path_exec[1]:t # Pour trouver nbbranches

set datedebut = `date +"%Y-%m-%d %T"`
echo "c~~ LAUNCH ON $nommachine THE $datedebut" >! $ftmp
echo "c~~ SAT2004-TEST-SCRIPT v1.2 INTERNAL MARKUPS: $idlancement $idmachine $idbench $idprog $numberseed" >> $ftmp
echo 'c~~ REAL COMMAND: '$commande >> $ftmp
echo 'c~~' >> $ftmp

    # HERE : The command is REALLY launched
    (/home/public/scripts/time $commande  >> $ftmp) >& $ftmperr
    # !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
set s = $status

# Now, stdout is in $ftmp, stderr should be in /tmp/error.$pidjob
# And watched infos in /tmp/watched.$pidjob
# Let's get the pidjob now (printed by mytime).
#set pidjob = `grep "c~~ timing-id:" $ftmp | tail -1 | awk '{print $3}'`
set datefin = `date +"%Y-%m-%d %T"`
set ltime = `cat $ftmp | grep 'c~~ timing:' | tail -1`

# Write here the errors output, should be in /tmp/errors.$pidjobs
#if (-f /tmp/errors.$pidjob) then
#    cat /tmp/errors.$pidjob | awk '{print "c~~ errors: " $0}' >> $ftmp
#    rm -f /tmp/errors.$pidjob
#endif
    
# An output file can only be valid if the following lines have been added.

echo "c~~" >> $ftmp
echo "c~~ JOB ENDED THE $datefin WITH STATUS $s IN $ltime[3] SECONDS" >> $ftmp
echo "c~~" >> $ftmp

# Now, we add all the watched informations...
#cat /tmp/watched.$pidjob >> $ftmp
#rm -f /tmp/watched.$pidjob
cat $ftmperr | awk '{print "c~~ errors: " $0}' >> $ftmp


# HERE, in the real scripts, the output will be analyzed and stored
# in the database...

echo "YOUR SOLVER OUTPUT"
echo "------------------"
grep -v '^c~~' $ftmp
echo ""
echo "NOW, CHECKING YOUR OUTPUT (automatic-and-limited check):"
echo "-------------------------"
/home/public/scripts/verifier $fichiercnf < $ftmp 

set outputfile = $fichiercnf:r:t.$program.out

mv -f $ftmp $outputfile
rm -f $ftmperr
echo ""
echo "The complete output (time+control) has been saved in $outputfile"

exit 0

interupt:
rm -f  $ftmp $ftmperr
exit 2


erreur:
rm -f $ftmp $ftmperr
exit 1




