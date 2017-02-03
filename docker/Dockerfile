FROM java:8

MAINTAINER EMC

ADD build/dist/mongoose*.tgz /opt/

RUN ln -s /opt/mongoose* /opt/mongoose

CMD ["java", "-jar", "/opt/mongoose/mongoose.jar"]
