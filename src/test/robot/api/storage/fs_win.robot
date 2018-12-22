*** Settings ***
Documentation  Mongoose FS Storage Driver For Win Tests
Force Tags  FS WIN
Library  Process
Library  Collections
Library  OperatingSystem
Library  Validation
Test Teardown  Remove Files

*** Variables ***

${MONGOOSE_JAR_PATH}    build\libs\mongoose-4.1.0.jar
${ITEM_OUTPUT_PATH}     \tmp\fs-results
${ITEM_COUNT}    10


*** Test Cases ***

Schould Create Files Test
	Start Mongoose

*** Keywords ***

Start Mongoose
	${cmd} =  Catenate  java -jar build\libs\mongoose-4.1.0.jar --storage-driver-type=fs --item-output-path=${ITEM_OUTPUT_PATH} --load-op-limit-count=${ITEM_COUNT}
	${std_out} =  Run Process  ${cmd}
    Log  ${std_out}
	
