FROM openjdk:8-jre-alpine

ADD build/dist/mongoose*.tgz /opt/

RUN ln -s /opt/mongoose* /opt/mongoose

EXPOSE 9010

ENTRYPOINT ["java", "-Dcom.sun.management.jmxremote", "-Dcom.sun.management.jmxremote.port=9010", "-Dcom.sun.management.jmxremote.rmi.port=9010", "-Dcom.sun.management.jmxremote.local.only=false", "-Dcom.sun.management.jmxremote.authenticate=false", "-Dcom.sun.management.jmxremote.ssl=false", "-jar", "/opt/mongoose/mongoose.jar"]
