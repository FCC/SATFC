import sys
from sys import platform as _platform
import os
import time
import platform
import re
import tempfile

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

glueminisat_dir = solver_dir + 'glueminisat/'
glueminisatx64_path = glueminisat_dir + 'glueminisatx64/glueminisat'

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
output_suffix = 'output'
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
elif solvername == 'glueminisat':
    if '64' in bits:
        solver_path = glueminisatx64_path
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


(instance_name_head,instance_name_tail) = os.path.split(instance_name)

print 'Solving '+instance_name+'...'

#####################################################################################################
#Make a new CNF where variables are named from 1 to n (where n is the total number of variables.
#Highly undesirable fix - should use a better variable identification scheme inside the (java) solver.

instance_file = open(instance_name)
instance_lines = instance_file.readlines()

variables = []
variables.append(0)

formatted_lines = []

variable_map = dict()
variable_index = 1

t = time.time()

for line in instance_lines:
    line = line.strip()
    line = line[:-1]
    line = line.rstrip()
    if not (line[0]=='p' or line[0]=='c'):
        litterals = line.split(' ')
        #litterals.remove('0')
        mapped_litterals = []
        for litteral in litterals:
            litteral = int(litteral)
            negated = litteral < 0
            litteral = abs(litteral)
            if litteral not in variable_map:
                variable_map[litteral] = variable_index
                variable_index = variable_index + 1 
            mapped_litterals.append((1-2*negated)*variable_map[litteral])
        formatted_line = ' '.join([str(l) for l in mapped_litterals])+' 0\n'
        formatted_lines.append(formatted_line)


formatted_lines.insert(0,'p cnf '+str(len(variable_map))+' '+str(len(formatted_lines))+'\n')

pid = str(os.getpid())
temp_CNF = tempfile.NamedTemporaryFile(dir=instance_name_head,prefix='temp_'+pid+'_',delete=True)
temp_CNF.write(''.join(formatted_lines))
temp_CNF.flush()

instance_name = temp_CNF.name

preprotime = float(time.time()-t)
print 'Time taken to pre-process CNF',str(preprotime)
####################################################################################################

temp_result = tempfile.NamedTemporaryFile(dir=instance_name_head,prefix='temp_'+pid+'_',delete=True)

#Run solver
mem_limit = str(1000)

if solvername == 'glueminisat':
    callstring = runsolver_path+' -M '+mem_limit+' -C '+cutoff_time+' '+solver_path+' '+instance_name+' '+temp_result.name
else:
    callstring = runsolver_path+' -M '+mem_limit+' -C '+cutoff_time+' '+solver_path+' '+instance_name


print callstring
    
(a,b,c) = os.popen3(callstring)

temp_result.flush()

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
    output_runtime = str(float(TIMEre.findall(std_out)[0].split(':')[1].replace(' ','')))
    
    #Each line of the assignment starts with a 'v' and the final line ends with '0'.
    ASSIGNMENTre = re.compile('\\s+v([\\s\\-\\d]+)\n')
    
    if len(ASSIGNMENTre.findall(std_out))==0:
        print 'Solver outputted no assignment!'
        assignment = ''.join(temp_result.readlines()[1:]).replace('\n','').split(' ')
        assignment.remove('0')
    else:
    
        assignment = reduce(lambda x,y : x+y,ASSIGNMENTre.findall(std_out)).lstrip().rstrip().split()
        assignment.remove('0')

####################################################################################################
    #Remap temp variables from assignment to original variables
    inv_variable_map = {v:k for k,v in variable_map.iteritems()}
    assignment = map(lambda l : str((1-2*(int(l)<0))*inv_variable_map[abs(int(l))]),assignment)
####################################################################################################

    assignment = ';'.join(assignment)
    
else:
    output_solved = 'CRASHED'
    output_runtime = str(time.time()-clock)
    print std_out
    print std_err

#output_runtime = str(float(output_runtime)+preprotime)

if output_solved == 'SAT':
    print 'Result for ParamILS: '+output_solved+', '+output_runtime+', '+output_runlength+', '+output_quality+', '+output_seed+', '+assignment+'\n'
else:
    print 'Result for ParamILS: '+output_solved+', '+output_runtime+', '+output_runlength+', '+output_quality+', '+output_seed+'\n'

temp_CNF.close()
temp_result.close()
sys.exit()
