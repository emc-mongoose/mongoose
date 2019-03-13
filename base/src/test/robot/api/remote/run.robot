*** Settings ***
Documentation  Mongoose Run API tests
Force Tags  Run
Resource   Common.robot
Library  Collections
Library  OperatingSystem
Library  RequestsLibrary

*** Variables ***
${HEADER_IF_MATCH} =  If-Match

*** Test Cases ***
Should Start Scenario
    ${data} =  Make Start Request Payload Full
    ${resp_start} =  Start Mongoose Scenario  ${data}
    Should Be Equal As Strings  ${resp_start.status_code}  202
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
    ${resp_stop} =  Stop Mongoose Scenario Run  ${resp_etag_header}

Should Start With Implicit Default Scenario
    ${data} =  Make Start Request Payload Without Scenario Part
    ${resp_start} =  Start Mongoose Scenario  ${data}
    Should Be Equal As Strings  ${resp_start.status_code}  202
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
    ${resp_stop} =  Stop Mongoose Scenario Run  ${resp_etag_header}

Should Start With Implicit Default Scenario And Partial Config
    ${data} =  Make Start Request Payload With Partial Config
    ${resp_start} =  Start Mongoose Scenario  ${data}
    Should Be Equal As Strings  ${resp_start.status_code}  202
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
    ${resp_stop} =  Stop Mongoose Scenario Run  ${resp_etag_header}

Should Stop Running Scenario
    ${data} =  Make Start Request Payload Full
    ${resp_start} =  Start Mongoose Scenario  ${data}
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
    ${resp_stop} =  Stop Mongoose Scenario Run  ${resp_etag_header}
    Should Be Equal As Strings  ${resp_stop.status_code}  200

Should Not Stop Not Running Scenario
    ${data} =  Make Start Request Payload Full
    ${resp_start} =  Start Mongoose Scenario  ${data}
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
    ${resp_stop_running} =  Stop Mongoose Scenario Run  ${resp_etag_header}
    Should Be Equal As Strings  ${resp_stop_running.status_code}  200
    ${resp_stop_stopped} =  Stop Mongoose Scenario Run  ${resp_etag_header}
    Should Be Equal As Strings  ${resp_stop_stopped.status_code}  204

Should Not Start Scenario With Invalid Defaults
    ${data} =  Make Start Request Payload Invalid
    ${resp_start} =  Start Mongoose Scenario  ${data}
    Should Be Equal As Strings  ${resp_start.status_code}  400

Should Return The Node State
    ${data} =  Make Start Request Payload Full
    ${resp_start} =  Start Mongoose Scenario  ${data}
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
    ${data} =  Make Start Request Payload Full
    ${resp_start} =  Start Mongoose Scenario  ${data}
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
    Should Return Mongoose Scenario Run State  ${resp_etag_header}  200
    ${resp_stop} =  Stop Mongoose Scenario Run  ${resp_etag_header}
    Wait Until Keyword Succeeds  10x  1s  Should Return Mongoose Scenario Run State  ${resp_etag_header}  204

*** Keywords ***
Make Start Request Payload Full
    ${defaults_data} =  Get Binary File  ${DATA_DIR}/aggregated_defaults.yaml
    ${scenario_data} =  Get Binary File  ${DATA_DIR}/scenario_dummy.js
    &{data} =  Create Dictionary  defaults=${defaults_data}  scenario=${scenario_data}
    [Return]  ${data}

Make Start Request Payload Invalid
    ${defaults_data} =  Get Binary File  ${DATA_DIR}/aggregated_defaults_invalid.yaml
    ${scenario_data} =  Get Binary File  ${DATA_DIR}/scenario_dummy.js
    &{data} =  Create Dictionary  defaults=${defaults_data}  scenario=${scenario_data}
    [Return]  ${data}

Make Start Request Payload Without Scenario Part
    ${defaults_data} =  Get Binary File  ${DATA_DIR}/aggregated_defaults.yaml
    &{data} =  Create Dictionary  defaults=${defaults_data}
    [Return]  ${data}

Make Start Request Payload With Partial Config
    ${defaults_data} =  Get Binary File  ${DATA_DIR}/partial_defaults.yaml
    &{data} =  Create Dictionary  defaults=${defaults_data}
    [Return]  ${data}

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

