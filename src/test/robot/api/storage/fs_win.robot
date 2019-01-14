*** Settings ***
Documentation  Mongoose FS Storage Driver For Win Tests
Force Tags  FS WIN
Library  Collections
Library  OperatingSystem
Library  Validation
Test Teardown  Remove Files

*** Variables ***

${HOME_DIR}   C:\\projects\\mongoose
#${HOME_DIR}   C:\\cygwin64\\home\\kochuv\\mongoose
${MONGOOSE_JAR_PATH}    ${HOME_DIR}\\build\\libs\\mongoose-4.1.1.jar
${ITEM_OUTPUT_PATH}     ${HOME_DIR}\\build\\fs-results
${ITEM_COUNT}    10
${ITEM_SIZE}    10KB
${STEP_ID}   win_fs_robotest
${LOG_DIR}   %HomeDrive%%HomePath%\\.mongoose\\4.1.1\\log
#${LOG_DIR}   C:\\Users\\kochuv\\.mongoose\\4.1.1\\log



*** Test Cases ***

Should Create Files Test
	Create Directory  ${ITEM_OUTPUT_PATH}
	Start Mongoose
	Validate Log File Metrics Total  ${LOG_DIR}\\${STEP_ID}  file_separator=\\  count_succ_min=${10}  count_succ_max=${10}
	...  transfer_size=${10240000}  transfer_size_delta=${10240000}

*** Keywords ***

Start Mongoose
	${cmd} =  Catenate  SEPARATOR=\t
	...  java -jar ${MONGOOSE_JAR_PATH}
	...  --item-data-size=${ITEM_SIZE}
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

