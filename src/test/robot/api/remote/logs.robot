*** Settings ***
Documentation  Mongoose Logs API tests
Force Tags  Logs
Library  Collections
Library  OperatingSystem
Library  RequestsLibrary
Library  String
Test Setup  SetUp

*** Variables ***
${DATA_DIR}  src/test/robot/api/remote/data
${STEP_ID}  robotest
${MESS_LOGGER_NAME}  Messages
${OP_TRACE_LOGGER_NAME}  OpTraces
${MONGOOSE_RUN_URI_PATH}  /run
${MONGOOSE_LOGS_URI_PATH}  /logs/${STEP_ID}

*** Test Cases ***
Messages test
	${uri_path} =  Catenate  ${MONGOOSE_LOGS_URI_PATH}/${MESS_LOGGER_NAME}
	Wait Until Keyword Succeeds  10x  2s  Should Return Status  ${uri_path}  200
	${resp} =  Get Request  mongoose_node  ${uri_path}
	Should Be Equal As Strings  ${resp.status_code}  200
	Should Have Lines  ${resp.text}

OpTrace test
	${uri_path} =  Catenate  ${MONGOOSE_LOGS_URI_PATH}/${OP_TRACE_LOGGER_NAME}
	Wait Until Keyword Succeeds  10x  2s  Start Mongoose Scenario  ${DATA_DIR}/aggregated_defaults.json  ${DATA_DIR}/scenario_dummy.js
    Wait Until Keyword Succeeds  10x  2s  Should Return Status  ${uri_path}  200
    ${resp} =  Get Request  mongoose_node  ${uri_path}
#    Should Be Equal As Strings  ${resp.status_code}  200
#    Should Have Lines  ${resp.text}

*** Keywords ***
SetUp
	Wait Until Keyword Succeeds  10x  2s  Start Mongoose Scenario  ${DATA_DIR}/aggregated_defaults.json  ${DATA_DIR}/scenario_dummy.js

Should Have Lines
	[Arguments]  ${result}
	${lines} =	Get Lines Matching Pattern	${result}	*| INFO |*
	${count} =  Get Line Count  ${lines}
    Should Be True  ${count}>0

Should Return Status
	[Arguments]  ${uri_path}  ${expected_status}
	${resp} =  Get Request  mongoose_node  ${uri_path}
	Should Be Equal As Strings  ${resp.status_code}  ${expected_status}

Start Mongoose Scenario
    [Arguments]  ${defaults_file_name}  ${scenario_file_name}
    ${defaults_data} =  Get Binary File  ${defaults_file_name}
    ${scenario_data} =  Get Binary File  ${scenario_file_name}
    &{data} =  Create Dictionary  defaults=${defaults_data}  scenario=${scenario_data}
    ${resp} =  Post Request  mongoose_node  ${MONGOOSE_RUN_URI_PATH}  files=${data}
    Log  ${resp.status_code}
    [Return]  ${resp}

#Should Return Logs
#    [Arguments]  ${expected_json_file_name}  ${uri_path}
#    ${file_content} =  Get File  ${expected_json_file_name}
#    ${expected_json_data} =  To Json  ${file_content}
#    ${resp} =  Get Request  mongoose_node  ${uri_path}
#    Should Be Equal As Strings  ${resp.status_code}  200
#    Should Be Equal  ${expected_json_data}  ${resp.json()}
