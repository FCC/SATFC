# Takes in a TAE csv file with a separate entry for every component and stiches together an output CSV that combines all of the results
import csv
import argparse
import re
from collections import defaultdict

def merge_results(results):
	return "UNSAT" if any([result == "UNSAT" for result in results]) else "TIMEOUT" if any([result == "TIMEOUT" for result in results]) else "SAT"

parser = argparse.ArgumentParser()
parser.add_argument('-csv', type=str, required=True)
parser.add_argument('-output', type=str, required=True)
args = parser.parse_args()


srpk_to_rows = defaultdict(list)

with open(args.csv, 'r') as data:
	reader = csv.reader(data)
	for i, row in enumerate(reader):
		if i == 0:
			header = row
		else: 
			name = row[4]
			if not "component" in name:
				continue
			srpk_name = re.findall(r'(.*)_component.*', name)[0]
			srpk_to_rows[srpk_name].append(row)

#parallel = []
sum_of_runtimes = []
java_exec = []

for srpk, srpk_rows in srpk_to_rows.items():
	srpk_rows.sort(key = lambda row : int(re.findall(r'.*_component(.*)', row[4])[0])) #sort by component number
	# 4 is name, 5 is result, 6 is runtime
	results = [srpk_row[5] for srpk_row in srpk_rows]

	java_exec_row = list(srpk_rows[0])
	java_exec_row[0] = args.output 
	java_exec_row[4] = srpk 
	java_exec_runtime = 0.0
	result = "SAT"
	for r in srpk_rows:
		java_exec_runtime += float(r[6])
		if java_exec_runtime > 60.0 or r[5] == 'TIMEOUT':
			result = "TIMEOUT"
			break
		elif r[5] == "UNSAT":
			result = "UNSAT"
			break
				
	java_exec_row[5] = result
	java_exec_row[6] = java_exec_runtime
	java_exec.append(java_exec_row)

with open(args.output, 'w') as out:
	writer = csv.writer(out)
	writer.writerows([header])
	writer.writerows(java_exec)
