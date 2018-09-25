#!/usr/bin/python           

import socket               

s = socket.socket()         # Create a socket object
host = socket.gethostname() # Get local machine name
port = 5000                # Reserve a port for your service.
s.bind((host, port))        # Bind to the port

s.listen(5)                 # Now wait for client connection.

try:
    while True:
       c, addr = s.accept()     # Establish connection with client.
       print 'Got connection from', addr
       c.send('Thank you for connecting')
       c.close()                # Close the connection
except KeyboardInterrupt:
    exit()
