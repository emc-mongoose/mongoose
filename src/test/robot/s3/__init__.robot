*** Settings ***
Documentation  Mongoose S3 Storage Driver Test Suite
Force Tags  Storage Driver  S3
Library  OperatingSystem
Suite Setup  Start S3 Server
Suite Teardown  Remove S3 Server

*** Variables ***
${MONGOOSE_IMAGE_NAME} =  emcmongoose/mongoose
${MONGOOSE_IMAGE_VERSION} =  testing
${S3_IMAGE_NAME} =  scality/s3server
${S3_IMAGE_VERSION} =  latest
${S3_PORT} =  8000
${S3_UID} =  user0
${S3_SECRET_KEY} =  secretKey0

*** Keywords ***
Start S3 Server
    ${std_out} =  Run  docker run -d --name s3server -p ${S3_PORT}:${S3_PORT} -e SCALITY_ACCESS_KEY_ID=${S3_UID} -e SCALITY_SECRET_ACCESS_KEY=${S3_SECRET_KEY} ${S3_IMAGE_NAME}:${S3_IMAGE_VERSION}
    Log  ${std_out}

Remove S3 Server
    Delete All Sessions
    Run  docker stop s3server
    Run  docker rm s3server

Start Mongoose Scenario
    [Arguments]  ${env}  ${args}

    ${std_out} =  Run  docker run -d --name mongoose ${docker_env_args} ${MONGOOSE_IMAGE_NAME}:${MONGOOSE_IMAGE_VERSION} ${args}
