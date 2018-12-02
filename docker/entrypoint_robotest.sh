#!/bin/sh
umask 0000
robot --outputdir /root/mongoose/build/robotest --suite ${SUITE} --include ${TEST} /root/mongoose/src/test/robot
ls -l /root
ls -l /root/mongoose
ls -l /root/mongoose/build
ls -l /root/mongoose/build/robotest
rebot /root/mongoose/build/robotest/output.xml
