import socket

UDP_IP = "localhost"
UDP_PORT = 49149
MESSAGE = "PING"

MAXPACKETSIZE=65000

print "UDP target IP:", UDP_IP
print "UDP target port:", UDP_PORT
print "message:", MESSAGE

#Send a message

sendsock = socket.socket(socket.AF_INET, # Internet
                             socket.SOCK_DGRAM) # UDP

sendsock.sendto(MESSAGE.encode('ASCII'), (UDP_IP, UDP_PORT))
#Receive a message
while True:
        data, addr = sendsock.recvfrom(MAXPACKETSIZE) # buffer size is 1024 bytes
        print "received message:",data,'from',addr

