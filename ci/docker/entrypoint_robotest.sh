#!/bin/sh
umask 0000
robot --outputdir /root/mongoose/base/build/robotest --suite ${SUITE} --include ${TEST} /root/mongoose/base/src/test/robot
rebot /root/mongoose/base/build/robotest/output.xml
