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
${LOGGER_NAME}  com.emc.mongoose.logging.Messages
${MONGOOSE_LOGS_URI_PATH}  logs/${STEP_ID}

*** Test Cases ***
Errors test
	Should Be Equal As Strings  ${DATA_DIR}  ${DATA_DIR}

Messages test
	${uri_path} =   Catenate    ${MONGOOSE_LOGS_URI_PATH}/${MESS_LOGGER_NAME}
	${resp}   Get Request  mongoose_node  ${uri_path}
	Log  ${resp}
	Should Be Equal As Strings  ${DATA_DIR}  ${DATA_DIR}

OpTrace test
	Should Be Equal As Strings  ${DATA_DIR}  ${DATA_DIR}

*** Keywords ***
#Should Return Logs
#    [Arguments]  ${expected_json_file_name}  ${uri_path}
#    ${file_content} =  Get File  ${expected_json_file_name}
#    ${expected_json_data} =  To Json  ${file_content}
#    ${resp} =  Get Request  mongoose_node  ${uri_path}
#    Should Be Equal As Strings  ${resp.status_code}  200
#    Should Be Equal  ${expected_json_data}  ${resp.json()}
