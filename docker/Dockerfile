FROM openjdk:8-jre-alpine

ADD build/dist/mongoose-*.tgz /opt/

RUN ln -s /opt/mongoose-* /opt/mongoose

EXPOSE 9010

ENTRYPOINT ["java", "-jar", "/opt/mongoose/mongoose.jar"]
