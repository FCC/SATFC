'''
Created on Aug 9, 2012

@author: manju
'''

#standard imports
import sys
import os
import json

# own imports
import parameter_parser
from tempfile import NamedTemporaryFile
from subprocess import Popen
import traceback
import platform
from sys import platform as _platform

class ClaspWrapper(object):
    
    def __init__(self):
        ''' Constructor '''
        self._config_file = ""
        self._instance = ""
        self.cutoff = 1.0 # at most 1.0 second
        self._cutoff_length = -1 #unused
        self.seed = -1
        self._params = []
        self._config = {}
        self._thread_to_params = {}
        self._thread_to_solver = {}

        (bits,linkage) = platform.architecture()
        if '32' in bits:
            self._runsolver = "../../../runsolver/runsolverx32/runsolver" 
        elif '64' in bits:
            if _platform == "linux" or _platform == "linux2":
                self._runsolver = "../../../runsolver/runsolverx64/runsolver"
            elif _platform == "darwin":
                self._runsolver = "../../../runsolver/runsolverx64osx/runsolver" 
        else:
            print 'Unrecognized architecture, cannot instantiate runsolver path!'

        self._MAX_MEM = 1000 # MAXIMAL MEMORY for clasp
        self._VERBOSE = True
        self._ZUSE = False
        
        self.status = "CRASHED"
        self.time = self.cutoff

    def parse_args(self,argv):
        ''' parse command line arguments 
            Args:
                argv: command line arguments list
        
        '''
	if(len(argv) < 5):
            print("clasp_wrapper.py is a wrapper for the clasp ASP solver.")
            print("Usage: python2.7 clasp_wrapper.py <config_file, not used in FH version> <instance_relname> <cutoff_time> <cutoff_length> <seed> <params to be passed on>.")
            sys.exit()
        
        if (self._VERBOSE):
            sys.stderr.write("Parsing of Commandline Arguments\n")
        
#        self._config_file = argv[1]
        self._instance = argv[2]
        self.__instance_specific = argv[3] # not used
        self.cutoff = max([float(argv[4]),1.0]) # at most 1.0 second
        self._cutoff_length = float(argv[5]) #unused
        self.seed = float(argv[6])
        self._params = argv[7:]    # remaining parameters
        
#	print "parsed args"

        #self._read_config_file()
        parameter_parser_ = parameter_parser.ParameterParser()
        [self._thread_to_params, self._thread_to_solver] = parameter_parser_.parse_parameters(self._params,"--")
#	print "parsed 2"
        
# FH: took out config file since it seemed to only be used for runsolver location
#    def _read_config_file(self):
#        ''' read config file (json format) 
#            has to provide:
#                "runsolver" : path to runsolver
#        '''
#        if (self._VERBOSE):
#            sys.stderr.write("Read Config File\n")
#        
#        if os.path.isfile(self._config_file):
#            fp = open(self._config_file,"r")
#            self._config = json.load(fp)
#        else:
#            sys.stderr.write("Warning: Config file not found!")

    def start(self):
	'''
            start solver
        '''
#	print "start of self"
        if (self._VERBOSE):
            sys.stderr.write("Start Clasp\n")
        globals_ = self._thread_to_params.pop(0)
        
        self.portfolio_file = None
        if len(self._thread_to_params) > 1: # multithreading
            self.portfolio_file = self.__write_portfolio()
        else:   # single threaded
            globals_.extend(self._thread_to_params[1])
        
        self.new_instance = None
        if self._instance.endswith(".gz"): # check gzip 
            self.new_instance = self._unpack()
            self._instance = self.new_instance.name
        
        clasp = self._thread_to_solver[0]
        self.solver_log = NamedTemporaryFile(prefix="SolverLog", delete=True)
        self.watcher_log = NamedTemporaryFile(prefix="WatcherLog", delete=True)
        # parallel case on Zuse
        if self.portfolio_file and len(self._thread_to_params) < 8 and self._ZUSE :
            cmd = ['taskset', '0xfe']
        else:
            cmd = []
        # non-parallel case on Zuse
        if not self.portfolio_file and self._ZUSE:
            cmd = ['taskset', '0xfe']
            
        cmd.extend([self._runsolver,'-M', str(self._MAX_MEM),
               '-o', self.solver_log.name, '-w', self.watcher_log.name, '-W', str(self.cutoff),clasp,"--outf","2","-q",self._instance])
        cmd.extend(globals_)
        if self.portfolio_file:
            cmd.extend(["-p",self.portfolio_file.name])
            cmd.extend(["-t",str(len(self._thread_to_params))])
        
