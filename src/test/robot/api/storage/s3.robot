*** Settings ***
Documentation  Mongoose S3 Storage Driver Tests
Force Tags  S3
Library  Collections
Library  CSVLibrary
Library  OperatingSystem
Test Setup  Start S3 Server
Test Teardown  Remove Containers

*** Variables ***
${DATA_DIR} =  src/test/robot/api/storage/data
${LOG_DIR} =  build/log
${S3_IMAGE_NAME} =  minio/minio
${S3_IMAGE_VERSION} =  latest
${S3_PORT} =  9000
${S3_UID} =  user1
${S3_SECRET_KEY} =  secretKey1
${S3_STORAGE_CONTAINER_NAME} =  s3storage
${MONGOOSE_CONTAINER_DATA_DIR} =  /data
${MONGOOSE_IMAGE_NAME} =  emcmongoose/mongoose
${MONGOOSE_SHARED_ARGS} =  --storage-driver-type=s3 --storage-net-node-port=${S3_PORT} --storage-auth-uid=${S3_UID} --storage-auth-secret=${S3_SECRET_KEY}

*** Test Cases ***
Should Copy Objects Using Bucket Listing
    ${step_id} =  Set Variable  copy_objects_using_bucket_listing
    Remove Directory  ${LOG_DIR}/${step_id}  recursive=True
    ${args} =  Catenate  SEPARATOR= \\\n\t
    ...  --item-data-size=10KB
    ...  --load-op-limit-count=1000
    ...  --load-step-id=${step_id}
    ...  --storage-driver-limit-concurrency=10
    ...  --run-scenario=${MONGOOSE_CONTAINER_DATA_DIR}/copy_using_input_path.js
    &{env_params} =  Create Dictionary  ITEM_SRC_PATH=/bucket0  ITEM_DST_PATH=/bucket1
	${std_out} =  Execute Mongoose Scenario  ${env_params}  ${args}
    # TODO validate stdout
    Log  ${std_out}
    Validate Metrics Total Log File  ${step_id}  CREATE  1000  0  10240000

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

Validate Metrics Total Log File
    [Arguments]  ${step_id}  ${op_type}  ${count_succ}  ${count_fail}  ${transfer_size}
    @{metricsTotal} =  Read CSV File To Associative  ${LOG_DIR}/${step_id}/metrics.total.csv
    Should Be Equal As Strings  &{metricsTotal[0]}[OpType]  ${op_type}
    Should Be Equal As Strings  &{metricsTotal[0]}[CountSucc]  ${count_succ}
    Should Be Equal As Strings  &{metricsTotal[0]}[CountFail]  ${count_fail}
    Should Be Equal As Strings  &{metricsTotal[0]}[Size]  ${transfer_size}
