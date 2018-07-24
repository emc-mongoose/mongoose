#!/bin/sh
umask 0000
export PATH=/usr/local/jdk/bin:/usr/kerberos/sbin:/usr/kerberos/bin:/usr/local/sbin:usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin:/opt/cpanel/composer/bin:/usr/local/easy/bin:/usr/local/bin:/usr/X11R6/bin:/root/bin
java -jar /opt/mongoose/mongoose.jar "$@"
