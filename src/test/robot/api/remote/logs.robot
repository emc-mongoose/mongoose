*** Settings ***
Documentation  Mongoose Logs API tests
Force Tags  Logs
Library  Collections
Library  OperatingSystem
Library  RequestsLibrary

*** Variables ***
${DATA_DIR}  src/test/robot/api/remote/data
${STEP_ID}  123
${MESS_LOGGER_NAME}  Messages
${LOGGER_NAME}  Messages
${MONGOOSE_LOGS_URI_PATH}  /logs/${STEP_ID}

*** Test Cases ***
Errors test
	Should Be Equal As Strings  ${DATA_DIR}  ${DATA_DIR}

Messages test
	${uri_path} =   Catenate    ${MONGOOSE_LOGS_URI_PATH}/${MESS_LOGGER_NAME}
	Wait Until Keyword Succeeds  10x  1s  Should Return Status  ${uri_path}  200
	${resp} =  Get Request  mongoose_node  ${uri_path}
	Should Be Equal As Strings  ${resp.status_code}  200
#	Should Have Lines  ${resp.body}

OpTrace test
	Should Be Equal As Strings  ${DATA_DIR}  ${DATA_DIR}

*** Keywords ***
Should Have Lines
	[Arguments]  ${result}
	${lines} =	Get Lines Matching Pattern	${result}	*
	[Return]  ${lines}

Should Return Status
	[Arguments]  ${uri_path}  ${expected_status}
	${resp} =  Get Request  mongoose_node  ${uri_path}
	Should Be Equal As Strings  ${resp.status_code}  ${expected_status}

#Should Return Logs
#    [Arguments]  ${expected_json_file_name}  ${uri_path}
#    ${file_content} =  Get File  ${expected_json_file_name}
#    ${expected_json_data} =  To Json  ${file_content}
#    ${resp} =  Get Request  mongoose_node  ${uri_path}
#    Should Be Equal As Strings  ${resp.status_code}  200
#    Should Be Equal  ${expected_json_data}  ${resp.json()}
