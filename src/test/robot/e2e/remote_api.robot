*** Settings ***
Documentation  End-to-end tests for the Mongoose Remote API
Library  OperatingSystem
Library  RequestsLibrary
Test Setup  Start Mongoose Node
Test Teardown  Stop Mongoose Node

*** Variables ***
${MONGOOSE_IMAGE_NAME} =  emcmongoose/mongoose
${MONGOOSE_IMAGE_VERSION} =  testing
${MONGOOSE_REMOTE_API_DEFAULT_PORT} =  9999
${MONGOOSE_CONFIG_API_PATH} =  /config

*** Test Cases ***
Config Defaults
    [Tags]  DEBUG
    ${defaults} =  Query Defaults
    Validate Defaults  ${defaults}

*** Keywords ***
Start Mongoose Node
    Run  docker run
    ...  --name=mongoose
    ...  -d
    ...  -p ${MONGOOSE_REMOTE_API_DEFAULT_PORT}:${MONGOOSE_REMOTE_API_DEFAULT_PORT}
    ...  ${MONGOOSE_IMAGE_NAME}:${MONGOOSE_IMAGE_VERSION}
    ...  --run-node

Stop Mongoose Node
    Run  docker stop
    ...  mongoose

Query Defaults
    Create Session  localhost  http://localhost:${MONGOOSE_REMOTE_API_DEFAULT_PORT}
    ...  debug=1
    ${std_out} =  Get Request  localhost  ${MONGOOSE_CONFIG_API_PATH}
    [Return]  ${std_out}

Validate Defaults
    [Arguments]  ${defaults}
    Run  echo ${defaults}