#	print cmd

	print(" ".join(cmd))
        if (self._VERBOSE):
            sys.stderr.write(" ".join(cmd)+"\n")
        
        #execute
        popen_ = Popen(cmd)
        popen_.communicate()
        
        self.__read_json(self.solver_log)
        
        # close files
        if self.new_instance:
            self.new_instance.close()
        if self.portfolio_file:
            self.portfolio_file.close()
        self.solver_log.close()
        self.watcher_log.close()
        
    def __read_json(self, solver_log):
        '''
            read json output of solvers
            Args:
                solver_log: file pointer of solver output
        '''
        self.time = self.cutoff
        self.status = "CRASHED"
        for l in solver_log:
            print(l.replace("\n",""))
            if (self._VERBOSE):
                sys.stderr.write(l.replace("\n","")+"\n")
        try:
            solver_log.seek(0)
            outJ = json.load(solver_log)
            if( outJ["Result"] != "UNKNOWN" and outJ["Stats"].get("Time") != None):
                self.time  = outJ["Stats"]["Time"]["Total"]
                self.status = outJ["Result"]
                if (self.status == "SATISFIABLE"):
                    self.status = "SAT"
                if (self.status == "OPTIMUM FOUND"):    # TODO: crude fix
                    self.status = "SAT"
                if (self.status == "UNSATISFIABLE"):
                    self.status = "UNSAT"
            else:
                self.status = "TIMEOUT"
        except:
            sys.stderr.write("error reading json output (Interrupted Clasp?)\n")
            self.status = "TIMEOUT"  # TODO: think about this! -> clasp don't produce
            
        if (self.status == "OPTIMUM FOUND"):
            self.status = "SAT"  #map optimum to SAT for smac 
    
        
    def _unpack(self):
        '''
            unpacks instance and returns a new instance file
        '''
        if (self._VERBOSE):
            sys.stderr.write("Unpack gzipped File\n")
        unpacked_file = NamedTemporaryFile(prefix = "Unpacked", delete=True)
        popen_ = Popen(["zcat",self._instance],stdout=unpacked_file)
        popen_.communicate()
        unpacked_file.flush()
        return unpacked_file
        
    def __write_portfolio(self):
        '''
            write portfolio file for clasp
        '''
        if (self._VERBOSE):
            sys.stderr.write("Write Parallel Portfolio File\n")
        portfolio_file = NamedTemporaryFile(prefix = "Portfolio", delete = True)
        for thread_,params in self._thread_to_params.items():
            portfolio_file.write("[%d]: %s\n" % (thread_," ".join(params)))
        portfolio_file.flush()
        return portfolio_file

if __name__ == '__main__':
    
    # zuse specific:
    #sys.stderr.write(os.environ["LD_LIBRARY_PATH"]+"\n")
    #os.putenv("LD_LIBRARY_PATH", "/cvos/shared/apps/python/2.7/lib:/cvos/shared/apps/gcc/4.7.0/lib:/cvos/shared/apps/gcc/4.7.0/lib64:/cvos/shared/apps/intel-tbb/em64t/30_018oss/lib:/cvos/shared/apps/torque/3.0.1/lib/:/cvos/shared/apps/maui/3.3.1/lib")
    #os.putenv("LD_LIBRARY_PATH", "/usr/java/jdk1.6.0_33/jre/lib/amd64/server:/usr/java/jdk1.6.0_33/jre/lib/amd64:/usr/java/jdk1.6.0_33/jre/../lib/amd64:/cvos/shared/apps/python/2.7/lib:/cvos/shared/apps/gcc/4.7.0/lib:/cvos/shared/apps/gcc/4.7.0/lib64:/cvos/shared/apps/intel-tbb/em64t/30_018oss/lib:/cvos/shared/apps/torque/3.0.1/lib/:/cvos/shared/apps/maui/3.3.1/lib")
    try:
        wrapper = ClaspWrapper()
        wrapper.parse_args(sys.argv)
        wrapper.start()   
        #print("Result for SMAC: %s, %f, -1, -1, %d" % (wrapper.status,wrapper.time,wrapper.seed))
        print("Result for ParamILS: %s, %f, -1, -1, %d" % (wrapper.status,wrapper.time,wrapper.seed))
    except:
        wrapper.solver_log.close()
        wrapper.watcher_log.close()
        if wrapper.new_instance:
            wrapper.new_instance.close()
        traceback.print_exc()
        print("Result for ParamILS: %s, %f, -1, -1, %d" % ("CRASHED",wrapper.cutoff,wrapper.seed))


