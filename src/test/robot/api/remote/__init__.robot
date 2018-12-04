*** Settings ***
Documentation  Mongoose Remote API suite
Force Tags  Remote API
Library  OperatingSystem
Library  RequestsLibrary
Suite Setup  Start Mongoose Node
Suite Teardown  Remove Mongoose Node

*** Variables ***
${MONGOOSE_IMAGE_NAME} =  emcmongoose/mongoose
${MONGOOSE_NODE_PORT} =  9999

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
    Run  sleep 10
    Log  ${std_out}
    ${debug_0} =  Run  ps alx | grep java
    Log  ${debug_0|
    ${debug_1} =  Run  sleep 1 | telnet 127.0.0.1 9999
    Log  ${debug_1|
    Create Session  mongoose_node  http://127.0.0.1:${MONGOOSE_NODE_PORT}  debug=1  timeout=1000  max_retries=10

Remove Mongoose Node
    Delete All Sessions
    Run  docker stop mongoose_node
    ${std_out} =  Run  docker logs mongoose_node
    Log  ${std_out}
    Run  docker rm mongoose_node
