*** Settings ***
Documentation  Mongoose FS Storage Driver For Win Tests
Force Tags  FS-WIN
Library  Collections
Library  OperatingSystem
Library  Validation
Test Teardown  Remove Files

*** Variables ***

${MONGOOSE_JAR_PATH}    build\libs\mongoose-4.1.0.jar
${ITEM_OUTPUT_PATH}
${ITEM_COUNT}


*** Test Cases ***

Schould Create Files Test


*** Keywords ***

Start Mongoose
	
