#!/bin/sh
umask 0000
robot --outputdir /root/mongoose/build/robotest --suite ${SUITE} --include ${TEST} /root/mongoose/src/test/robot
rebot /root/mongoose/build/robotest/output.xml
