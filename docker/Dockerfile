FROM openjdk:8-jre-alpine

RUN mkdir /opt /opt/mongoose $HOME/.mongoose; \
    chmod -R ugo+rwx $HOME/.mongoose

ADD build/libs/mongoose-*.jar /opt/mongoose/
ADD docker/entrypoint.sh /opt/mongoose/entrypoint.sh
ADD docker/entrypoint-debug.sh /opt/mongoose/entrypoint-debug.sh


RUN ln -s /opt/mongoose/mongoose-*.jar /opt/mongoose/mongoose.jar; \
    chmod +x /opt/mongoose/entrypoint.sh; \
    chmod +x /opt/mongoose/entrypoint-debug.sh

ENTRYPOINT ["/opt/mongoose/entrypoint.sh"]
