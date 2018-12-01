#!/bin/sh
umask 0000
docker run hello-world
docker run \
    --name s3storage \
    --publish 9000:9000
    --env MINIO_ACCESS_KEY=user1 \
    --env MINIO_SECRET_KEY=secretKey1 \
    minio/minio \
    server /data
docker kill s3storage
docker rm s3storage
docker run \
    --name mongoose \
    --network host \
    emcmongoose/mongoose:0f8c0e8ca85e5a8d148904ddca0588abe9feeb7e \
    --load-op-limit-count=1000 \
    --storage-driver-type=dummy-mock
robot --outputdir /root/mongoose/build/robotest --suite ${SUITE} --include ${TEST} /root/mongoose/src/test/robot
rebot /root/mongoose/build/robotest/output.xml
