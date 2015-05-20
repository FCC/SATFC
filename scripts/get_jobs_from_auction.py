jobs = []
prefix = "/home/afrechet/scratch/experiments/satfc-paper/instances/april27-instances/"
auctions = ['2921', '2920', '2910', '2913', '2914', '2916', '2918', '2919']
for auction in auctions:
	srpks = !find $auction/UHF/srpks -name "*.srpk" | sed 's/^\.\///'
	srpks = map(lambda srpk: prefix + srpk, srpks)
	jobs += srpks
with open('jobs.txt', 'w') as out:
	for job in jobs:
		out.write(job+'\n')