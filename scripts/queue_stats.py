# coding: utf-8
import redis
import argparse

def metrics(r, qname):
	remaining_jobs = r.llen(qname)
	processing_jobs = r.llen(qname+'_PROCESSING')
	timeouts = r.llen(qname+'_TIMEOUTS')
	print "There are %d jobs in the queue" % remaining_jobs
	print "There are %d jobs in the processing queue" % processing_jobs
	print "There are %d timeouts" % timeouts

if __name__ == '__main__':
	parser = argparse.ArgumentParser()
	parser.add_argument('--qname', type=str, help="redis queue to delete")
	parser.add_argument('--host', type=str, help="redis host")
	parser.add_argument('--port', type=int, help="redis port")
	args = parser.parse_args()

	r = redis.StrictRedis(host=args.host, port=args.port)
	metrics(r, args.qname)