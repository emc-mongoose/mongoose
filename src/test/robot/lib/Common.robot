*** Settings ***
Documentation  Commons Remoute API Keywords

*** Keywords ***
Start Mongoose Scenario
    [Arguments]  ${defaults_file_name}  ${scenario_file_name}
    ${defaults_data} =  Get Binary File  ${defaults_file_name}
    ${scenario_data} =  Get Binary File  ${scenario_file_name}
    &{data} =  Create Dictionary  defaults=${defaults_data}  scenario=${scenario_data}
    ${resp} =  Post Request  mongoose_node  ${MONGOOSE_RUN_URI_PATH}  files=${data}
    Log  ${resp.status_code}
    [Return]  ${resp}

Stop Mongoose Scenario Run
    [Arguments]  ${etag}
    &{req_headers} =  Create Dictionary  If-Match=${etag}
    ${resp} =  Delete Request  mongoose_node  ${MONGOOSE_RUN_URI_PATH}  headers=${req_headers}
    Log  ${resp.status_code}
    [Return]  ${resp}
