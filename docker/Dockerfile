FROM openjdk:8-jre-alpine

RUN mkdir /opt /opt/mongoose /root/.mongoose; \
    chmod -R ugo+rwx /root/.mongoose

ADD build/libs/mongoose-*.jar /opt/mongoose/
ADD docker/entrypoint.sh /opt/mongoose/entrypoint.sh

RUN ln -s /opt/mongoose/mongoose-*.jar /opt/mongoose/mongoose.jar; \
    chmod +x /opt/mongoose/entrypoint.sh

ENTRYPOINT /opt/mongoose/entrypoint.sh

EXPOSE 9010
