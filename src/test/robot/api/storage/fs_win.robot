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

Foo Test Case
    [tags]              FooTag
    [Documentation]     Created by John Doe
    Do An Action        Argument
    Do Another Action   ${robotVar}
