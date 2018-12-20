*** Settings ***
Documentation  Mongoose Logs API tests
Force Tags     Logs
Resource       Common.robot
Library        Collections
Library        OperatingSystem
Library        RequestsLibrary
Library        String
Test Setup     SetUp


*** Variables ***
${DATA_DIR}  src/test/robot/api/remote/data
${STEP_ID}  robotest
${MESS_LOGGER_NAME}  Messages
${OP_TRACE_LOGGER_NAME}  OpTraces
${MONGOOSE_RUN_URI_PATH}  /run
${MONGOOSE_LOGS_URI_PATH}  /logs/${STEP_ID}

*** Test Cases ***
Should Respond Message Logs
	${resp_start} =  Start Mongoose Scenario  ${DATA_DIR}/logs_test_defaults.json  ${DATA_DIR}/scenario_dummy.js
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
	#
    ${uri_path} =  Catenate  ${MONGOOSE_LOGS_URI_PATH}/${MESS_LOGGER_NAME}
    Wait Until Keyword Succeeds  10x  1s  Should Return Status  ${uri_path}  200
    ${resp} =  Get Request  mongoose_node  ${uri_path}
    Should Be Equal As Strings  ${resp.status_code}  200
    Should Have Lines  ${resp.text}  *| INFO |*
    #
    ${resp_stop} =  Stop Mongoose Scenario Run  ${resp_etag_header}

Should Respond Operation Trace Logs
	${resp_start} =  Start Mongoose Scenario  ${DATA_DIR}/logs_test_defaults.json  ${DATA_DIR}/scenario_dummy.js
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
	#
    ${uri_path} =  Catenate  ${MONGOOSE_LOGS_URI_PATH}/${OP_TRACE_LOGGER_NAME}
    Wait Until Keyword Succeeds  10x  1s  Should Return Status  ${uri_path}  200
    ${resp} =  Get Request  mongoose_node  ${uri_path}
    Should Be Equal As Strings  ${resp.status_code}  200
    Should Have Lines  ${resp.text}  *
    #
    ${resp_stop} =  Stop Mongoose Scenario Run  ${resp_etag_header}

Should Delete logs
	${resp_start} =  Start Mongoose Scenario  ${DATA_DIR}/logs_test_defaults.json  ${DATA_DIR}/scenario_dummy.js
    ${resp_etag_header} =  Get From Dictionary  ${resp_start.headers}  ${HEADER_ETAG}
	#
    ${uri_path} =  Catenate  ${MONGOOSE_LOGS_URI_PATH}/${MESS_LOGGER_NAME}
    Wait Until Keyword Succeeds  10x  1s  Should Return Status  ${uri_path}  200
    Delete Request  mongoose_node  ${uri_path}
    ${resp} =  Get Request  mongoose_node  ${uri_path}
    # The log file doesn't exist
    Should Be Equal As Strings  ${resp.status_code}  404
    #
    ${resp_stop} =  Stop Mongoose Scenario Run  ${resp_etag_header}

*** Keywords ***

Should Have Lines
    [Arguments]  ${result}  ${pattern}
    ${lines} =    Get Lines Matching Pattern    ${result}    ${pattern}
    ${count} =  Get Line Count  ${lines}
    Should Be True  ${count}>0

Should Return Status
    [Arguments]  ${uri_path}  ${expected_status}
    ${resp} =  Get Request  mongoose_node  ${uri_path}
    Should Be Equal As Strings  ${resp.status_code}  ${expected_status}

