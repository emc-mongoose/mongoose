#!/bin/sh
umask 0000
./gradlew clean systemTest --tests com.emc.mongoose.system.${TEST}
