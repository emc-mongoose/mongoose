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
    ...  docker run -d --network host
    ...  --name mongoose_node
    ...  ${MONGOOSE_IMAGE_NAME}:${image_version}
    ...  --run-node --load-step-id=robotest
    ${std_out} =  Run  ${cmd}
    Log  ${std_out}
    ${remove_it} =  Run  docker ps --no-trunc
    Log  ${remove_it}
    Create Session  mongoose_node  http://localhost:${MONGOOSE_NODE_PORT}  debug=1  timeout=1000  max_retries=10

Remove Mongoose Node
    Delete All Sessions
    Run  docker stop mongoose_node
    ${std_out} =  Run  docker logs mongoose_node
    Log  ${std_out}
    Run  docker rm mongoose_node
