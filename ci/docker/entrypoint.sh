#!/bin/sh
umask 0000
export JAVA_HOME=/opt/mongoose
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${JAVA_HOME}/bin
java -jar /opt/mongoose/mongoose.jar "$@"
