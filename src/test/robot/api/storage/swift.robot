*** Settings ***
Documentation  Mongoose Swift Storage Driver Tests
Force Tags  S3
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
${MONGOOSE_SHARED_ARGS} =  --storage-driver-type=swift --storage-net-node-port=${SWIFT_PORT} --storage-auth-uid=${SWIFT_UID} --storage-auth-secret=${SWIFT_SECRET_KEY}

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
