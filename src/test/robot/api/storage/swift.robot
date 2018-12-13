*** Settings ***
Documentation  Mongoose Swift Storage Driver Tests
Force Tags  Swift
Library  Collections
Library  OperatingSystem
Library  Validation
Resource  MongooseContainer.robot
Test Setup  Start Swift Server
Test Teardown  Remove Containers

*** Variables ***
${SWIFT_IMAGE_NAME} =  morrisjobke/docker-swift-onlyone
${SWIFT_IMAGE_VERSION} =  latest
${SWIFT_PORT} =  8080
${SWIFT_UID} =  test:tester
${SWIFT_SECRET_KEY} =  testing
${SWIFT_STORAGE_CONTAINER_NAME} =  swift_server
${MONGOOSE_SHARED_ARGS} =  --storage-driver-type=swift --storage-namespace=ns1 --storage-net-node-port=${SWIFT_PORT} --storage-auth-uid=${SWIFT_UID} --storage-auth-secret=${SWIFT_SECRET_KEY}

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
    ${std_out} =  Execute Mongoose Scenario  ${DATA_DIR}  ${env_params}  ${MONGOOSE_SHARED_ARGS} ${args}
    Log  ${std_out}
    Validate Log File Metrics Total  ${LOG_DIR}/${step_id}  count_succ_min=${object_count_limit}
    ...  count_succ_max=${object_count_limit}  count_fail_max=${0}  transfer_size=${10240000}

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
    ${std_out} =  Execute Mongoose Scenario  ${DATA_DIR}  ${env_params}  ${MONGOOSE_SHARED_ARGS} ${args}
    Log  ${std_out}
    Validate Log File Metrics Total  ${LOG_DIR}/${step_id}  count_succ_min=${10}  count_succ_max=${100}
    ...  count_fail_max=${10}  transfer_size=${2147483648}  transfer_size_delta=${167772160}
    Validate Item Output File  item_output_file_name=${DATA_DIR}/${step_id}.csv  item_output_path=mpu
    ...  item_size_min=${20971520}  item_size_max=${104857600}

*** Keywords ***
Start Swift Server
    ${cmd} =  Catenate  SEPARATOR= \\\n\t
    ...  docker run
    ...  --detach
    ...  --name ${SWIFT_STORAGE_CONTAINER_NAME}
    ...  --publish ${SWIFT_PORT}:${SWIFT_PORT}
    ...  ${SWIFT_IMAGE_NAME}:${SWIFT_IMAGE_VERSION}
    ${std_out} =  Run  ${cmd}
    Log  ${std_out}

Remove Swift Server
    Run  docker stop ${SWIFT_STORAGE_CONTAINER_NAME}
    Run  docker rm ${SWIFT_STORAGE_CONTAINER_NAME}

Remove Containers
    Remove Swift Server
    Remove Mongoose Container
