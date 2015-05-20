import redis
import json
import argparse
import os

def write_file(fname, cnfs):
	with open(fname, 'w') as out:
		for cnf in cnfs:
			out.write(cnf + '\n')

def make_indices(r, qname):
    component_cnfs = set()
    full_instance_cnfs = set()
    presat_cnfs = set()
    cnf_to_srpk_index = {}

    match = "%s:CNFIndex:*" % (qname)
    keys = list(r.scan_iter(match=match))
    print "Found %d keys" % (len(keys))
    for key in keys:
        srpks = r.lrange(key, 0, -1)
        cnf = key.split(':')[-1] + '.cnf'
        cnf_to_srpk_index[cnf] = srpks
        for srpk in srpks:
            if 'component' in srpk:
                component_cnfs.add(cnf)
            elif 'StationSubsetSATCertifier' in srpk:
                presat_cnfs.add(cnf)
            else:
                full_instance_cnfs.add(cnf)

    write_file(qname + '_Full_Instance_CNFs.txt', full_instance_cnfs)
    write_file(qname + '_Component_CNFs.txt', component_cnfs)
    write_file(qname + '_PreSAT_CNFs.txt', presat_cnfs)
    with open(qname + '_Reverse_Index.txt', 'w') as reverse_index:
        json.dump(cnf_to_srpk_index, reverse_index)

def write_cnf_files(r, qname):
    match = "%s:CNF:*" % (qname)
    for key in r.scan_iter(match=match):
        cnf_name = key.split(':')[-1]
        cnf = r.get(key)
        cnfdir = "cnfs"
        if not os.path.exists(cnfdir):
            os.mkdir(cnfdir)
        with open(os.path.join(cnfdir, cnf_name + '.cnf'), 'w') as cnf_file: 
            cnf_file.write(cnf)
        assignment_key = "%s:CNFAssignment:%s" % (qname, cnf_name)
        if r.exists(assignment_key):
            assignment = r.get(assignment_key)
            with open(os.path.join(cnfdir, cnf_name + '_assignment.txt'), 'w') as assignment_file: 
                assignment_file.write(assignment)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--qname', type=str, help="redis queue", required=True)
    parser.add_argument('--host', type=str, help="redis host", required=True)
    parser.add_argument('--port', type=int, help="redis port", required=True)
    args = parser.parse_args()

    r = redis.StrictRedis(host=args.host, port=args.port)
    make_indices(r, args.qname)
    write_cnf_files(r, args.qname)