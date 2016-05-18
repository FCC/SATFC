import json

SAT_KEY_MATCH_STR = '*:SAT*'
OFFICIAL_C_TYPE = ['CO', 'ADJ+1', 'ADJ-1', 'ADJ+2', 'ADJ-2']
COMPACT_C_TYPE = ['CO', 'ADJ+1', 'ADJ+2']

C_TYPE_INDEX = 0
FIRST_CHANNEL_INDEX = 1
SECOND_CHANNEL_INDEX = 2
STUDY_CHANNEL_INDEX = 3
INTERFERING_STATIONS_START_INDEX = 4

DOMAIN_STATION_INDEX = 1
DOMAIN_CHANNELS_START_INDEX = 2


class AutoVivification(dict):
    """Implementation of perl's autovivification feature."""
    def __getitem__(self, item):
        try:
            return dict.__getitem__(self, item)
        except KeyError:
            value = self[item] = type(self)()
            return value

interference_dict = AutoVivification() # 4 level deep (station, channel, constraint type, actual constraint) nested dictionary of interference constraints
domain_dict = {}

def str_minus_one(string):
    return str(int(string)-1)

def str_add_one(string):
    return str(int(string)+1)

def str_add_two(string):
    return str(int(string)+2)

def log_to_station_channel_map(assignment):
    j = json.loads(assignment)
    station_to_channel = {}
    for key, value in j.iteritems():
        for i in range(0, len(value)):
            station_to_channel[value[i]] = key
    return station_to_channel

def append_constraint(existing_cons, new_cons):
    if(existing_cons):
        existing_cons.append(new_cons)
        return existing_cons
    else:
        return [new_cons]

def store_inferred_interference(interference_dict, line):
    cons = line.split(',')
    station = cons[STUDY_CHANNEL_INDEX]
    channel = cons[FIRST_CHANNEL_INDEX]
    c_type = cons[C_TYPE_INDEX]
    if(c_type not in COMPACT_C_TYPE):
        raise ValueError('unexpected constraint type in compact interference: ' + c_type)

    type_co = 'CO'
    type_plus_one = 'ADJ+1'
    if(c_type == 'ADJ+1'):
        # inferred constraints from ADJ+1 constraint
        cons[SECOND_CHANNEL_INDEX] = str_add_one(cons[FIRST_CHANNEL_INDEX]) # ADJ+1,c,c+1,s...
        interference_dict[station][channel][c_type] = append_constraint(interference_dict[station][channel][c_type], cons)

        plus_one_first_co = list(cons) # make a new copy
        plus_one_first_co[C_TYPE_INDEX] = type_co
        plus_one_first_co[SECOND_CHANNEL_INDEX] = cons[FIRST_CHANNEL_INDEX]
        interference_dict[station][channel][type_co] = append_constraint(interference_dict[station][channel][type_co], plus_one_first_co)

        plus_one_second_co = list(cons)
        plus_one_second_co[C_TYPE_INDEX] = type_co
        plus_one_second_co[FIRST_CHANNEL_INDEX] = str_add_one(cons[FIRST_CHANNEL_INDEX])
        interference_dict[station][str_add_one(channel)][type_co] = append_constraint(interference_dict[station][str_add_one(channel)][type_co], plus_one_second_co)

    # inferred constraints from ADJ+2 constraint
    elif(c_type == 'ADJ+2'):
        cons[SECOND_CHANNEL_INDEX] = str_add_two(cons[FIRST_CHANNEL_INDEX]) # ADJ+2,c,c+2,s...
        interference_dict[station][channel][c_type] = append_constraint(interference_dict[station][channel][c_type], cons)

        plus_two_first_co = list(cons)
        plus_two_first_co[C_TYPE_INDEX] = type_co
        plus_two_first_co[SECOND_CHANNEL_INDEX] = cons[FIRST_CHANNEL_INDEX]
        interference_dict[station][channel][type_co] = append_constraint(interference_dict[station][channel][type_co], plus_two_first_co)

        plus_two_second_co = list(cons)
        plus_two_first_co[C_TYPE_INDEX] = type_co
        plus_two_second_co[FIRST_CHANNEL_INDEX] = str_add_one(cons[FIRST_CHANNEL_INDEX])
        plus_two_second_co[SECOND_CHANNEL_INDEX] = str_add_one(cons[FIRST_CHANNEL_INDEX])
        interference_dict[station][str_add_one(channel)][type_co] = append_constraint(interference_dict[station][str_add_one(channel)][type_co], plus_two_second_co)

        plus_two_third_co = list(cons)
        plus_two_first_co[C_TYPE_INDEX] = type_co
        plus_two_third_co[FIRST_CHANNEL_INDEX] = str_add_two(cons[FIRST_CHANNEL_INDEX])
        interference_dict[station][str_add_two(channel)][type_co] = append_constraint(interference_dict[station][str_add_two(channel)][type_co], plus_two_third_co)

        first_plus_one = list(cons)
        first_plus_one[C_TYPE_INDEX] = type_plus_one
        first_plus_one[SECOND_CHANNEL_INDEX] = str_add_one(cons[FIRST_CHANNEL_INDEX])
        interference_dict[station][channel][type_plus_one] = append_constraint(interference_dict[station][channel][type_plus_one], first_plus_one)

        second_plus_one = list(cons)
        second_plus_one[C_TYPE_INDEX] = type_plus_one
        second_plus_one[FIRST_CHANNEL_INDEX] = str_add_one(cons[FIRST_CHANNEL_INDEX])
        interference_dict[station][str_add_one(channel)][type_plus_one] = append_constraint(interference_dict[station][str_add_one(channel)][type_plus_one], second_plus_one)

    elif(c_type == 'CO'):
        interference_dict[station][channel][c_type] = append_constraint(interference_dict[station][channel][c_type], cons)

