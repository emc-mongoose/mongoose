<<<<<<< HEAD
ARG BASE_VERSION
FROM emcmongoose/mongoose-base:${BASE_VERSION}
ARG BASE_VERSION
ARG LOAD_STEP_PIPELINE_VERSION
ARG LOAD_STEP_WEIGHTED_VERSION
ARG STORAGE_DRIVER_COOP_VERSION
ARG STORAGE_DRIVER_PREEMPT_VERSION
ARG STORAGE_DRIVER_NETTY_VERSION
ARG STORAGE_DRIVER_NIO_VERSION
ARG STORAGE_DRIVER_FS_VERSION
ARG STORAGE_DRIVER_HTTP_VERSION
ARG STORAGE_DRIVER_ATMOS_VERSION
ARG STORAGE_DRIVER_S3_VERSION
ARG STORAGE_DRIVER_SWIFT_VERSION
ADD ci/docker/entrypoint.sh /opt/mongoose/entrypoint.sh
RUN mkdir -p $HOME/.mongoose/${BASE_VERSION}/ext \
	&& chmod +x /opt/mongoose/entrypoint.sh \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-load-step-pipeline/${LOAD_STEP_PIPELINE_VERSION}/mongoose-load-step-pipeline-${LOAD_STEP_PIPELINE_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-load-step-pipeline.jar \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-load-step-weighted/${LOAD_STEP_WEIGHTED_VERSION}/mongoose-load-step-weighted-${LOAD_STEP_WEIGHTED_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-load-step-weighted.jar \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-coop/${STORAGE_DRIVER_COOP_VERSION}/mongoose-storage-driver-coop-${STORAGE_DRIVER_COOP_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-storage-driver-coop.jar \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-preempt/${STORAGE_DRIVER_PREEMPT_VERSION}/mongoose-storage-driver-preempt-${STORAGE_DRIVER_PREEMPT_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-storage-driver-preempt.jar \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-netty/${STORAGE_DRIVER_NETTY_VERSION}/mongoose-storage-driver-netty-${STORAGE_DRIVER_NETTY_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-storage-driver-netty.jar \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-nio/${STORAGE_DRIVER_NIO_VERSION}/mongoose-storage-driver-nio-${STORAGE_DRIVER_NIO_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-storage-driver-nio.jar \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-fs/${STORAGE_DRIVER_FS_VERSION}/mongoose-storage-driver-fs-${STORAGE_DRIVER_FS_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-storage-driver-fs.jar \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-http/${STORAGE_DRIVER_HTTP_VERSION}/mongoose-storage-driver-http-${STORAGE_DRIVER_HTTP_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-storage-driver-http.jar \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-atmos/${STORAGE_DRIVER_ATMOS_VERSION}/mongoose-storage-driver-atmos-${STORAGE_DRIVER_ATMOS_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-storage-driver-atmos.jar \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-s3/${STORAGE_DRIVER_S3_VERSION}/mongoose-storage-driver-s3-${STORAGE_DRIVER_S3_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-storage-driver-s3.jar \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-swift/${STORAGE_DRIVER_SWIFT_VERSION}/mongoose-storage-driver-swift-${STORAGE_DRIVER_SWIFT_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-storage-driver-swift.jar
=======
FROM adoptopenjdk/openjdk11:alpine AS build

ADD base ./base/
ADD gradle ./gradle/
ADD load ./load/
ADD storage ./storage/
ADD build.gradle ./build.gradle
ADD gradlew ./gradlew
ADD settings.gradle ./settings.gradle

RUN ./gradlew clean jre

FROM alpine:3.8

RUN apk --update add --no-cache ca-certificates curl openssl binutils xz \
    && GLIBC_VER="2.28-r0" \
    && ALPINE_GLIBC_REPO="https://github.com/sgerrand/alpine-pkg-glibc/releases/download" \
    && GCC_LIBS_URL="https://archive.archlinux.org/packages/g/gcc-libs/gcc-libs-8.2.1%2B20180831-1-x86_64.pkg.tar.xz" \
    && GCC_LIBS_SHA256=e4b39fb1f5957c5aab5c2ce0c46e03d30426f3b94b9992b009d417ff2d56af4d \
    && ZLIB_URL="https://archive.archlinux.org/packages/z/zlib/zlib-1%3A1.2.9-1-x86_64.pkg.tar.xz" \
    && ZLIB_SHA256=bb0959c08c1735de27abf01440a6f8a17c5c51e61c3b4c707e988c906d3b7f67 \
    && curl -Lks https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub -o /etc/apk/keys/sgerrand.rsa.pub \
    && curl -Lks ${ALPINE_GLIBC_REPO}/${GLIBC_VER}/glibc-${GLIBC_VER}.apk > /tmp/${GLIBC_VER}.apk \
    && apk add --allow-untrusted /tmp/${GLIBC_VER}.apk \
    && curl -Lks ${GCC_LIBS_URL} -o /tmp/gcc-libs.tar.xz \
    && echo "${GCC_LIBS_SHA256}  /tmp/gcc-libs.tar.xz" | sha256sum -c - \
    && mkdir /tmp/gcc \
    && tar -xf /tmp/gcc-libs.tar.xz -C /tmp/gcc \
    && mv /tmp/gcc/usr/lib/libgcc* /tmp/gcc/usr/lib/libstdc++* /usr/glibc-compat/lib \
    && strip /usr/glibc-compat/lib/libgcc_s.so.* /usr/glibc-compat/lib/libstdc++.so* \
    && curl -Lks ${ZLIB_URL} -o /tmp/libz.tar.xz \
    && echo "${ZLIB_SHA256}  /tmp/libz.tar.xz" | sha256sum -c - \
    && mkdir /tmp/libz \
    && tar -xf /tmp/libz.tar.xz -C /tmp/libz \
    && mv /tmp/libz/usr/lib/libz.so* /usr/glibc-compat/lib \
    && apk del binutils \
    && rm -rf /tmp/${GLIBC_VER}.apk /tmp/gcc /tmp/gcc-libs.tar.xz /tmp/libz /tmp/libz.tar.xz /var/cache/apk/*

COPY --from=build /build/jre /opt/mongoose

ADD build/libs/mongoose-*.jar /opt/mongoose/
ADD ci/docker/entrypoint.sh /opt/mongoose/entrypoint.sh

RUN ln -s /opt/mongoose/mongoose-*.jar /opt/mongoose/mongoose.jar; \
    mkdir ${HOME}/.mongoose; \
    chmod -R ugo+rwx ${HOME}/.mongoose; \
    chmod -R +x /opt/mongoose/bin; \
    chmod +x /opt/mongoose/entrypoint.sh

ENTRYPOINT ["/opt/mongoose/entrypoint.sh"]
>>>>>>> origin/master
