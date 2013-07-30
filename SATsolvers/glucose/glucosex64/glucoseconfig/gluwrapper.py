#!/usr/bin/env python
'''
Created on 2012-06-06

@author: sam
'''
import signal
import subprocess
import subprocess
import sys
import time
import math


killed=False

logfile=None
logging=False

#Super simple logging system for debugging the wrapper script itself
def log(message):
    global logfile;
    if logging:       
        if logfile is None:
            logfile = open("/tmp/gluwrapper_log.txt", "a")
        if logfile is not None:
            logfile.write(message)

def logflush():
    global logfile;
    if logging and logfile is not None:
        logfile.flush()
        
def logclose():
    global logfile;
    if logging and logfile is not None:
        logfile.close()

def handle_alarm(signum, frame):
    # If the alarm is triggered, we're still in p.communicate()
    # call, so use p.kill() to end the process.
    frame.f_locals['self'].kill()
    global killed
    killed=True




log("ACTUAL ORIGINAL ARGS:" + str(sys.argv))
log("\noriginalargs: ")
for i in range(0,len(sys.argv)):
    log(sys.argv[i] + " ")
log("\n");

args=["./glucose.sh"]
seed= int(sys.argv[5]);
for i in range(6,len(sys.argv), 2):
    
    if (i+1>=len(sys.argv)):
        print "Bad number of arguments"
        log("\nBad number of arguments " + str(len(sys.argv)) + "\n");
        logclose()
        sys.exit( 1)
    
    if (sys.argv[i]=="-glu-rnd-init"):
        if  ((int(sys.argv[i+1]))==1):
            args.append("-glu-rnd-init")
    elif (sys.argv[i]=="--no-preprocess"):
        if  ((int(sys.argv[i+1]))==1):
            args.append("--no-preprocess")
    elif sys.argv[i].startswith("-pre"): #Handle SatElite's odd +/- syntax:
        cmd=sys.argv[i][5:]
        if  ((int(sys.argv[i+1]))==1):
            args.append("-pre"+"+"+cmd)
        elif  ((int(sys.argv[i+1]))==0):
            args.append("-pre"+"-"+cmd)
        else:            
            raise Exception( "Bad SatElite argument: " + sys.argv[i] +  " " + sys.argv[i+1])
    else:   
        args.append(sys.argv[i] + "="+sys.argv[i+1])

args.append("-glu-rnd-seed="+str(seed))
args.append(" " + sys.argv[1]);

log("\nParsedArgs:" + str(args))

timeout=float(sys.argv[3]);

log("\nTimeout:" + str(timeout))

allargs=""
for a in args:
    allargs+= " "+ a
    
log("\nCommand:" + allargs + "\n")
logflush()


signal.signal(signal.SIGALRM, handle_alarm)
if(timeout>=0):
    signal.alarm(long(math.ceil(timeout)))

print ("[gluwrapper]: Calling " + ' '.join(args));
print("[gluwrapper]: Timeout: " + str(long(math.ceil(timeout))) + " seconds")

start=time.time();
p = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

try:
    out, err = p.communicate()
except IOError:
    
    # process was killed due to exceeding the alarm
    log("\nExit code: " + str(p.returncode))
    log( "\nResult for ParamILS: TIMEOUT, " + str(timeout) + ", -1,-1, " + str(seed)+"\n" )  
    print "Result for ParamILS: TIMEOUT, " + str(timeout) + ", -1,-1, " + str(seed) 
    logclose()
    sys.exit(0)
finally:
    if(timeout>=0):
        signal.alarm(0)
    
log("\nFinished:" + str(out) + "err: " + str(err))
log("\nExit code: " + str(p.returncode))
elapsed = (time.time() - start)

if p.returncode==10:
    result = "SATISFIABLE"
elif p.returncode==20:
    result="UNSATISFIABLE"
else:
    result="CRASHED"
    
if(killed):
    log( "\nResult for ParamILS: TIMEOUT, " + str(elapsed) + ", -1,-1, " + str(seed)+"\n" )  
    print "Result for ParamILS: TIMEOUT, " + str(elapsed) + ", -1,-1, " + str(seed) 
else:
    log("\nResult for ParamILS: " + result +", " + str(elapsed) + ", -1,-1, " + str(seed) + "\n" )
    print "Result for ParamILS: " + result +", " + str(elapsed) + ", -1,-1, " + str(seed) 
    
logclose()