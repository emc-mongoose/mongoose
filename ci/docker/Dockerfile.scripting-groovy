ARG MONGOOSE_VERSION

FROM emcmongoose/mongoose:${MONGOOSE_VERSION}

ADD ["http://central.maven.org/maven2/org/codehaus/groovy/groovy/2.4.15/groovy-2.4.15.jar", "/root/.mongoose/${MONGOOSE_VERSION}/ext/groovy.jar"]
ADD ["http://central.maven.org/maven2/org/codehaus/groovy/groovy-jsr223/2.4.15/groovy-jsr223-2.4.15.jar", "/root/.mongoose/${MONGOOSE_VERSION}/ext/groovy-jsr223.jar"]
