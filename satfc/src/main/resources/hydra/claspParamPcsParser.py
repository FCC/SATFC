def get_command_line_cmd(config):
    '''
    @contact:    lindauer@informatik.uni-freiburg.de, fh@informatik.uni-freiburg.de
    Returns the command line call string to execute the target algorithm (here: Spear).
        config: a mapping from parameter name to parameter value
        A command call list to execute the target algorithm.
    '''
    cmd = ""
    
    params = []
    for name, value in config.items():
        params.append(name)
        params.append(value)
    thread_to_params, thread_to_solver, params_to_tags = parse_parameters(params)
    
    for t, p_list in thread_to_params.items():
         for p in p_list:
             cmd += " "+p
    
    return cmd

def parse_parameters(params,prefix="--",separator="="):
    ''' parse parameters; syntax: -@[Thread]:{[component]:|S|F}*[param] [value]#
        globals parameters at thread-id 0
        Args:
            parameter : parameter list
            prefix: of build parameters, e.g. "--" (can be overwritten by parameter file)
            separator: of build parameters, e.g. "=" for --t=8 or " " for --t 8
        Returns:
            thread_to_params:    thread-id to list of parameters (0 -> global parameters)
            thread_to_solver:    thread-id to solver binary
            params_to_tags:      parameter name to smac like tags 
    '''     
    params_to_tags = {}
    thread_to_params = {}
    thread_to_solver = {}
    parallel_thread_list = []
    threadIndex = 0
    while params:
        localParam = {}
        local_prefix = prefix
        local_separator = separator
        value = ""
        option = ""
        backupparams = []
        while(params != []):
            head = params.pop(0)
            value = params.pop(0).replace("'","")
            if not "solver" in head:
                value = value.replace("__",",")
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
                if (parameterName == "separator"):
                    if value == "W": # parameter with only whitespaces is not allowed by SMAC
                        value = " "
                    local_separator = value
                    continue
                if len(optionSplits) > 0:  # composed multi parameter
                    skip = False
                    flag = False
                    component = -1
                    if (params_to_tags.get(parameterName) == None):
                        params_to_tags[parameterName] = set()
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
                            params_to_tags[parameterName].add(opt)
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
                    tags = params_to_tags[parameterName]
                    if "ITERATIVE" in tags:
                        parallel_thread_list.append(threadIndex)
                else:
                    localParam[option] = value
            else:
                backupparams.append(head) # keep unhandled
                backupparams.append(value)
        params = backupparams
        if not (len(localParam) == 0 and threadIndex > 0):
            thread_to_params[threadIndex] = __dic_to_flat_list(localParam, local_prefix, local_separator)
        threadIndex += 1
    #print(thread_to_params)
    __join_multi_threading_solver(thread_to_params, thread_to_solver, params_to_tags, parallel_thread_list)
    return thread_to_params, thread_to_solver, params_to_tags

def __join_multi_threading_solver( thread_to_params, thread_to_solver, params_to_tags, parallel_thread_list):
    '''
        join all multi threading solvers and increase their thread argument
        Args:
            thread_to_params:    thread-id to list of parameters (0 -> global parameters)
            thread_to_solver:    thread-id to solver binary
            params_to_tags:      parameter name to smac like tags
            parallel_thread_list: list of threadindexes of parallel solvers 
    '''
    handled_solvers = []
    for parallel_thread in parallel_thread_list:
        try:    
            solver = thread_to_solver[parallel_thread]
        except KeyError:
            continue
        params = thread_to_params[parallel_thread]
        dup_dict = dict(map(lambda (t, x): (t,x==solver), thread_to_solver.items()))
        dups = 0
        index = 1
        # remove all duplicates
        for thread, dup in dup_dict.items():
            if dup == True:
                dups += 1
                thread_to_solver.pop(thread)
                thread_to_params.pop(thread)
                index += 1
                
        # modify configuration, resp. threading parameter
        for par_index in range(0, len(params), 2): # pair (parameter_name, value)
            parameter_name = params[par_index]
            value = params[par_index+1]
            tags_minus = params_to_tags.get(parameter_name[1:]) # parameter has already prefix attached :-(
            tags_minus_minus = params_to_tags.get("--"+parameter_name[2:])
            if (tags_minus and "ITERATIVE" in tags_minus) or (tags_minus_minus and "ITERATIVE" in tags_minus_minus): 
                #TODO: if a parameter name is used more than once (can happen by using several solver),
                #      the parameter tag mapping is not unique -> not proper handled right now
                value = dups # assumption: number of threads is an integer
                params[par_index+1] = str(value)
        handled_solvers.append(solver)
        thread_to_solver[parallel_thread] = solver
        thread_to_params[parallel_thread] = params
    
def __sorted_dict_values(adict):
    keys = adict.keys()
    keys.sort()
    return map(adict.get, keys)

def __dic_to_flat_list(adict, prefix, separator):
    '''
        convert dictionary to flat parameter list
        Args:
            adict: dictionary : head -> value
            prefix: prefix of parameter head, e.g. "--"
    '''
    allP = []
    for k,v in adict.items():
        if (type(v) is dict):
            vsort = __sorted_dict_values(v)
            sortedvalues = ",".join(vsort)
            if separator == " ":
                allP.append(prefix+k)
                allP.append(sortedvalues)
            else:
                allP.append(prefix+k+separator+sortedvalues )
        else:
            if (type(v) is list):
                allP.extend(v)
            else:
                if (v == ""):   # flag handling
                    allP.append(prefix+k)
                else:
                    if separator == " ":
                        allP.append(prefix+k)
                        allP.append(v)
                    else:
                        allP.append(prefix+k+separator+v)
                        
    return allP

def get_commmand(s):
    # replace ugly pcs hack for advanced forbidden syntax
    params = map(lambda s: s.replace('_AT_', '@').replace('_COLON_', ':').replace('_HYPHEN_', '-').replace('_PLUS_', '+'), s.split(' '))
    configMap = dict((name, value) for name, value in zip(params[::2], params[1::2]))
    return get_command_line_cmd(configMap)