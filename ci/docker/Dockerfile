ARG BASE_VERSION
FROM emcmongoose/mongoose-base:${BASE_VERSION}
ARG BASE_VERSION
ARG LOAD_STEP_PIPELINE_VERSION
ARG LOAD_STEP_WEIGHTED_VERSION
ARG STORAGE_DRIVER_COOP_VERSION
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
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-netty/${STORAGE_DRIVER_NETTY_VERSION}/mongoose-storage-driver-netty-${STORAGE_DRIVER_NETTY_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-storage-driver-netty.jar \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-nio/${STORAGE_DRIVER_NIO_VERSION}/mongoose-storage-driver-nio-${STORAGE_DRIVER_NIO_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-storage-driver-nio.jar \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-fs/${STORAGE_DRIVER_FS_VERSION}/mongoose-storage-driver-fs-${STORAGE_DRIVER_FS_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-storage-driver-fs.jar \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-http/${STORAGE_DRIVER_HTTP_VERSION}/mongoose-storage-driver-http-${STORAGE_DRIVER_HTTP_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-storage-driver-http.jar \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-atmos/${STORAGE_DRIVER_ATMOS_VERSION}/mongoose-storage-driver-atmos-${STORAGE_DRIVER_ATMOS_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-storage-driver-atmos.jar \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-s3/${STORAGE_DRIVER_S3_VERSION}/mongoose-storage-driver-s3-${STORAGE_DRIVER_S3_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-storage-driver-s3.jar \
	&& curl http://repo.maven.apache.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-swift/${STORAGE_DRIVER_SWIFT_VERSION}/mongoose-storage-driver-swift-${STORAGE_DRIVER_SWIFT_VERSION}.jar -o $HOME/.mongoose/${BASE_VERSION}/ext/mongoose-storage-driver-swift.jar
