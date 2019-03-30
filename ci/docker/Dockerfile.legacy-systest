FROM docker:dind

RUN apk add --no-cache --update openjdk8

ADD ci /root/mongoose/ci
ADD gradle /root/mongoose/gradle
ADD load /root/mongoose/load
ADD src /root/mongoose/src
ADD storage /root/mongoose/storage
ADD build.gradle /root/mongoose/build.gradle
ADD gradlew /root/mongoose/gradlew
ADD settings.gradle /root/mongoose/settings.gradle

RUN chmod ugo+x /root/mongoose/ci/docker/entrypoint_legacy_systest.sh

WORKDIR /root/mongoose

ENTRYPOINT ["/root/mongoose/docker/entrypoint_legacy_systest.sh"]