def load_compact_interference(path):

    with open(path, 'r') as f:
        lines = f.read().splitlines()
        for line in lines:
            store_inferred_interference(interference_dict, line)

    return 0

def store_interference(interference_dict, line):
    cons = line.split(',')
    station = cons[STUDY_CHANNEL_INDEX]
    channel = cons[FIRST_CHANNEL_INDEX]
    c_type = cons[C_TYPE_INDEX]
    if(c_type not in OFFICIAL_C_TYPE):
        raise ValueError('unexpected constraint type in interference: ' + c_type)

    interference_dict[station][channel][c_type] = append_constraint(interference_dict[station][channel][c_type], cons)

def load_interference(path):
    interference_dict = AutoVivification() # 4 level deep (station, channel, constraint type, actual constraint) nested dictionary of interference constraints

    with open(path, 'r') as f:
        lines = f.read().splitlines()
        for line in lines:
            store_interference(interference_dict, line)

    return 0

def check_constraint_violation(station_to_channel, interference_dict):
    for station in station_to_channel:
        assigned_channel = station_to_channel[station]
        if(interference_dict[station][assigned_channel]):
            for c_type in interference_dict[station][assigned_channel]:
                constraints = interference_dict[station][assigned_channel][c_type]
                for cons in constraints:
                    illegal_channel = cons[SECOND_CHANNEL_INDEX]

                    interfering_stations = cons[INTERFERING_STATIONS_START_INDEX:]
                    for i_station in interfering_stations:
                        if(station_to_channel.has_key(i_station)):
                            i_station_channel = station_to_channel[i_station]
                            if(i_station_channel == illegal_channel):
                                #print 'study station '+ str(station) + ' on channel ' + str(assigned_channel) + ' , interfering station ' + str(i_station) + ' on channel ' + str(illegal_channel) + '. Violated constraint (may be inferred): ' + str(cons)
                                return [i_station, illegal_channel, str(cons)]

def load_domain_csv(path):
    f = open(path, 'r')
    lines = f.read().splitlines()
    for line in lines:
        dom = line.split(',')
        station = dom[DOMAIN_STATION_INDEX]
        channels = dom[DOMAIN_CHANNELS_START_INDEX:]
        domain_dict[station] = channels
    f.close()
    return 0

def check_domain(station_to_channel, domain_dict):
    for station in station_to_channel:
        assigned_channel = station_to_channel[station]
        if assigned_channel not in domain_dict[station]:
            #print 'assigned channel ' + str(assigned_channel) + ' not on station ' + str(station) + '\'s domain: ' + str(domain_dict[station])
            return [station, assigned_channel, str(station) + ":" + str(domain_dict[station])]

def check_violations(assignment):
    station_to_channel = log_to_station_channel_map(assignment)

    domain_result = check_domain(station_to_channel, domain_dict)
    if(domain_result):
        return domain_result

    interference_result = check_constraint_violation(station_to_channel, interference_dict)
    if(interference_result):
        return interference_result