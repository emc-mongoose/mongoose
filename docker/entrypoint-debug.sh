#!/bin/sh
umask 0000
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar /opt/mongoose/mongoose.jar "$@"
