*** Settings ***
Documentation  Mongoose FS Storage Driver For Win Tests
Force Tags  FS WIN
Library  Process
Library  Collections
Library  OperatingSystem
Library  Validation
Test Teardown  Remove Files

*** Variables ***

${HOME_DIR}   C:\projects\mongoose
${MONGOOSE_JAR_PATH}    ${HOME_DIR}\build\libs\mongoose-4.1.1.jar
${ITEM_OUTPUT_PATH}     ${HOME_DIR}\build\fs-results
${ITEM_COUNT}    10


*** Test Cases ***

Schould Create Files Test
#	Create Directory  ${ITEM_OUTPUT_PATH}
#	Start Mongoose

*** Keywords ***

Start Mongoose
	${cmd} =  Catenate  java -jar ${MONGOOSE_JAR_PATH} --storage-driver-type=fs --item-output-path=${ITEM_OUTPUT_PATH} --load-op-limit-count=${ITEM_COUNT}
	${std_out} =  Run Process  ${cmd}
    Log  ${std_out}

Create Directory
	[Arguments]  ${path}
#	${cmd} =  Catenate  mkdir ${path}
	${cmd} =  Catenate  IF EXIST %PATH_TO_ARTIFACTS%  ECHO %PATH_TO_ARTIFACTS% exists.
	${std_out} =  Run Process  ${cmd}
    Log  ${std_out}

Remove Directory
	[Arguments]  ${path}
#    ${cmd} =  Catenate  mkdir ${path}
    ${std_out} =  Run Process  ${cmd}
    Log  ${std_out}
