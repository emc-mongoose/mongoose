![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-bundle/maven-metadata.xml.svg?label=%20)
[![CI status](https://gitlab.com/emc-mongoose/mongoose/badges/master/pipeline.svg)](https://gitlab.com/emc-mongoose/mongoose/commits/master)
[![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose.svg)](https://hub.docker.com/r/emcmongoose/mongoose/)
[![Gitter chat](https://badges.gitter.im/emc-mongoose.png)](https://gitter.im/emc-mongoose)

# Introduction

The repo contains the automation scripts to build/test/deploy the Mongoose backward compatibility bundle. Previously the
repo contained the Mongoose sources for the basic functionality and some commonly used extensions. Currently it was
split into the independent repos and the corresponding components. Each component has its own documentation, CI versioning. For the
mongoose documentation refer this [link](https://github.com/emc-mongoose/mongoose-base/tree/master/doc).

# Bundle Contents

The components listed below are included in this backward compatibility bundle.

| Repo | Description | Latest Release | Integration Status | Issue Tracker Link |
|------|-------------|---------|-------------------------------|--------|
| [mongoose-**base**](https://github.com/emc-mongoose/mongoose-base) | Mongoose storage performance testing tool - base functionality | ![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-base/maven-metadata.xml.svg?label=%20) | ![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/emc-mongoose/mongoose-base.svg?label=%20) | [BASE](https://mongoose-issues.atlassian.net/projects/BASE)
| [mongoose-load-step-**pipeline**](https://github.com/emc-mongoose/mongoose-load-step-pipeline) | Load operations pipeline (create,delay,read-then-update, for example), extension |  ![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-load-step-pipeline/maven-metadata.xml.svg?label=%20) | ![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/emc-mongoose/mongoose-load-step-pipeline.svg?label=%20) | [BASE](https://mongoose-issues.atlassian.net/projects/BASE)
| [mongoose-load-step-**weighted**](https://github.com/emc-mongoose/mongoose-load-step-weighted) | Weighted load extension, allowing to generate 20% write and 80% read operations, for example |  ![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-load-step-weighted/maven-metadata.xml.svg?label=%20) | ![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/emc-mongoose/mongoose-load-step-weighted.svg?label=%20) | [BASE](https://mongoose-issues.atlassian.net/projects/BASE)
| [mongoose-storage-driver-**coop**](https://github.com/emc-mongoose/mongoose-storage-driver-coop) | Cooperative multitasking storage driver primitive, utilizing [fibers](https://github.com/akurilov/fiber4j) |  ![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-coop/maven-metadata.xml.svg?label=%20) | ![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/emc-mongoose/mongoose-storage-driver-coop.svg?label=%20) | [BASE](https://mongoose-issues.atlassian.net/projects/BASE)
| [mongoose-storage-driver-**netty**](https://github.com/emc-mongoose/mongoose-storage-driver-netty) | [Netty](https://netty.io/)-storage-driver-nettyd storage driver primitive, extends the cooperative multitasking storage driver primitive |  ![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-netty/maven-metadata.xml.svg?label=%20) | ![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/emc-mongoose/mongoose-storage-driver-netty.svg?label=%20) | [BASE](https://mongoose-issues.atlassian.net/projects/BASE)
| [mongoose-storage-driver-**nio**](https://github.com/emc-mongoose/mongoose-storage-driver-nio) | Non-blocking I/O storage driver primitive, extends the cooperative multitasking storage driver primitive |  ![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-nio/maven-metadata.xml.svg?label=%20) | ![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/emc-mongoose/mongoose-storage-driver-nio.svg?label=%20) | [BASE](https://mongoose-issues.atlassian.net/projects/BASE)
| [mongoose-storage-driver-**http**](https://github.com/emc-mongoose/mongoose-storage-driver-http) | HTTP storage driver primitive, extends the Netty-storage-driver-httpd storage driver primitive |  ![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-http/maven-metadata.xml.svg?label=%20) | ![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/emc-mongoose/mongoose-storage-driver-http.svg?label=%20) | [BASE](https://mongoose-issues.atlassian.net/projects/BASE)
| [mongoose-storage-driver-**fs**](https://github.com/emc-mongoose/mongoose-storage-driver-fs) | [VFS](https://www.oreilly.com/library/view/understanding-the-linux/0596005652/ch12s01.html) storage driver, extends the NIO storage driver primitive |  ![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-fs/maven-metadata.xml.svg?label=%20) | ![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/emc-mongoose/mongoose-storage-driver-fs.svg?label=%20) | [FS](https://mongoose-issues.atlassian.net/projects/FS)
| [mongoose-storage-driver-**atmos**](https://github.com/emc-mongoose/mongoose-storage-driver-atmos) | [Dell EMC Atmos](https://poland.emc.com/collateral/software/data-sheet/h5770-atmos-ds.pdf) storage driver, extends the HTTP storage driver primitive | ![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-atmos/maven-metadata.xml.svg?label=%20) | ![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/emc-mongoose/mongoose-storage-driver-atmos.svg?label=%20) | [BASE](https://mongoose-issues.atlassian.net/projects/BASE)
| [mongoose-storage-driver-**s3**](https://github.com/emc-mongoose/mongoose-storage-driver-s3) | [Amazon S3](https://docs.aws.amazon.com/en_us/AmazonS3/latest/API/Welcome.html) storage driver, extends the HTTP storage driver primitive | ![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-s3/maven-metadata.xml.svg?label=%20) | ![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/emc-mongoose/mongoose-storage-driver-s3.svg?label=%20) | [S3](https://mongoose-issues.atlassian.net/projects/S3)
| [mongoose-storage-driver-**swift**](https://github.com/emc-mongoose/mongoose-storage-driver-swift) | [OpenStack Swift](https://wiki.openstack.org/wiki/Swift) storage driver, extends the HTTP storage driver primitive | ![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-swift/maven-metadata.xml.svg?label=%20) | ![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/emc-mongoose/mongoose-storage-driver-swift.svg?label=%20) | [SWIFT](https://mongoose-issues.atlassian.net/projects/SWIFT)

# Additional Extensions

The *additional extension* are not included in this bundle.

| Repo | Description | Latest Release | Integration Status | Issue Tracker Link |
|------|-------------|---------|-------------------------------|--------|
| [mongoose-storage-driver-**preempt**](https://github.com/emc-mongoose/mongoose-storage-driver-preempt) | Preemptive multitasking storage driver primitive, using thread-per-task approach for the I/O |  ![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-preempt/maven-metadata.xml.svg?label=%20) | ![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/emc-mongoose/mongoose-storage-driver-preempt.svg?label=%20) | [BASE](https://mongoose-issues.atlassian.net/projects/BASE)
| [mongoose-storage-driver-**hdfs**](https://github.com/emc-mongoose/mongoose-storage-driver-hdfs) | [Apache HDFS](http://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-hdfs/HdfsDesign.html) storage driver, extends the NIO storage driver primitive |  ![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-hdfs/maven-metadata.xml.svg?label=%20) | ![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/emc-mongoose/mongoose-storage-driver-hdfs.svg?label=%20) | [HDFS](https://mongoose-issues.atlassian.net/projects/HDFS)
| [mongoose-storage-driver-**pravega**](https://github.com/emc-mongoose/mongoose-storage-driver-pravega) | [Pravega](http://pravega.io) storage driver, extends the preemptive multitasking storage driver primitive | ![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-pravega/maven-metadata.xml.svg?label=%20) | ![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/emc-mongoose/mongoose-storage-driver-pravega.svg?label=%20) | [PRAVEGA](https://mongoose-issues.atlassian.net/projects/PRAVEGA)
| [mongoose-storage-driver-**kafka**](https://github.com/emc-mongoose/mongoose-storage-driver-kafka) | [Apache Kafka](https://kafka.apache.org/) storage driver, extends the preemptive multitasking storage driver primitive | ![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-kafka/maven-metadata.xml.svg?label=%20) | ![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/emc-mongoose/mongoose-storage-driver-kafka.svg?label=%20) | [KAFKA](https://mongoose-issues.atlassian.net/projects/KAFKA)
| [mongoose-storage-driver-**pulsar**](https://github.com/emc-mongoose/mongoose-storage-driver-pulsar) | [Apache Pulsar](https://pulsar.apache.org/) storage driver, extends the cooperative multitasking storage driver primitive | ![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-pulsar/maven-metadata.xml.svg?label=%20) | ![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/emc-mongoose/mongoose-storage-driver-pulsar.svg?label=%20) | [PULSAR](https://mongoose-issues.atlassian.net/projects/PULSAR)

# Auxiliary Tools

| Repo | Description | Latest Release | Integration Status | Issue Tracker Link |
|------|-------------|---------|-------------------------------|--------|
| [darzee](https://github.com/emc-mongoose/darzee) | Mongoose GUI web application | TBD | TBD | [GUI](https://mongoose-issues.atlassian.net/browse/GUI)
| [mongoose-helm-charts](https://github.com/emc-mongoose/mongoose-helm-charts) | [Helm](https://helm.sh/) charts to easily deploy Mongoose in [K8s](https://kubernetes.io/) environment | TBD | TBD | [HELM](https://mongoose-issues.atlassian.net/projects/HELM/issues) 
| [e2e-latency-generator](https://github.com/emc-mongoose/e2e-latency-generator) | The tool consuming the Mongoose's operations trace output data and producing the raw end-to-end latency data and heatmap chart | - | - | -

# Backward Compatibility Notes

* The extensions are not overriding the base default options when launched from the jar file. E.g. the default storage
  port is 7 and the default storage driver is "dummy-mock". Override the defaults explicitly or consider using the
  Docker image.

* The base Mongoose version and this bundle version may differ. The base version is used to determine the logs output
  path.

Example:
```bash 
java -jar mongoose-bundle-<BUNDLE_VERSION>.jar \
    --storage-driver-type=s3 \
    --storage-net-node-port=9020
```

# Build

```bash
./gradlew clean jar
ls -l build/libs
```

# Deploy

## Bare Jar Download

http://central.maven.org/maven2/com/github/emc-mongoose/mongoose-bundle/

## Docker

```bash
docker run ... emcmongoose/mongoose[:<VERSION>] ...
```

# Dependency

The following dependency graph should be considered when running Mongoose w/o Docker and using some specific extension.
For example, using the `mongoose-storage-driver-hdfs` extension will require to have the `mongoose-storage-driver-coop`
and `mongoose-storage-driver-nio` extensions in the `~/.mongoose/<BASE_VERSION>/ext` directory (with proper versions).

```
mongoose-base
    |___ mongoose-load-step-pipeline
    |___ mongoose-load-step-weighted
    |___ mongoose-storage-driver-coop
    |       |___ mongoose-storage-driver-netty
    |       |       |___ mongoose-storage-driver-http
    |       |               |___ mongoose-storage-driver-atmos
    |       |               |___ mongoose-storage-driver-s3
    |       |               |___ mongoose-storage-driver-swift
    |       |___ mongoose-storage-driver-nio
    |       |       |___ mongoose-storage-driver-fs
    |       |       |___ mongoose-storage-driver-hdfs
    |       |___ mongoose-storage-driver-pulsar
    |___ mongoose-storage-driver-preempt
            |___ mongoose-storage-driver-kafka
            |___ mongoose-storage-driver-pravega
```
