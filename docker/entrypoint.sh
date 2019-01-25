#!/bin/sh
umask 0000
export JAVA_HOME=/opt/mongoose/jre
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${JAVA_HOME}/bin
java --module com.emc.mongoose/com.emc.mongoose.Main "$@"
