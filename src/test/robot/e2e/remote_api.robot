*** Settings ***
Documentation  End-to-end tests for the Mongoose Remote API
Library  OperatingSystem
Library  RequestsLibrary

*** Variables ***
${MONGOOSE_IMAGE_NAME} =  emcmongoose/mongoose
${MONGOOSE_IMAGE_VERSION} =  testing
${MONGOOSE_REMOTE_API_DEFAULT_PORT} =  9999
${MONGOOSE_CONFIG_API_PATH} =  /config

*** Test Cases ***
Config Defaults
    [Tags]  DEBUG
    Run Mongoose Node
    ${defaults} =  Query Defaults
    Validate Defaults  ${defaults}

*** Keywords ***
Run Mongoose Node
    Run Mongoose Container

Run Mongoose Container
    Run Docker Container  ${MONGOOSE_IMAGE_NAME}  --run-node  ${MONGOOSE_IMAGE_VERSION}

Run Docker Container
    [Arguments]  ${image_name}  ${container_args}  ${image_version}=latest
    Run  docker run --network host ${image_name}:${image_version} ${container_args}

Query Defaults
    ${std_out} =  Get Request  uri=http://localhost:${MONGOOSE_REMOTE_API_DEFAULT_PORT}/${MONGOOSE_CONFIG_API_PATH}
    [Return]  ${std_out}

Validate Defaults
    [Arguments]  ${defaults}
    Run  echo ${defaults}
