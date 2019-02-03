*** Settings ***
Documentation  Mongoose S3 Storage Driver Tests
Force Tags  S3
Library  Collections
Library  OperatingSystem
Library  Validation
Resource  MongooseContainer.robot
Test Setup  Start S3 Server
Test Teardown  Remove Containers

*** Variables ***
${DATA_DIR} =  base/src/test/robot/api/storage/data
${LOG_DIR} =  base/build/log
${S3_IMAGE_NAME} =  minio/minio
${S3_IMAGE_VERSION} =  latest
${S3_PORT} =  9000
${S3_UID} =  user1
${S3_SECRET_KEY} =  secretKey1
${S3_STORAGE_CONTAINER_NAME} =  s3_server
${MONGOOSE_SHARED_ARGS} =  --storage-driver-type=s3 --storage-net-node-port=${S3_PORT} --storage-auth-uid=${S3_UID} --storage-auth-secret=${S3_SECRET_KEY}

*** Test Cases ***
Should Copy Objects Using Bucket Listing
    ${step_id} =  Set Variable  copy_objects_using_bucket_listing
    ${object_count_limit} =  Convert To Integer  1000
    Remove Directory  ${LOG_DIR}/${step_id}  recursive=True
    ${args} =  Catenate  SEPARATOR= \\\n\t
    ...  --item-data-size=10KB
    ...  --load-op-limit-count=${object_count_limit}
    ...  --load-step-id=${step_id}
    ...  --storage-driver-limit-concurrency=10
    ...  --run-scenario=${MONGOOSE_CONTAINER_DATA_DIR}/copy_using_input_path.js
    &{env_params} =  Create Dictionary  ITEM_SRC_PATH=/bucket0  ITEM_DST_PATH=/bucket1
    ${std_out} =  Execute Mongoose Scenario  ${DATA_DIR}  ${env_params}  ${MONGOOSE_SHARED_ARGS} ${args}
    Log  ${std_out}
    Validate Log File Metrics Total  ${LOG_DIR}/${step_id}  count_succ_min=${object_count_limit}
    ...  count_succ_max=${object_count_limit}  count_fail_max=${0}  transfer_size=${10240000}

Should Create Objects Using Multipart Upload
    ${step_id} =  Set Variable  create_objects_using_multipart_upload
    Remove Directory  ${LOG_DIR}/${step_id}  recursive=True
    Remove File  ${DATA_DIR}/${step_id}.csv
    ${args} =  Catenate  SEPARATOR= \\\n\t
    ...  --item-data-ranges-threshold=16MB
    ...  --item-data-size=20MB-100MB
    ...  --item-output-file=${MONGOOSE_CONTAINER_DATA_DIR}/${step_id}.csv
    ...  --item-output-path=mpu
    ...  --load-batch-size=1
    ...  --load-step-limit-size=2GB
    ...  --load-step-id=${step_id}
    ...  --storage-driver-limit-concurrency=20
    &{env_params} =  Create Dictionary
    ${std_out} =  Execute Mongoose Scenario  ${DATA_DIR}  ${env_params}  ${MONGOOSE_SHARED_ARGS} ${args}
    Log  ${std_out}
    Validate Log File Metrics Total  ${LOG_DIR}/${step_id}  count_succ_min=${10}  count_succ_max=${100}
    ...  count_fail_max=${10}  transfer_size=${2147483648}  transfer_size_delta=${167772160}
    Validate Item Output File  item_output_file_name=${DATA_DIR}/${step_id}.csv  item_output_path=mpu
    ...  item_size_min=${20971520}  item_size_max=${104857600}

