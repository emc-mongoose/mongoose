FROM docker:dind

RUN apk add --no-cache --update python py-pip \
    && pip install -U virtualenv \
    && pip install -U requests \
    && pip install -U robotframework \
    && pip install -U robotframework-requests \
    && pip install -U robotframework-csvlibrary

ENV PYTHONPATH=$PYTHONPATH:/usr/lib/python2.7/site-packages:/root/mongoose/base/src/test/robot/lib

ADD base/build /root/mongoose/base/build
ADD ci/docker/entrypoint_robotest.sh /root/mongoose/ci/docker/entrypoint_robotest.sh
ADD base/src/test/robot /root/mongoose/base/src/test/robot

RUN chmod ugo+x /root/mongoose/ci/docker/entrypoint_robotest.sh

WORKDIR /root/mongoose

ENTRYPOINT ["/root/mongoose/ci/docker/entrypoint_robotest.sh"]
