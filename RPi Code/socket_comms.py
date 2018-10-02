import socket
from _thread import *
import ast
import io
import picamera
import logging
import socketserver
from threading import Condition
from http import server
import time
import wiringpi
from noise import pnoise1


"""
ToDo:
Save state of otions and send them out on socket connect
More camera settings like low light etc
"""
MAX_SERVO = 250;
MIN_SERVO = 50;
MAIN_LOOP_DELAY = 0.01
PERLIN_SPEED = 2500     #the larger the smmoother

varDict = {'x':0,
           'y':0,
           'mode':0,    #0:manual, 1:perlin, 2:random, 3:still
           'quality':0, #0:1080p30, 1:720p30, 2:720p60, 3:480p30, 4:480p60, 5:480p90
           'update':1}

#COde to handle communication with client sockets
def threaded_comms(soc, addr):
    data = ''
    msg = ''
    while True:
        data = soc.recv(16)
        if not data:
            break
        msg = data.decode(encoding='UTF-8')
        msg = msg.replace("{", "{\'").replace("=", "\': ").rstrip()    
        print(addr[0] + ":" + msg)
        d = ast.literal_eval(msg);
        for key in varDict:
            if key in d and (key == 'x' or key == 'y'):
                varDict[key] += d[key]
                varDict[key] = max(MIN_SERVO, min(MAX_SERVO, varDict[key]))
            elif key in d:
                varDict[key] = d[key]                
        
#Setup socket        
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
print('Socket created')
try:
    s.bind(('192.168.4.1', 5000))
except socket.error as err:
    print('Bind failed. Error Code : ' .format(err))
s.listen(4)
print("Socket Listening")

#create thread to handle listening for clients
def create_clients():
    while True:
        conn, addr = s.accept()
        print("Connection extablished: ", addr)
        start_new_thread(threaded_comms,(conn,addr));
start_new_thread(create_clients, ())

# Web streaming example
# Adapted from source code in the official PiCamera package
# http://picamera.readthedocs.io/en/latest/recipes2.html#web-streaming

PAGE="""\
<style>
html,body{
    margin:0;
    height:100%;
}
img{
  display:block;
  width:100%; height:100%;
  object-fit: cover;
}
</style>
<html>
<body>
<img src="stream.mjpg" alt="">
</html>
"""

class StreamingOutput(object):
    def __init__(self):
        self.frame = None
        self.buffer = io.BytesIO()
        self.condition = Condition()

    def write(self, buf):
        if buf.startswith(b'\xff\xd8'):
            # New frame, copy the existing buffer's content and notify all
            # clients it's available
            self.buffer.truncate()
            with self.condition:
                self.frame = self.buffer.getvalue()
                self.condition.notify_all()
            self.buffer.seek(0)
        return self.buffer.write(buf)
class StreamingHandler(server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/':
            self.send_response(301)
            self.send_header('Location', '/index.html')
            self.end_headers()
        elif self.path == '/index.html':
            content = PAGE.encode('utf-8')
            self.send_response(200)
            self.send_header('Content-Type', 'text/html')
            self.send_header('Content-Length', len(content))
            self.end_headers()
            self.wfile.write(content)
        elif self.path == '/stream.mjpg':
            self.send_response(200)
            self.send_header('Age', 0)
            self.send_header('Cache-Control', 'no-cache, private')
            self.send_header('Pragma', 'no-cache')
            self.send_header('Content-Type', 'multipart/x-mixed-replace; boundary=FRAME')
            self.end_headers()
            try:
                while True:
                    with output.condition:
                        output.condition.wait()
                        frame = output.frame
                    self.wfile.write(b'--FRAME\r\n')
                    self.send_header('Content-Type', 'image/jpeg')
                    self.send_header('Content-Length', len(frame))
                    self.end_headers()
                    self.wfile.write(frame)
                    self.wfile.write(b'\r\n')
            except Exception as e:
                logging.warning(
                    'Removed streaming client %s: %s',
                    self.client_address, str(e))
        else:
            self.send_error(404)
            self.end_headers()
class StreamingServer(socketserver.ThreadingMixIn, server.HTTPServer):
    allow_reuse_address = True
    daemon_threads = True


#setup camera (with block ensures nice cleanup)
with picamera.PiCamera(resolution='1280x720', framerate=24) as camera:
    output = StreamingOutput()
    #camera.rotation = 90
    camera.start_recording(output, format='mjpeg')
    def run_streaming():
        try:
            address = ('', 8000)
            server = StreamingServer(address, StreamingHandler)
            print("Streaming")
            server.serve_forever()
        finally:
            camera.stop_recording()
    start_new_thread(run_streaming, ())

    #setup servos
    wiringpi.wiringPiSetupGpio()
    wiringpi.pinMode(18, wiringpi.GPIO.PWM_OUTPUT)
    wiringpi.pinMode(13, wiringpi.GPIO.PWM_OUTPUT)
    wiringpi.pwmSetMode(wiringpi.GPIO.PWM_MODE_MS)
    # divide down clock for servos
    wiringpi.pwmSetClock(192)
    wiringpi.pwmSetRange(2000)

    #handle servos and settings changes
    startMillis = int(round(time.time() * 1000))
    while True:
        time.sleep(MAIN_LOOP_DELAY)
        if varDict['mode'] == 0:
            wiringpi.pwmWrite(18, varDict['x'])
            wiringpi.pwmWrite(13, varDict['y'])
        elif varDict['mode'] == 1:
            millis = int(round(time.time() * 1000))
            x = pnoise1((startMillis-millis)/PERLIN_SPEED+1234.56789, octaves=4)
            y = pnoise1((startMillis-millis)/PERLIN_SPEED, octaves=4)
            wiringpi.pwmWrite(18, int((x+1)/2*200+50))
            wiringpi.pwmWrite(13, int((y+1)/2*200+50))
        elif varDict['mode'] == 2:
            #
            #   TODO: RANDOM LOOKING
            #
        else:
            wiringpi.pwmWrite(18, 150)
            wiringpi.pwmWrite(13, 150)

            
        if varDict['update'] != 0:
            varDict['update'] = 0
            camera.stop_recording()
            quality = varDict['quality']
            if quality == 0:
                camera.resolution = "1920x1080"
                camera.framerate = 30
            elif quality == 1:
                camera.resolution = "1280x720"
                camera.framerate = 30
            elif quality == 2:
                camera.resolution = "1280x720"
                camera.framerate = 60
            elif quality == 3:
                camera.resolution = "640x480"
                camera.framerate = 30
            elif quality == 4:
                camera.resolution = "640x480"
                camera.framerate = 60
            elif quality == 5:
                camera.resolution = "640x480"
                camera.framerate = 90
            camera.start_recording(output, format='mjpeg')
        


                           
