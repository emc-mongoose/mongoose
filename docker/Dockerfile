FROM openjdk:8-jre-alpine

RUN mkdir /opt /opt/mongoose /root/.mongoose; \
    chmod -R ugo+rwx /root/.mongoose

ADD build/libs/mongoose-*.jar /opt/mongoose/
ADD docker/entrypoint.sh /opt/mongoose/entrypoint.sh
ADD docker/entrypoint-debug.sh /opt/mongoose/entrypoint-debug.sh
ADD docker/entrypoint-jmx.sh /opt/mongoose/entrypoint-jmx.sh

RUN ln -s /opt/mongoose/mongoose-*.jar /opt/mongoose/mongoose.jar; \
    chmod +x /opt/mongoose/entrypoint.sh; \
    chmod +x /opt/mongoose/entrypoint-debug.sh; \
    chmod +x /opt/mongoose/entrypoint-jmx.sh

ENTRYPOINT ["/opt/mongoose/entrypoint.sh"]

EXPOSE 9010
