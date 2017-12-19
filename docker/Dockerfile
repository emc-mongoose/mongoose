ARG MONGOOSE_VERSION

FROM emcmongoose/mongoose-base:${MONGOOSE_VERSION}

ADD ["https://github.com/emc-mongoose/mongoose-storage-driver-s3/releases/download/latest/mongoose-storage-driver-s3.jar", "/opt/mongoose/ext/mongoose-storage-driver-s3.jar"]

ADD ["https://github.com/emc-mongoose/mongoose-storage-driver-atmos/releases/download/latest/mongoose-storage-driver-atmos.jar", "/opt/mongoose/ext/mongoose-storage-driver-atmos.jar"]

ADD ["https://github.com/emc-mongoose/mongoose-storage-driver-swift/releases/download/latest/mongoose-storage-driver-swift.jar", "/opt/mongoose/ext/mongoose-storage-driver-swift.jar"]

ADD ["https://github.com/emc-mongoose/mongoose-storage-driver-emc-s3/releases/download/latest/mongoose-storage-driver-emc-s3.jar", "/opt/mongoose/ext/mongoose-storage-driver-emc-s3.jar"]

ADD ["https://github.com/emc-mongoose/mongoose-storage-driver-fs/releases/download/latest/mongoose-storage-driver-fs.jar", "/opt/mongoose/ext/mongoose-storage-driver-fs.jar"]
