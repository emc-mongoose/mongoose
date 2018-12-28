*** Settings ***
Documentation  Mongoose FS Storage Driver For Win Tests
Force Tags  FS WIN
Library  Collections
Library  OperatingSystem
Library  Validation
Test Teardown  Remove Files

*** Variables ***

${HOME_DIR}   C:\\projects\\mongoose
${MONGOOSE_JAR_PATH}    ${HOME_DIR}\\build\\libs\\mongoose-4.1.1.jar
${ITEM_OUTPUT_PATH}     ${HOME_DIR}\\build\\fs-results
${ITEM_COUNT}    10
${STEP_ID}   win_fs_robotest
${LOG_DIR}   C:\\projects\\mongoose\\build\\log


*** Test Cases ***

Schould Create Files Test
	Create Directory  ${ITEM_OUTPUT_PATH}
	Start Mongoose
	Validate Log File Metrics Total  """${LOG_DIR}\\${STEP_ID}"""  file_separator=\\  count_succ_min=${10}  count_succ_max=${10}

*** Keywords ***

Start Mongoose
	${cmd} =  Catenate  SEPARATOR=
	...  java -jar ${MONGOOSE_JAR_PATH}
	...  --storage-driver-type=fs
	...  --item-output-path=${ITEM_OUTPUT_PATH}
	...  --load-op-limit-count=${ITEM_COUNT}
	...  --load-step-id=${STEP_ID}
	${std_out} =  Run   ${cmd}
    Log  ${std_out}

Create Directory
	[Arguments]  ${path}
	${cmd} =  Catenate  mkdir ${path}
	${std_out} =  Run   ${cmd}
    Log  ${std_out}

Remove Directory
	[Arguments]  ${path}
#    ${cmd} =  Catenate  mkdir ${path}
    ${std_out} =  Run  ${cmd}
    Log  ${std_out}

