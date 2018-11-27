#!/bin/sh
umask 0000
robot --outputdir /root/mongoose/build/robotest --suite "$1" --include "$2" /root/mongoose/src/test/robot
rebot /root/mongoose/build/robotest/output.xml
