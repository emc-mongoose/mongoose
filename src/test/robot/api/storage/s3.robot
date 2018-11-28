*** Settings ***
Documentation  Mongoose S3 Storage Driver Tests
Force Tags  S3
Library  OperatingSystem
Test Setup  Start S3 Server
Test Teardown  Remove Containers

*** Variables ***
${DATA_DIR} =  src/test/robot/api/storage/data
${LOG_DIR} =  build/log
${S3_IMAGE_NAME} =  scality/s3server
${S3_IMAGE_VERSION} =  latest
${S3_PORT} =  8000
${S3_UID} =  user0
${S3_SECRET_KEY} =  secretKey0
${MONGOOSE_CONTAINER_DATA_DIR} =  /data
${MONGOOSE_IMAGE_NAME} =  emcmongoose/mongoose
${MONGOOSE_SHARED_ARGS} =  --storage-driver-type=s3 --storage-net-node-port=${S3_PORT} --storage-auth-uid=${S3_UID} --storage-auth-secret=${S3_SECRET_KEY}

*** Test Cases ***
Should Copy Objects Using Bucket Listing
    &{env_params} =  Create Dictionary  ITEM_SRC_PATH=/bucket0  ITEM_DST_PATH=/bucket1
    ${step_id} =  Set Variable  copy_objects_using_bucket_listing
    ${args} =  Catenate  SEPARATOR= \\\n\t
    ...  --item-data-size=10KB
    ...  --load-op-limit-count=100
    ...  --load-step-id=${step_id}
    ...  --storage-driver-limit-concurrency=10
    ...  --run-scenario=${MONGOOSE_CONTAINER_DATA_DIR}/copy_using_input_path.js
    ${std_out} =  Execute Mongoose Scenario  ${env_params}  ${args}
    # TODO validate stdout
    Log  ${std_out}
    # TODO validate log files
    ${metricsTotalContent} =  Get File  ${LOG_DIR}/metrics.total.csv
    Log  ${metricsTotalContent}

*** Keywords ***
Start S3 Server
    ${cmd} =  Catenate  SEPARATOR= \\\n\t
    ...  docker run
    ...  --detach
    ...  --name s3server
    ...  --publish ${S3_PORT}:${S3_PORT}
    ...  --env SCALITY_ACCESS_KEY_ID=${S3_UID}
    ...  --env SCALITY_SECRET_ACCESS_KEY=${S3_SECRET_KEY}
    ...  ${S3_IMAGE_NAME}:${S3_IMAGE_VERSION}
    ${std_out} =  Run  ${cmd}
    Log  ${std_out}

Remove S3 Server
    Run  docker stop s3server
    Run  docker rm s3server

Execute Mongoose Scenario
    [Arguments]  ${env}  ${args}
    ${docker_env_vars} =  Evaluate  ' '.join(['-e %s=%s' % (key, value) for (key, value) in ${env}.items()])
    ${host_working_dir} =  Get Environment Variable  HOST_WORKING_DIR
    ${version} =  Get Environment Variable  MONGOOSE_VERSION
    ${cmd} =  Catenate  SEPARATOR= \\\n\t
    ...  docker run
    ...  --name mongoose
    ...  --network host
    ...  ${docker_env_vars}
    ...  --volume ${host_working_dir}/${DATA_DIR}:${MONGOOSE_CONTAINER_DATA_DIR}
    ...  --volume ${host_working_dir}/${LOG_DIR}:/root/.mongoose/${version}/log
    ...  ${MONGOOSE_IMAGE_NAME}:${version}
    ...  ${MONGOOSE_SHARED_ARGS} ${args}
    ${std_out} =  Run  ${cmd}
    [Return]  ${std_out}

Remove Mongoose Container
    ${std_out} =  Run  docker rm mongoose
    Log  ${std_out}

Remove Containers
    Remove S3 Server
    Remove Mongoose Container
