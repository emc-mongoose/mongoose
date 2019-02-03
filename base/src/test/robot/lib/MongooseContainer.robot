*** Settings ***
Documentation  Mongoose Container Keywords
Library  OperatingSystem
Library  RequestsLibrary

*** Variables ***
${LOG_DIR} =  base/build/log
${MONGOOSE_CONTAINER_DATA_DIR} =  /data
${MONGOOSE_CONTAINER_NAME} =  mongoose
${MONGOOSE_IMAGE_NAME} =  emcmongoose/mongoose
${MONGOOSE_NODE_PORT} =  9999

*** Keywords ***
Start Mongoose Node
    ${image_version} =  Get Environment Variable  MONGOOSE_IMAGE_VERSION
    # ${service_host} should be used instead of the "localhost" in GL CI
    ${service_host} =  Get Environment Variable  SERVICE_HOST
    ${cmd} =  Catenate  SEPARATOR= \\\n\t
    ...  docker run
    ...  --detach
    ...  --name ${MONGOOSE_CONTAINER_NAME}
    ...  --publish ${MONGOOSE_NODE_PORT}:${MONGOOSE_NODE_PORT}
    ...  ${MONGOOSE_IMAGE_NAME}:${image_version}
    ...  --load-step-id=robotest
    ...  --run-node
    ${std_out} =  Run  ${cmd}
    Log  ${std_out}
    Create Session  mongoose_node  http://${service_host}:${MONGOOSE_NODE_PORT}  debug=1  timeout=1000  max_retries=10

Execute Mongoose Scenario
    [Arguments]  ${shared_data_dir}  ${env}  ${args}
    ${docker_env_vars} =  Evaluate  ' '.join(['-e %s=%s' % (key, value) for (key, value) in ${env}.items()])
    ${host_working_dir} =  Get Environment Variable  HOST_WORKING_DIR
    Log  Host working dir: ${host_working_dir}
    ${mongoose_version} =  Get Environment Variable  MONGOOSE_VERSION
    ${image_version} =  Get Environment Variable  MONGOOSE_IMAGE_VERSION
    ${cmd} =  Catenate  SEPARATOR= \\\n\t
    ...  docker run
    ...  --name ${MONGOOSE_CONTAINER_NAME}
    ...  --network host
    ...  ${docker_env_vars}
    ...  --volume ${host_working_dir}/${shared_data_dir}:${MONGOOSE_CONTAINER_DATA_DIR}
    ...  --volume ${host_working_dir}/${LOG_DIR}:/root/.mongoose/${mongoose_version}/log
    ...  ${MONGOOSE_IMAGE_NAME}:${image_version}
    ...  ${args}
    ${std_out} =  Run  ${cmd}
    [Return]  ${std_out}

Remove Mongoose Node
    Delete All Sessions
    Run  docker stop ${MONGOOSE_CONTAINER_NAME}
    Remove Mongoose Container

Remove Mongoose Container
    Run  docker rm ${MONGOOSE_CONTAINER_NAME}
