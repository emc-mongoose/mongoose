#!/bin/sh
umask 0000
robot --outputdir /root/mongoose/build /root/mongoose/src/test/robot
rebot /root/mongoose/build/output.xml
