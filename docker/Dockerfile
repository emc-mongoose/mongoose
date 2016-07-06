FROM java:8

MAINTAINER EMC

ADD build/dist/mongoose*.tgz /opt/

RUN ln -s /opt/mongoose* /opt/mongoose

EXPOSE 1099

ENTRYPOINT ["java", "-jar", "/opt/mongoose/mongoose.jar"]
