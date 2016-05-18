import redis
import argparse

HASHNUM_KEY = 'SATFC:HASHNUM'

"""Merge the entries from redis cache A into cache B"""

def merge(r_a, r_b):	
	r_b_hashnum = int(r_b.incr(HASHNUM_KEY, amount=0))
	match = "SATFC:*SAT*"
	for i, key in enumerate(r_a.scan_iter(match=match)):
		if i % 10000 == 0:
			print "Merged %d keys" % (i)
		val = r_a.hgetall(key)
		new_key = ':'.join(key.split(':')[:-1]) + ':' + str(r_b_hashnum + i + 1)
		r_b.hmset(new_key, val)
	print "Done merging %d keys" % (i)
	r_b.incr(HASHNUM_KEY, i + 1)

if __name__ == '__main__':
	parser = argparse.ArgumentParser()
	parser.add_argument('--host_a', type=str, help="first redis host", required=True)
	parser.add_argument('--port_a', type=int, help="first redis port", required=True)
	parser.add_argument('--host_b', type=str, help="second redis host", required=True)
	parser.add_argument('--port_b', type=int, help="second redis port", required=True)
	args = parser.parse_args()

	r_a = redis.StrictRedis(host=args.host_a, port=args.port_a)
	r_b = redis.StrictRedis(host=args.host_b, port=args.port_b)
	print "Merging all of the keys from %s:%d to %s:%d" % (args.host_a, args.port_a, args.host_b, args.port_b)
	merge(r_a, r_b)
	print "Done"