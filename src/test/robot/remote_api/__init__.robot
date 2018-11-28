*** Settings ***
Documentation  Mongoose remote API tests suite
Force Tags  Remote API
Library  OperatingSystem
Library  RequestsLibrary
Test Setup  Execute Mongoose Scenario

*** Variables ***
${MONGOOSE_IMAGE_NAME} =  emcmongoose/mongoose
${MONGOOSE_IMAGE_VERSION} =  testing
${MONGOOSE_NODE_PORT} =  9999

*** Keywords ***
Start Mongoose Node
    ${std_out} =  Run  docker run -d --name mongoose_node --network host ${MONGOOSE_IMAGE_NAME}:${MONGOOSE_IMAGE_VERSION} --run-node
    Log  ${std_out}
    Create Session  mongoose_node  http://localhost:${MONGOOSE_NODE_PORT}  debug=1  timeout=1000  max_retries=10

Remove Mongoose Node
    Delete All Sessions
    Run  docker stop mongoose_node
    ${std_out} =  Run  docker logs mongoose_node
    Log  ${std_out}
    Run  docker rm mongoose_node
