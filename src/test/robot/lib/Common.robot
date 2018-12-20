*** Settings ***
Documentation  Commons Remoute API Keywords

*** Keywords ***
Start Mongoose Scenario
    [Arguments]  ${data}
    ${resp} =  Post Request  mongoose_node  ${MONGOOSE_RUN_URI_PATH}  files=${data}
    Log  ${resp.status_code}
    [Return]  ${resp}

Stop Mongoose Scenario Run
    [Arguments]  ${etag}
    &{req_headers} =  Create Dictionary  If-Match=${etag}
    ${resp} =  Delete Request  mongoose_node  ${MONGOOSE_RUN_URI_PATH}  headers=${req_headers}
    Log  ${resp.status_code}
    [Return]  ${resp}
