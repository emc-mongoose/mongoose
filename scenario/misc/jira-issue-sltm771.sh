#!/bin/bash
echo "Test Start...."

# Essential Stuffs
export DATA_NODES="127.0.0.1"
export CLIENTS="127.0.0.1"

export USER_ID=user1
export KEY="KcdSSHHxju0RNmv149Ho3ddxQ8NtA5ZG/+DsJEMO"

# More Essential Stuffs
export MONGOOSE_DIR=./mongoose-2.5.1
export TOOL_DIR=.
export RUN_TIME=30
export WAIT_TIME=5
export INIT_RUN_TIME=30


export COLLECT_STATS="--stats"
export COLLECT_ECS_LOGS="--ecs-log"
export COLLECT_CONFIG="--config"
export DB_IMPORT="--import"
export DELETE_MONGOOSE_LOG="--delete-log"
export ENGINEER="DanDao"
export PROJECT="Test"
export NOTIFY="dan.dao@emc.com"
export LOG_DIR=$PWD



# To run S3 MIX workload
export CREATE_PERCENT=50
export READ_PERCENT=50
java -Dstorage.addrs=${DATA_NODES} -Dauth.id=${USER_ID} -Dauth.secret=${KEY} -Dload.server.addrs=${CLIENTS} -Ditem.dst.container=bucket2 -jar ${MONGOOSE_DIR}/mongoose.jar client -f ${MONGOOSE_DIR}/scenario/misc/asdjira-sltm-issue-771.json

