#!/bin/sh
umask 0000
echo Mongoose Version: ${MONGOOSE_VERSION}
echo Storage Type: ${STORAGE_TYPE}
echo Run Mode: ${RUN_MODE}
echo Concurrency Limit: ${CONCURRENCY}
echo Item Size: ${ITEM_SIZE}
echo Test: ${TEST}
#./gradlew clean systemTest --tests com.emc.mongoose.system.${TEST}
