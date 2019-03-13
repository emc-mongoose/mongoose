*** Settings ***
Documentation  Mongoose Config API tests
Force Tags  Config
Library  OperatingSystem
Library  RequestsLibrary

*** Variables ***
${DATA_DIR} =  base/src/test/robot/api/remote/data
${MONGOOSE_CONFIG_URI_PATH}=  /config
${MONGOOSE_CONFIG_SCHEMA_URI_PATH}=  ${MONGOOSE_CONFIG_URI_PATH}/schema

*** Test Cases ***
Should Return Aggregated Defaults
    Should Return Text  ${DATA_DIR}/aggregated_defaults.yaml  ${MONGOOSE_CONFIG_URI_PATH}

Should Return Aggregated Schema
    Should Return Text  ${DATA_DIR}/aggregated_defaults_schema.yaml  ${MONGOOSE_CONFIG_SCHEMA_URI_PATH}

*** Keywords ***
Should Return Text
    [Arguments]  ${expected_file_name}  ${uri_path}
    ${expected_text} =  Get File  ${expected_file_name}
    ${resp} =  Get Request  mongoose_node  ${uri_path}
    Should Be Equal As Strings  ${resp.status_code}  200
    Should Be Equal  ${expected_text}  ${resp.text}
