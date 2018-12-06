*** Settings ***
Documentation  Mongoose S3 Storage Driver Tests
Force Tags  S3
Library  Collections
Library  CSVLibrary
Library  OperatingSystem
Library  Validation
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
    Log  ${std_out}
    Validate Log File Metrics Total  ${LOG_DIR}/${step_id}  count_succ_min=1000  count_succ_max=1000  count_fail_max=0
    ...  transfer_size=10240000

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
    ...  --load-step-limit-size=1GB
    ...  --load-step-id=${step_id}
    ...  --storage-driver-limit-concurrency=10
    &{env_params} =  Create Dictionary
    ${std_out} =  Execute Mongoose Scenario  ${env_params}  ${args}
    Log  ${std_out}
    Validate Log File Metrics Total  ${LOG_DIR}/${step_id}  count_succ_min=11  count_succ_max=51  count_fail_max=0
    ...  transfer_size=1073741824  transfer_size_delta=10240000
    Validate Item Output File  item_output_file_name=${DATA_DIR}/${step_id}.csv  item_output_path=mpu
    ...  item_size_min=20971520  item_size_max=104857600

Should Create Objects Using Multiple Buckets And Users
    Pass Execution  "TODO"

Should Read Multiple Random Byte Ranges
    Pass Execution  "TODO"

Should Update Multiple Random Byte Ranges
    Pass Execution  "TODO"

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
    ${mongoose_version} =  Get Environment Variable  MONGOOSE_VERSION
    ${image_version} =  Get Environment Variable  MONGOOSE_IMAGE_VERSION
    ${cmd} =  Catenate  SEPARATOR= \\\n\t
    ...  docker run
    ...  --name mongoose
    ...  --network host
    ...  ${docker_env_vars}
    ...  --volume ${host_working_dir}/${DATA_DIR}:${MONGOOSE_CONTAINER_DATA_DIR}
    ...  --volume ${host_working_dir}/${LOG_DIR}:/root/.mongoose/${mongoose_version}/log
    ...  ${MONGOOSE_IMAGE_NAME}:${image_version}
    ...  ${MONGOOSE_SHARED_ARGS} ${args}
    ${std_out} =  Run  ${cmd}
    [Return]  ${std_out}

Remove Mongoose Container
    ${std_out} =  Run  docker rm mongoose
    Log  ${std_out}

Remove Containers
    Remove S3 Server
    Remove Mongoose Container
