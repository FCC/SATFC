import sys
from sys import platform as _platform
import os
import time
import platform
import re

#Paths
solver_dir = './'
glucose_dir = solver_dir+'glucose/'
glucosex32_path = glucose_dir +'glucosex32/glucose.sh'
glucosex64_path = glucose_dir +'glucosex64/full_glucose.sh'

lingeling_dir = solver_dir +'lingeling/'
plingelingx32_path = lingeling_dir +'lingelingx32/plingeling'
plingelingx64_path = lingeling_dir +'lingelingx64/plingeling'

picosat_dir = solver_dir + 'picosat/'
picosatx64_path = picosat_dir + 'picosatx64/picosat'
picosatx32_path = picosat_dir + 'picosatx32/picosat'
picosatx64osx_path = picosat_dir + 'picosatx64osx/picosat'

clasp_dir = solver_dir + 'clasp/'
claspx64_path = clasp_dir + 'claspx64/build/release/bin/clasp'
claspx32_path = clasp_dir + 'claspx32/build/release/bin/clasp'
claspx64osx_path = clasp_dir + 'claspx64osx/build/release/bin/clasp'
tunedclasp_path = clasp_dir + 'tunedclasp/claspCMarch13'


runsolver_dir = solver_dir + 'runsolver/'
runsolverx64_path = runsolver_dir+'runsolverx64/runsolver'
runsolverx32_path = runsolver_dir+'runsolverx32/runsolver'
runsolverx64osx_path = runsolver_dir + 'runsolverx64osx/runsolver'

#Execution

#Process input
instance_name = sys.argv[1]
instance_specific_information = sys.argv[2]
cutoff_time = sys.argv[3]
cutoff_length = sys.argv[4]
seed = sys.argv[5]

solvername = sys.argv[7].replace(' ','').replace("'",'')

(bits,linkage) = platform.architecture()

if '32' in bits:
    runsolver_path = runsolverx32_path
elif '64' in bits:
    if _platform == "linux" or _platform == "linux2":
    	runsolver_path = runsolverx64_path
    elif _platform == "darwin":
    	runsolver_path = runsolverx64osx_path
else:
    print 'UNRECOGNIZED ARCHITECTURE IN SETTING SOLVER PATH!'

if solvername == 'glucose':
    if '32' in bits:
        solver_path = glucosex32_path
    elif '64' in bits:
        solver_path = glucosex64_path
    else:
        print 'UNRECOGNIZED ARCHITECTURE IN SETTING SOLVER PATH!'
elif solvername == 'clasp':
    if '32' in bits:
        solver_path = claspx32_path
    elif '64' in bits:
        if _platform == "linux" or _platform == "linux2":
            solver_path = claspx64_path
        elif _platform == "darwin":
            solver_path = claspx64osx_path
    else:
        print 'UNRECOGNIZED ARCHITECTURE IN SETTING SOLVER PATH!'
elif solvername == 'tunedclasp':
    solver_path = tunedclasp_path
elif solvername == 'plingeling':
    if '32' in bits:
        solver_path = plingelingx32_path
    elif '64' in bits:
        solver_path = plingelingx64_path
    else:
        print 'UNRECOGNIZED ARCHITECTURE IN SETTING SOLVER PATH!'
elif solvername == 'picosat':
    if '32' in bits:
        solver_path = picosatx32_path
    elif '64' in bits:
        if _platform == "linux" or _platform == "linux2":
            solver_path = picosatx64_path
        elif _platform == "darwin":
            solver_path = picosatx64osx_path
    else:
        print 'UNRECOGNIZED ARCHITECTURE IN SETTING SOLVER PATH!'
else:
    print 'ERROR, invalid solver name ',solvername



#Run solver
mem_limit = str(1000)
callstring = runsolver_path+' -M '+mem_limit+' -C '+cutoff_time+' '+solver_path+' '+instance_name    
(a,b,c) = os.popen3(callstring)

clock = time.time()
#Get output        
std_out = ' '.join(b.readlines())
std_err = ' '.join(c.readlines())    

#Analyze output
output_solved = ''
output_runtime = ''
output_runlength = '-1'
output_quality = '-1'
output_seed = seed

TIMEOUTre = re.compile('Maximum CPU time exceeded')
SATre = re.compile('\\bSATISFIABLE\\b')
UNSATre = re.compile('\\bUNSATISFIABLE\\b')

TIMEre = re.compile('CPU time \(s\)\: \d+\.*\d*')

if re.search(TIMEOUTre,std_out):
    output_solved = 'TIMEOUT'
    output_runtime = cutoff_time
    
elif re.search(UNSATre,std_out):
    output_solved = 'UNSAT'
    output_runtime = str(float(TIMEre.findall(std_out)[0].split(':')[1].replace(' ','')))
elif re.search(SATre,std_out):
    output_solved = 'SAT'
    TIMEre.findall(std_out)
    output_runtime = str(float(TIMEre.findall(std_out)[0].split(':')[1].replace(' ','')))
else:
    output_solved = 'CRASHED'
    print std_out
    print std_err
    output_runtime = str(time.time()-clock)

print 'Result for ParamILS: '+output_solved+', '+output_runtime+', '+output_runlength+', '+output_quality+', '+output_seed+'\n'+std_out