Should Read Single Random Byte Ranges
    ${step_id} =  Set Variable  read_multiple_random_byte_ranges
    ${object_count_limit} =  Convert To Integer  1000
    ${random_byte_range_count} =  Convert To Integer  1
    Remove Directory  ${LOG_DIR}/${step_id}  recursive=True
    Remove File  ${DATA_DIR}/${step_id}.csv
    ${args} =  Catenate  SEPARATOR= \\\n\t
    ...  --item-data-size=100KB
    ...  --item-data-verify
    ...  --load-op-limit-count=${object_count_limit}
    ...  --load-step-id=${step_id}
    ...  --run-scenario=${MONGOOSE_CONTAINER_DATA_DIR}/${step_id}.js
    ...  --storage-driver-limit-concurrency=50
    &{env_params} =  Create Dictionary  ITEM_LIST_FILE=${MONGOOSE_CONTAINER_DATA_DIR}/${step_id}.csv  RANDOM_BYTE_RANGE_COUNT=${random_byte_range_count}
    ${std_out} =  Execute Mongoose Scenario  ${DATA_DIR}  ${env_params}  ${MONGOOSE_SHARED_ARGS} ${args}
    Log  ${std_out}
    Validate Log File Metrics Total  ${LOG_DIR}/${step_id}  op_type=READ  count_succ_min=${object_count_limit}
    ...  count_succ_max=${object_count_limit}  transfer_size=${32768000}  transfer_size_delta=${27680000}

Should Update Multiple Random Byte Ranges
    ${step_id} =  Set Variable  update_multiple_random_byte_ranges
    ${object_count_limit} =  Convert To Integer  1000
    ${random_byte_range_count} =  Convert To Integer  5
    Remove Directory  ${LOG_DIR}/${step_id}  recursive=True
    Remove File  ${DATA_DIR}/${step_id}.csv
    ${args} =  Catenate  SEPARATOR= \\\n\t
    ...  --item-data-size=1KB
    ...  --load-op-limit-count=${object_count_limit}
    ...  --load-step-id=${step_id}
    ...  --run-scenario=${MONGOOSE_CONTAINER_DATA_DIR}/${step_id}.js
    ...  --storage-driver-limit-concurrency=100
    &{env_params} =  Create Dictionary  ITEM_LIST_FILE=${MONGOOSE_CONTAINER_DATA_DIR}/${step_id}.csv  RANDOM_BYTE_RANGE_COUNT=${random_byte_range_count}
    ${std_out} =  Execute Mongoose Scenario  ${DATA_DIR}  ${env_params}  ${MONGOOSE_SHARED_ARGS} ${args}
    Log  ${std_out}
    Validate Log File Metrics Total  ${LOG_DIR}/${step_id}  op_type=UPDATE  count_succ_min=${object_count_limit}
    ...  count_succ_max=${object_count_limit}  transfer_size=${507000}  transfer_size_delta=${100000}

Should Create Objects With Custom Headers
    ${step_id} =  Set Variable  custom_headers
    Remove Directory  ${LOG_DIR}/${step_id}  recursive=True
    ${args} =  Catenate  SEPARATOR= \\\n\t
    ...  --item-output-path=/bucket2
    ...  --load-op-limit-count=10
    ...  --load-step-id=${step_id}
    ...  --storage-driver-limit-concurrency=0
    ...  --run-scenario=${MONGOOSE_CONTAINER_DATA_DIR}/${step_id}.js
    &{env_params} =  Create Dictionary
    ${std_out} =  Execute Mongoose Scenario  ${DATA_DIR}  ${env_params}  ${MONGOOSE_SHARED_ARGS} ${args}
    Log  ${std_out}
    Validate Log File Metrics Total  ${LOG_DIR}/${step_id}  count_succ_min=${10}  count_succ_max=${10}
    ...  transfer_size=${10485760}  transfer_size_delta=${0}

*** Keywords ***
Start S3 Server
    ${cmd} =  Catenate  SEPARATOR= \\\n\t
    ...  docker run
    ...  --detach
    ...  --name ${S3_STORAGE_CONTAINER_NAME}
    ...  --publish ${S3_PORT}:${S3_PORT}
    ...  --env MINIO_ACCESS_KEY=${S3_UID}
    ...  --env MINIO_SECRET_KEY=${S3_SECRET_KEY}
    ...  ${S3_IMAGE_NAME}:${S3_IMAGE_VERSION}
    ...  server /data
    ${std_out} =  Run  ${cmd}
    Log  ${std_out}

Remove S3 Server
    Run  docker stop ${S3_STORAGE_CONTAINER_NAME}
    Run  docker rm ${S3_STORAGE_CONTAINER_NAME}

Remove Containers
    Remove S3 Server
    Remove Mongoose Container
