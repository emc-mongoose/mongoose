FROM nile-registry.lss.emc.com:5000/emccaspian/java8:latest

MAINTAINER EMC

ADD mongoose*.tgz /opt/

RUN ln -s /opt/mongoose* /opt/mongoose

EXPOSE 1099

ENTRYPOINT ["java", "-jar", "/opt/mongoose/mongoose.jar"]
