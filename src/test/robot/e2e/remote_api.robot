*** Settings ***
Documentation  End-to-end tests for the Mongoose Remote API
Library  OperatingSystem
Library  RequestsLibrary
Test Setup  Start Mongoose Node
Test Teardown  Remove Mongoose Node

*** Variables ***
${MONGOOSE_IMAGE_NAME} =  emcmongoose/mongoose
${MONGOOSE_IMAGE_VERSION} =  testing
${MONGOOSE_CONFIG_API_PATH} =  /config
${MONGOOSE_NODE_PORT} =  9999

*** Test Cases ***
Config Defaults
    [Tags]  DEBUG
    ${defaults} =  Query Defaults
    Validate Defaults  ${defaults}

*** Keywords ***
Start Mongoose Node
    ${std_out} =  Run  docker run -d --name mongoose_node --network host ${MONGOOSE_IMAGE_NAME}:${MONGOOSE_IMAGE_VERSION} --run-node
    Log  ${std_out}

Remove Mongoose Node
    Run  docker stop mongoose_node
    Run  docker rm mongoose_node

Query Defaults
    Create Session  local_mongoose_node  http://localhost:${MONGOOSE_NODE_PORT}  debug=1  timeout=1000  max_retries=10
    ${resp} =  Get Request  local_mongoose_node  ${MONGOOSE_CONFIG_API_PATH}
    Should Be Equal As Strings  ${resp.status_code}  200
    Log  ${resp.json()}

Validate Defaults
    [Arguments]  ${defaults}
    Run  echo ${defaults}
