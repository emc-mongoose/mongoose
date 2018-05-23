FROM openjdk:8-jre-alpine

RUN mkdir /opt/mongoose /root/.mongoose

RUN chmod -R ugo+rwx /root/.mongoose

ADD build/libs/mongoose-*.jar /opt/mongoose/

RUN ln -s /opt/mongoose/mongoose-*.jar /opt/mongoose/mongoose.jar

ADD docker/entrypoint.sh /opt/mongoose/entrypoint.sh

RUN chmod +x /opt/mongoose/entrypoint.sh

ENTRYPOINT /opt/mongoose/entrypoint.sh

EXPOSE 9010
