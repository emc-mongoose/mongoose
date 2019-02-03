*** Settings ***
Documentation  Mongoose Swift Storage Driver Tests
Force Tags  Swift
Library  Collections
Library  String
Library  OperatingSystem
Library  String
Library  Validation
Resource  MongooseContainer.robot
Test Setup  Start Swift Server
Test Teardown  Remove Containers

*** Variables ***
${DATA_DIR} =  base/src/test/robot/api/storage/data
${LOG_DIR} =  base/build/log
${SWIFT_IMAGE_NAME} =  serverascode/swift-onlyone
${SWIFT_IMAGE_VERSION} =  latest
${SWIFT_PORT} =  8080
${SWIFT_UID} =  test:tester
${SWIFT_STORAGE_CONTAINER_NAME} =  swift_server
${MONGOOSE_SHARED_ARGS} =  --storage-driver-type=swift --storage-namespace=AUTH_test --storage-net-node-port=${SWIFT_PORT} --storage-auth-uid=${SWIFT_UID}

*** Test Cases ***
Should Copy Objects Using Container Listing
    ${step_id} =  Set Variable  copy_objects_using_container_listing
    ${object_count_limit} =  Convert To Integer  1000
    Remove Directory  ${LOG_DIR}/${step_id}  recursive=True
    ${args} =  Catenate  SEPARATOR= \\\n\t
    ...  --item-data-size=10KB
    ...  --load-op-limit-count=${object_count_limit}
    ...  --load-step-id=${step_id}
    ...  --storage-driver-limit-concurrency=10
    ...  --run-scenario=${MONGOOSE_CONTAINER_DATA_DIR}/copy_using_input_path.js
    &{env_params} =  Create Dictionary  ITEM_SRC_PATH=/container0  ITEM_DST_PATH=/container1
    ${std_out} =  Execute Mongoose Scenario  ${DATA_DIR}  ${env_params}  ${MONGOOSE_SHARED_ARGS} ${args} --storage-auth-secret=${SWIFT_SECRET_KEY}
    Log  ${std_out}
    Validate Log File Metrics Total  ${LOG_DIR}/${step_id}  count_succ_min=${object_count_limit}
    ...  count_succ_max=${object_count_limit}  transfer_size=${10240000}

Should Create Dynamic Large Objects
    ${step_id} =  Set Variable  create_dynamic_large_objects
    Remove Directory  ${LOG_DIR}/${step_id}  recursive=True
    Remove File  ${DATA_DIR}/${step_id}.csv
    ${args} =  Catenate  SEPARATOR= \\\n\t
    ...  --item-data-ranges-threshold=16MB
    ...  --item-data-size=20MB-100MB
    ...  --item-output-file=${MONGOOSE_CONTAINER_DATA_DIR}/${step_id}.csv
    ...  --item-output-path=dlo
    ...  --load-batch-size=1
    ...  --load-step-limit-size=2GB
    ...  --load-step-id=${step_id}
    ...  --storage-driver-limit-concurrency=10
    &{env_params} =  Create Dictionary
    ${std_out} =  Execute Mongoose Scenario  ${DATA_DIR}  ${env_params}  ${MONGOOSE_SHARED_ARGS} ${args} --storage-auth-secret=${SWIFT_SECRET_KEY}
    Log  ${std_out}
    Validate Log File Metrics Total  ${LOG_DIR}/${step_id}  count_succ_min=${10}  count_succ_max=${100}
    ...  count_fail_max=${10}  transfer_size=${2147483648}  transfer_size_delta=${167772160}
    Validate Item Output File  item_output_file_name=${DATA_DIR}/${step_id}.csv  item_output_path=dlo
    ...  item_size_min=${20971520}  item_size_max=${104857600}

Should Read Multiple Random Byte Ranges
    ${step_id} =  Set Variable  read_multiple_random_byte_ranges
    ${object_count_limit} =  Convert To Integer  1000
    ${random_byte_range_count} =  Convert To Integer  10
    Remove Directory  ${LOG_DIR}/${step_id}  recursive=True
    Remove File  ${DATA_DIR}/${step_id}.csv
    ${args} =  Catenate  SEPARATOR= \\\n\t
    ...  --item-data-size=100KB
    ...  --load-op-limit-count=${object_count_limit}
    ...  --load-step-id=${step_id}
    ...  --run-scenario=${MONGOOSE_CONTAINER_DATA_DIR}/${step_id}.js
    ...  --storage-driver-limit-concurrency=10
    &{env_params} =  Create Dictionary  ITEM_LIST_FILE=${MONGOOSE_CONTAINER_DATA_DIR}/${step_id}.csv  RANDOM_BYTE_RANGE_COUNT=${random_byte_range_count}
    ${std_out} =  Execute Mongoose Scenario  ${DATA_DIR}  ${env_params}  ${MONGOOSE_SHARED_ARGS} ${args} --storage-auth-secret=${SWIFT_SECRET_KEY}
    Log  ${std_out}
    Validate Log File Metrics Total  ${LOG_DIR}/${step_id}  op_type=READ  count_succ_min=${object_count_limit}
    ...  count_succ_max=${object_count_limit}  transfer_size=${60000000}  transfer_size_delta=${10000000}

Should Create Auth Tokens
    ${step_id} =  Set Variable  create_auth_tokens
    ${token_count_limit} =  Convert To Integer  1000
    Remove Directory  ${LOG_DIR}/${step_id}  recursive=True
    ${args} =  Catenate  SEPARATOR= \\\n\t
    ...  --item-type=token
    ...  --load-op-limit-count=${token_count_limit}
    ...  --load-step-id=${step_id}
    ...  --storage-driver-limit-concurrency=10
    &{env_params} =  Create Dictionary
    ${std_out} =  Execute Mongoose Scenario  ${DATA_DIR}  ${env_params}  ${MONGOOSE_SHARED_ARGS} ${args} --storage-auth-secret=${SWIFT_SECRET_KEY}
    Log  ${std_out}
    Validate Log File Metrics Total  ${LOG_DIR}/${step_id}  count_succ_min=${token_count_limit}
    ...  count_succ_max=${token_count_limit}

*** Keywords ***
Start Swift Server
    ${cmd} =  Catenate  SEPARATOR= \\\n\t
    ...  docker run
    ...  --detach
    ...  --name ${SWIFT_STORAGE_CONTAINER_NAME}
    ...  --publish ${SWIFT_PORT}:${SWIFT_PORT}
    ...  ${SWIFT_IMAGE_NAME}:${SWIFT_IMAGE_VERSION}
    Run  ${cmd}
    Sleep  5  Wait 5 sec until Swift server generates passwords...
    ${std_out} =  Run  docker logs ${SWIFT_STORAGE_CONTAINER_NAME}
    ${lines} =  Get Lines Containing String  ${std_out}  user_test_tester =
    Log  ${lines}
    ${tokens} =  Split String  ${lines}  ${SPACE}
    ${swift_secret_key} =  Get From List  ${tokens}  2
    Log  ${swift_secret_key}
    Set Test Variable  ${SWIFT_SECRET_KEY}  ${swift_secret_key}

Remove Swift Server
    Run  docker stop ${SWIFT_STORAGE_CONTAINER_NAME}
    Run  docker rm ${SWIFT_STORAGE_CONTAINER_NAME}

Remove Containers
    Remove Swift Server
    Remove Mongoose Container
