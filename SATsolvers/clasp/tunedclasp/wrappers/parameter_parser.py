'''
Created on Aug 9, 2012

@author: manju
'''
import traceback

class ParameterParser(object):
    ''' parse special parameter syntax to executable syntax '''
    
    
    def __init__(self):
        ''' constructor '''
        pass
    
    def parse_parameters(self,params,prefix):
        ''' parse parameters; syntax: -@[Thread]:{[component]:|S|F}*[param] [value]#
            globals parameters at thread-id 0
            Args:
                parameter : parameter list
                prefix: of build parameters, e.g. "--" (can be overwritten by parameter file)
            Returns:
                thread_to_params:    thread-id to list of parameters (0 -> global parameters)
                thread_to_solver:    thread-id to solver binary
        '''     
        thread_to_params = {}
        thread_to_solver = {}
        threadIndex = 0
        while (True):
            localParam = {}
            local_prefix = prefix
            value = ""
            option = ""
            backupparams = []
            while(params != []):
                head = params.pop(0)
                value = params.pop(0).replace("'","")
                backupparams.append(head)
                backupparams.append(value)
                value_is_int = False
                try:
                    value = int(value)
                    value = str(value)
                    value_is_int = True
                except ValueError:
                    value_is_int = False
                if not value_is_int:
                    try:
                        value = round(float(value),4) # some solvers have problems with E notation, e.g. 6,37E8
                        value = str(value)
                    except ValueError:
                        pass
                if (head.startswith("-@"+str(threadIndex))):
                    option = head[4:]
                    optionSplits = option.split(":")
                    parameterName = optionSplits.pop() # last item in list = parameterName
                    if (parameterName == "solver"): # special filter for solver binary path
                        thread_to_solver[threadIndex] = value
                        continue
                    if (parameterName == "prefix"):
                        local_prefix = value
                        continue
                    if len(optionSplits) > 0:  # composed multi parameter
                        skip = False
                        flag = False
                        component = -1
                        for opt in optionSplits:
                            if (opt == "F"): # Flag
                                flag = True
                                continue
                            if (opt == "S"): # skip
                                skip = True
                                continue
                            try:
                                component = int(opt)
                            except ValueError:
                                pass
                        if (skip == True):
                            continue
                        if (flag == True and value == "no"):
                            continue # won't be passed to solver
                        if (flag == True and value == "yes"):
                            value = ""
                        if (component != -1):
                            if (localParam.get(parameterName) == None):
                                localParam[parameterName] = {}
                            localParam[parameterName][component] = value
                        else:
                            localParam[parameterName] = value
                    else:
                        localParam[option] = value
            params = backupparams
            if (len(localParam) == 0 and threadIndex > 0):
                break
            else:
                thread_to_params[threadIndex] = self.__dic_to_flat_list(localParam,local_prefix)
            threadIndex += 1
        #print(thread_to_params)
        return thread_to_params,thread_to_solver
             
    def __sorted_dict_values(self,adict):
        keys = adict.keys()
        keys.sort()
        return map(adict.get, keys)
    
    def __dic_to_flat_list(self,adict,prefix):
        '''
            convert dictionary to flat parameter list
            Args:
                adict: dictionary : head -> value
                prefix: prefix of parameter head, e.g. "--"
        '''
        allP = []
        for k,v in adict.items():
            if (type(v) is dict):
                vsort = self.__sorted_dict_values(v)
                sortedvalues = ",".join(vsort)
                allP.append(prefix+k+"="+sortedvalues ) 
            else:
                if (type(v) is list):
                    allP.extend(v)
                else:
                    if (v == ""):   # flag behandlung
                        allP.append(prefix+k)
                    else:
                        allP.append(prefix+k+"="+v)
        return allP

