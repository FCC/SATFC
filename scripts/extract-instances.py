import sys
import os
import csv
import argparse
from collections import Counter

LVHF_Channels = (2,3,4,5,6)
HVHF_Channels = (7,8,9,10,11,12,13)
UHF_Channels = (14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,38,39,40,41,42,43,44,45,46,47,48,49,50,51)

# Parse args
parser = argparse.ArgumentParser()
parser.add_argument('--instancedir', type=str, help='Directory containing the question files')
parser.add_argument('--outdir', type=str, help='Directory to write the srpks to')
args = parser.parse_args()

instancedir = args.instancedir
outdir = args.outdir
dirname = os.path.basename(os.path.normpath(os.path.abspath(instancedir)))
band_counter = Counter()
highest_channel = -1

def get_band(problem):
	max_channel = max(max(problem.values(), key=max))
	min_channel = min(min(problem.values(), key=min))
	if (min_channel in LVHF_Channels or min_channel in HVHF_Channels) and (max_channel in LVHF_Channels or max_channel in HVHF_Channels):
		return "VHF", max_channel
	else:
		return "UHF", max_channel

print 'Looking in "%s" for all the csv problems lists...' % instancedir

filenames = []
for (dp,d,f) in os.walk(instancedir):
    filenames.extend(f)
    break

filenames = filter(lambda f : f.split('.')[-1] == 'csv',filenames)

print '%d problem lists to process.' % len(filenames)

i=0
for filename in filenames:
    i+=1

    filename = os.path.join(instancedir,filename)

    instancename, ext = os.path.splitext(os.path.basename(filename))

    print '[%d/%d] Extracting srpk problems from "%s" into "%s" ...' % (i,len(filenames),filename,outdir)

    with open(filename,'r') as instancefile:
        reader = csv.reader(instancefile)

        interference_data = None
        cutoff = None

        #Parse header
        for row in reader:
            key = row[0]

            if key == 'constraints':
                interference_data = row[1]
            elif key == 'timeout':
                cutoff = float(row[1])/1000.0
            elif key == 'shared_info':
                break
            elif key == 'sequence_id':
				sequence_id = int(row[1])
            else:
                raise ValueError('Unrecognized instances header key "%s".' % str(key))

        previous_assignment = {}
        current_problem = {}

        #Parse assignment
        for row in reader:
            key = row[0]
            if key == 'problems':
                break
            else:
                station = int(row[0])
                previous_channel = int(row[1])
                domain = [int(r) for r in row[2:]]
                previous_assignment[station] = previous_channel
                current_problem[station] = domain

        #Parse problems
        p=0
        for row in reader:
            p+=1
            key = row[0]
            if key == 'answers':
                break
            else:
                station = int(row[0])
                domain = [int(r) for r in row[1:]]

                problem = dict(current_problem)
                problem[station] = domain
                band, problem_highest_channel = get_band(problem)
                highest_channel = max(highest_channel, problem_highest_channel)
                band_counter[band] += 1

                #Write problem to file.
                problem_name = '%s_%s_%d_%d.srpk' % (dirname,instancename,sequence_id,p)
                problem_filename = os.path.join(outdir,band,'srpks',problem_name)
                with open(problem_filename,'w') as problemfile:
                    writer = csv.writer(problemfile)
                    writer.writerow(['INTERFERENCE',interference_data])
                    writer.writerow(['CUTOFF',cutoff])
                    for station in sorted(problem.keys()):
                        previous_channel = previous_assignment.get(station,-1)
                        domain = sorted(problem[station])
                        writer.writerow([station,previous_channel]+domain)

    print '... %d problems extracted.' % p
print band_counter
print "This auction has a highest channel of %d" % highest_channel
print 'DONE'
