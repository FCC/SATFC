# coding: utf-8
# Go from a csv file with full paths to cnfs to the corresponding srpk file (not full path)
import argparse
import json
import csv
import os
from collections import defaultdict

parser = argparse.ArgumentParser()
parser.add_argument('-cnf_to_srpk_index', type=str, required=True)
parser.add_argument('-input', type=str, required=True, help="A TAE csv file")
parser.add_argument('-output', type=str, required=True, help="Name of output TAE csv file")
args = parser.parse_args()

with open(args.cnf_to_srpk_index, 'r') as index:
	mapping = json.load(index)

new_rows = set()
with open(args.input, 'r') as data:
        reader = csv.reader(data)
        for i, row in enumerate(reader):
                if i > 0:
            	        cnf_name = os.sep.join(row[4].split(os.sep)[-2:]) # extract just cnf name, not full path
			for srpk_name in mapping[cnf_name]:
				new_row = row			
        			new_row[4] = srpk_name.replace('.srpk', '').replace('_StationSubsetSATCertifier', '')
			        new_rows.add(tuple(new_row))
		else:
			header = row
with open(args.output, 'w') as out:
    writer = csv.writer(out)
    writer.writerows([header])
    writer.writerows(new_rows)            
