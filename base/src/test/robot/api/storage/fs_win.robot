*** Settings ***
Documentation  Mongoose FS Storage Driver For Win Tests
Force Tags  FS WIN
Library  Collections
Library  OperatingSystem
Library  Validation
Library  String
Test Teardown  Remove Files

*** Variables ***

${VERSION}     4.2.0
${HOME_DIR}   C:\\projects\\mongoose
${MONGOOSE_JAR_PATH}    ${HOME_DIR}\\build\\libs\\mongoose-${VERSION}.jar
${ITEM_OUTPUT_PATH}     ${HOME_DIR}\\build\\fs-results
${ITEM_COUNT}    10
${ITEM_SIZE}    10KB
${STEP_ID}   win_fs_robotest

*** Test Cases ***

Should Create Files Test
	Create Directory  ${ITEM_OUTPUT_PATH}
	Start Mongoose
	${log_dir} =  Get Log Directory
	Validate Log File Metrics Total  ${log_dir}\\${STEP_ID}  file_separator=\\  count_succ_min=${10}  count_succ_max=${10}
	...  transfer_size=${10240000}  transfer_size_delta=${10240000}

*** Keywords ***

Get Log Directory
	${cmd} =  Catenate  ECHO %HomeDrive%%HomePath%
	${std_out} =  Run   ${cmd}
	${std_out} =  Strip String  ${std_out}
	[Return]  ${std_out}\\.mongoose\\${VERSION}\\log

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


