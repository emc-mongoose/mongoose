FROM alpine:3.8

ENV LANG C.UTF-8

ENV JAVA_HOME /opt/openjdk-13
ENV PATH $JAVA_HOME/bin:$PATH

# http://jdk.java.net/
ENV JAVA_VERSION 13-ea+1
ENV JAVA_URL https://download.java.net/java/early_access/alpine/1/binaries/openjdk-13-ea+1_linux-x64-musl_bin.tar.gz
ENV JAVA_SHA256 ce179bb08dff9980a7cca20df1b87b9556c5c71f3cf2fefbcf31d9bfa25b1804
# "For Alpine Linux, builds are produced on a reduced schedule and may not be in sync with the other platforms."

RUN set -eux; \
	\
	wget -O /openjdk.tgz "$JAVA_URL"; \
	echo "$JAVA_SHA256 */openjdk.tgz" | sha256sum -c -; \
	mkdir -p "$JAVA_HOME"; \
	tar --extract --file /openjdk.tgz --directory "$JAVA_HOME" --strip-components 1; \
	rm /openjdk.tgz; \

RUN mkdir /opt/mongoose $HOME/.mongoose; \
    chmod -R ugo+rwx $HOME/.mongoose

ADD build/libs/mongoose-*.jar /opt/mongoose/
ADD docker/entrypoint.sh /opt/mongoose/entrypoint.sh
ADD docker/entrypoint_debug.sh /opt/mongoose/entrypoint_debug.sh
ADD docker/entrypoint_limit_heap_1GB.sh /opt/mongoose/entrypoint_limit_heap_1GB.sh

RUN ln -s /opt/mongoose/mongoose-*.jar /opt/mongoose/mongoose.jar; \
    chmod +x /opt/mongoose/entrypoint.sh; \
    chmod +x /opt/mongoose/entrypoint_debug.sh; \
    chmod +x /opt/mongoose/entrypoint_limit_heap_1GB.sh

ENTRYPOINT ["/opt/mongoose/entrypoint.sh"]
