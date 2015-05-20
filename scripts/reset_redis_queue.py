# coding: utf-8
import redis
import argparse

# Load up a redis queue with jobs
parser = argparse.ArgumentParser()
parser.add_argument('--qname', type=str, help="redis queue to delete", required=True)
parser.add_argument('--host', type=str, help="redis host", required=True)
parser.add_argument('--port', type=int, help="redis port", required=True)
args = parser.parse_args()

r = redis.StrictRedis(host=args.host, port=args.port)
print "Removing all keys matching %s:*" % args.qname
match = "%s:*" % (args.qname)
keys = list(r.scan_iter(match=match))
keys_removed = 0
keys_removed += r.delete(*keys)
print "Removing other queues"
keys_removed += r.delete(args.qname)
keys_removed += r.delete(args.qname + '_PROCESSING')
keys_removed += r.delete(args.qname + '_TIMEOUTS')
print "Removed %d keys" % keys_removed