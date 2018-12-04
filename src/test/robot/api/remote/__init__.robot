*** Settings ***
Documentation  Mongoose Remote API suite
Force Tags  Remote API
Library  OperatingSystem
Library  RequestsLibrary
Suite Setup  Start Mongoose Node
Suite Teardown  Remove Mongoose Node

*** Variables ***
${MONGOOSE_IMAGE_NAME} =  emcmongoose/mongoose
${MONGOOSE_NODE_PORT} =  9000
${S3_IMAGE_NAME} =  minio/minio
${S3_IMAGE_VERSION} =  latest
${S3_PORT} =  9999
${S3_UID} =  user1
${S3_SECRET_KEY} =  secretKey1
${S3_STORAGE_CONTAINER_NAME} =  s3storage

*** Keywords ***
Start Mongoose Node
    ${image_version} =  Get Environment Variable  MONGOOSE_IMAGE_VERSION
    ${cmd} =  Catenate  SEPARATOR= \\\n\t
    ...  docker run
    ...  --detach
    ...  --name mongoose_node
    ...  --publish ${MONGOOSE_NODE_PORT}:${MONGOOSE_NODE_PORT}
    ...  ${MONGOOSE_IMAGE_NAME}:${image_version}
    ...  --load-step-id=robotest --run-node
    ${std_out} =  Run  ${cmd}
    Log  ${std_out}
    Create Session  mongoose_node  http://127.0.0.1:${MONGOOSE_NODE_PORT}  debug=1  timeout=1000  max_retries=10
    ${cmd1} =  Catenate  SEPARATOR= \\\n\t
        ...  docker run
        ...  --detach
        ...  --name ${S3_STORAGE_CONTAINER_NAME}
        ...  --publish ${S3_PORT}:${S3_PORT}
        ...  --env MINIO_ACCESS_KEY=${S3_UID}
        ...  --env MINIO_SECRET_KEY=${S3_SECRET_KEY}
        ...  ${S3_IMAGE_NAME}:${S3_IMAGE_VERSION}
        ...  server /data
    ${std_out1} =  Run  ${cmd1}
    Log  ${std_out1}
    Create Session  s3server  http://127.0.0.1:${MONGOOSE_NODE_PORT}  debug=1  timeout=1000  max_retries=10

Remove Mongoose Node
    Delete All Sessions
    Run  docker stop mongoose_node
    ${std_out} =  Run  docker logs mongoose_node
    Log  ${std_out}
    Run  docker rm mongoose_node
    Run  docker stop s3server
    Run  docker rm s3server
