import json
import csv
import argparse

def make_row(taename, metric):
    row = {'TAE Name' : taename, 'TAE Configuration': 'DEFAULT', 'Cutoff': '60.0', 'Seed': '-1', 'Additional Run Data': ''}
    row['Instance'] = metric['name'].replace('.srpk', '')
    row['Run Result'] = metric['result']
    row['Run Time'] = metric['runtime']
    return row

parser = argparse.ArgumentParser(description="Use this script to go from SATFC metrics output to a TAE CSV style file")
parser.add_argument('-metrics', type=str, required=True)
parser.add_argument('-taename', type=str, required=True)
parser.add_argument('-output', type=str, required=True)
parser.add_argument('-components', type=bool, default=False)
args = parser.parse_args()
f = open(args.metrics, 'r')
rows = []
for line in f.readlines():
    obj = json.loads(line)
    if not args.components:
        row = make_row(args.taename, obj)
        rows.append(row)
    else:
        for component in obj['components']:
            row = make_row(args.taename, component)
            rows.append(row)
f.close()

with open(args.output, 'w') as csvfile:
	fieldnames = ['TAE Name','TAE Configuration','Cutoff','Seed','Instance','Run Result','Run Time','Additional Run Data']
	writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
	writer.writeheader()
	writer.writerows(rows)
