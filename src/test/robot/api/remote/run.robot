*** Settings ***
Documentation  Mongoose Run API tests
Force Tags  Run
Library  Collections
Library  OperatingSystem
Library  RequestsLibrary

*** Variables ***
${DATA_DIR} =  src/test/robot/api/remote/data
${HEADER_ETAG} =  ETag
${HEADER_IF_MATCH} =  If-Match
${MONGOOSE_RUN_URI_PATH}=  /run

*** Test Cases ***
Should Start Scenario
    ${resp_start} =  Start Mongoose Scenario  ${DATA_DIR}/aggregated_defaults.json  ${DATA_DIR}/scenario_dummy.js
    Should Be Equal As Strings  ${resp_start.status_code}  202
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
    ${resp_stop} =  Stop Mongoose Scenario Run  ${resp_etag_header}

Should Not Start Scenario With Invalid Defaults
    ${resp_start} =  Start Mongoose Scenario  ${DATA_DIR}/aggregated_defaults_invalid.json  ${DATA_DIR}/scenario_dummy.js
    Should Be Equal As Strings  ${resp_start.status_code}  400

Should Stop Running Scenario
    ${resp_start} =  Start Mongoose Scenario  ${DATA_DIR}/aggregated_defaults.json  ${DATA_DIR}/scenario_dummy.js
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
    ${resp_stop} =  Stop Mongoose Scenario Run  ${resp_etag_header}
    Should Be Equal As Strings  ${resp_stop.status_code}  200

Should Not Stop Not Running Scenario
    ${resp_start} =  Start Mongoose Scenario  ${DATA_DIR}/aggregated_defaults.json  ${DATA_DIR}/scenario_dummy.js
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
    ${resp_stop_running} =  Stop Mongoose Scenario Run  ${resp_etag_header}
    Should Be Equal As Strings  ${resp_stop_running.status_code}  200
    ${resp_stop_stopped} =  Stop Mongoose Scenario Run  ${resp_etag_header}
    Should Be Equal As Strings  ${resp_stop_stopped.status_code}  204

Should Return The Node State
    ${resp_start} =  Start Mongoose Scenario  ${DATA_DIR}/aggregated_defaults.json  ${DATA_DIR}/scenario_dummy.js
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
    ${resp_status_running} =  Get Mongoose Node Status
    Should Be Equal As Strings  ${resp_status_running.status_code}  200
    ${resp_status_etag_header} =  Get From Dictionary  ${resp_status_running.headers}  ${HEADER_ETAG}
    Should Be Equal As Strings  ${resp_etag_header}  ${resp_status_etag_header}
    ${resp_stop} =  Stop Mongoose Scenario Run  ${resp_etag_header}
    Should Be Equal As Strings  ${resp_stop.status_code}  200
    ${resp_status_stopped} =  Get Mongoose Node Status
    Should Be Equal As Strings  ${resp_status_stopped.status_code}  204

Should Return Scenario Run State
    ${resp_start} =  Start Mongoose Scenario  ${DATA_DIR}/aggregated_defaults.json  ${DATA_DIR}/scenario_dummy.js
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
    Should Return Mongoose Scenario Run State  ${resp_etag_header}  200
    ${resp_stop} =  Stop Mongoose Scenario Run  ${resp_etag_header}
    Wait Until Keyword Succeeds  10x  1s  Should Return Mongoose Scenario Run State  ${resp_etag_header}  204

*** Keywords ***
Start Mongoose Scenario
    [Arguments]  ${defaults_file_name}  ${scenario_file_name}
    ${defaults_data} =  Get Binary File  ${defaults_file_name}
    ${scenario_data} =  Get Binary File  ${scenario_file_name}
    &{data} =  Create Dictionary  defaults=${defaults_data}  scenario=${scenario_data}
    ${resp} =  Post Request  mongoose_node  ${MONGOOSE_RUN_URI_PATH}  files=${data}
    Log  ${resp.status_code}
    [Return]  ${resp}

Should Return Mongoose Scenario Run State
    [Arguments]  ${etag}  ${expected_status_code}
    ${resp_state} =  Get Mongoose Scenario Run State  ${etag}
    Should Be Equal As Strings  ${resp_state.status_code}  ${expected_status_code}

Get Mongoose Node Status
    ${resp} =  Head Request  mongoose_node  ${MONGOOSE_RUN_URI_PATH}
    Log  ${resp.status_code}
	[Return]  ${resp}

Get Mongoose Scenario Run State
    [Arguments]  ${etag}
    &{req_headers} =  Create Dictionary  If-Match=${etag}
    ${resp} =  Get Request  mongoose_node  ${MONGOOSE_RUN_URI_PATH}  headers=${req_headers}
    Log  ${resp.status_code}
    [Return]  ${resp}

Stop Mongoose Scenario Run
    [Arguments]  ${etag}
    &{req_headers} =  Create Dictionary  If-Match=${etag}
    ${resp} =  Delete Request  mongoose_node  ${MONGOOSE_RUN_URI_PATH}  headers=${req_headers}
    Log  ${resp.status_code}
    [Return]  ${resp}
