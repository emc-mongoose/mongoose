*** Settings ***
Documentation  End-to-end tests for the Mongoose Remote API
Library  Collections
Library  OperatingSystem
Library  RequestsLibrary
Test Setup  Start Mongoose Node
Test Teardown  Remove Mongoose Node

*** Variables ***
${HEADER_ETAG} =  ETag
${HEADER_IF_MATCH} =  If-Match
${DATA_DIR} =  src/test/robot/data
${MONGOOSE_CONFIG_SCHEMA_URI_PATH}=  ${MONGOOSE_CONFIG_URI_PATH}/schema
${MONGOOSE_CONFIG_URI_PATH}=  /config
${MONGOOSE_IMAGE_NAME} =  emcmongoose/mongoose
${MONGOOSE_IMAGE_VERSION} =  testing
${MONGOOSE_NODE_PORT} =  9999
${MONGOOSE_RUN_URI_PATH}=  /run

*** Test Cases ***
Should Return Aggregated Defaults
    Should Return Json  ${DATA_DIR}/aggregated_defaults.json  ${MONGOOSE_CONFIG_URI_PATH}

Should Return Aggregated Schema
    Should Return Json  ${DATA_DIR}/aggregated_defaults_schema.json  ${MONGOOSE_CONFIG_SCHEMA_URI_PATH}

Should Start Scenario
    ${resp_start} =  Start Mongoose Scenario  ${DATA_DIR}/scenario_dummy.js
    Should Be Equal As Strings  ${resp_start.status_code}  202
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
    ${resp_stop} =  Stop Mongoose Scenario Run  ${resp_etag_header}

Should Stop Scenario
    ${resp_start} =  Start Mongoose Scenario  ${DATA_DIR}/scenario_dummy.js
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
    ${resp_stop} =  Stop Mongoose Scenario Run  ${resp_etag_header}
    Should Be Equal As Strings  ${resp_stop.status_code}  200

Should Return Scenario Run State
    ${resp_start} =  Start Mongoose Scenario  ${DATA_DIR}/scenario_dummy.js
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
    ${resp_state_running} =  Get Mongoose Scenario Run State  ${resp_etag_header}
    Should Be Equal As Strings  ${resp_state_running.status_code}  200
    ${resp_stop} =  Stop Mongoose Scenario Run  ${resp_etag_header}
    ${resp_state_stopped} =  Get Mongoose Scenario Run State  ${resp_etag_header}
    Should Be Equal As Strings  ${resp_state_running.status_code}  204

*** Keywords ***
Should Return Json
    [Arguments]  ${expected_json_file_name}  ${uri_path}
    ${file_content} =  Get File  ${expected_json_file_name}
    ${expected_json_data} =  To Json  ${file_content}
    ${resp} =  Get Request  mongoose_node  ${uri_path}
    Should Be Equal As Strings  ${resp.status_code}  200
    Log  ${resp.json()}
    Should Be Equal  ${expected_json_data}  ${resp.json()}

Start Mongoose Node
    ${std_out} =  Run  docker run -d --name mongoose_node --network host ${MONGOOSE_IMAGE_NAME}:${MONGOOSE_IMAGE_VERSION} --run-node
    Log  ${std_out}
    Create Session  mongoose_node  http://localhost:${MONGOOSE_NODE_PORT}  debug=1  timeout=1000  max_retries=10

Remove Mongoose Node
    Delete All Sessions
    Run  docker stop mongoose_node
    Run  docker rm mongoose_node

Start Mongoose Scenario
    [Arguments]  ${scenario_file_name}
    ${defaults_data} =  Get File  ${DATA_DIR}/aggregated_defaults.json
    ${scenario_data} =  Get File  ${scenario_file_name}
    &{data} =  Create Dictionary  defaults=${defaults_data}  scenario=${scenario_data}
    ${resp} =  Post Request  mongoose_node  ${MONGOOSE_RUN_URI_PATH}  data=${data}
    Log  ${resp.status_code}
    [Return]  ${resp}

Get Mongoose Scenario Run State
    [Arguments]  ${etag}
    &{req_headers} =  Create Dictionary  If-Match=${etag}
    ${resp} =  Head Request  mongoose_node  ${MONGOOSE_RUN_URI_PATH}  headers=${req_headers}
    Log  ${resp.status_code}
    [Return]  ${resp}

Stop Mongoose Scenario Run
    [Arguments]  ${etag}
    &{req_headers} =  Create Dictionary  If-Match=${etag}
    ${resp} =  Delete Request  mongoose_node  ${MONGOOSE_RUN_URI_PATH}  headers=${req_headers}
    Log  ${resp.status_code}
    [Return]  ${resp}
