*** Settings ***
Documentation  Mongoose Config API tests
Force Tags  Config
Library  OperatingSystem
Library  RequestsLibrary

*** Variables ***
${DATA_DIR} =  src/test/robot/api/remote/data
${MONGOOSE_CONFIG_URI_PATH}=  /config
${MONGOOSE_CONFIG_SCHEMA_URI_PATH}=  ${MONGOOSE_CONFIG_URI_PATH}/schema

*** Test Cases ***
Should Do Something
    ${resp} =  Get Request  s3server  /
    Should Be Equal As Strings  ${resp.status_code}  200

Should Return Aggregated Defaults
    Should Return Json  ${DATA_DIR}/aggregated_defaults.json  ${MONGOOSE_CONFIG_URI_PATH}

Should Return Aggregated Schema
    Should Return Json  ${DATA_DIR}/aggregated_defaults_schema.json  ${MONGOOSE_CONFIG_SCHEMA_URI_PATH}

*** Keywords ***
Should Return Json
    [Arguments]  ${expected_json_file_name}  ${uri_path}
    ${file_content} =  Get File  ${expected_json_file_name}
    ${expected_json_data} =  To Json  ${file_content}
    ${resp} =  Get Request  mongoose_node  ${uri_path}
    Should Be Equal As Strings  ${resp.status_code}  200
    Should Be Equal  ${expected_json_data}  ${resp.json()}
