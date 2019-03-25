
| Repo | Description | Included? | Status |
|------|-------------|--------------------------|--------|
| [mongoose-**base**](https://github.com/emc-mongoose/mongoose-base) | Mongoose storage performance testing tool - base functionality | :heavy_check_mark: | [![](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-base/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/github/emc-mongoose/mongoose-base) <br/> [![CI status](https://gitlab.com/emc-mongoose/mongoose-base/badges/master/pipeline.svg)](https://gitlab.com/emc-mongoose/mongoose-base/commits/master) <br/> [![](https://img.shields.io/badge/Issue-Tracker-orange.svg)](https://mongoose-issues.atlassian.net/projects/BASE) <br/> [![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose-base.svg)](https://hub.docker.com/r/emcmongoose/mongoose-base/)
| [mongoose-load-step-**pipeline**](https://github.com/emc-mongoose/mongoose-load-step-pipeline) | Load operations pipeline (create,delay,read-then-update, for example), extension | :heavy_check_mark: | [![](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-load-step-pipeline/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/github/emc-mongoose/mongoose-load-step-pipeline) <br/> [![CI status](https://gitlab.com/emc-mongoose/mongoose-load-step-pipeline/badges/master/pipeline.svg)](https://gitlab.com/emc-mongoose/mongoose-load-step-pipeline/commits/master) <br/> [![](https://img.shields.io/badge/Issue-Tracker-orange.svg)](https://mongoose-issues.atlassian.net/projects/BASE) <br/> [![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose-load-step-pipeline.svg)](https://hub.docker.com/r/emcmongoose/mongoose-load-step-pipeline/)
| [mongoose-load-step-**weighted**](https://github.com/emc-mongoose/mongoose-load-step-weighted) | Weighted load extension, allowing to generate 20% write and 80% read operations, for example | :heavy_check_mark: | [![](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-load-step-weighted/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/github/emc-mongoose/mongoose-load-step-weighted) <br/> [![CI status](https://gitlab.com/emc-mongoose/mongoose-load-step-weighted/badges/master/pipeline.svg)](https://gitlab.com/emc-mongoose/mongoose-load-step-weighted/commits/master) <br/> [![](https://img.shields.io/badge/Issue-Tracker-orange.svg)](https://mongoose-issues.atlassian.net/projects/BASE) <br/> [![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose-load-step-weighted.svg)](https://hub.docker.com/r/emcmongoose/mongoose-load-step-weighted/)
| [mongoose-storage-driver-**coop**](https://github.com/emc-mongoose/mongoose-storage-driver-coop) | Cooperative multitasking storage driver primitive, utilizing [fibers](https://github.com/akurilov/fiber4j) | :heavy_check_mark: | [![](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-coop/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-coop) <br/> [![CI status](https://gitlab.com/emc-mongoose/mongoose-storage-driver-coop/badges/master/pipeline.svg)](https://gitlab.com/emc-mongoose/mongoose-storage-driver-coop/commits/master) <br/> [![](https://img.shields.io/badge/Issue-Tracker-orange.svg)](https://mongoose-issues.atlassian.net/projects/BASE) <br/> [![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose-storage-driver-coop.svg)](https://hub.docker.com/r/emcmongoose/mongoose-storage-driver-coop/)
| [mongoose-storage-driver-**preempt**](https://github.com/emc-mongoose/mongoose-storage-driver-preempt) | Preemptive multitasking storage driver primitive, using thread-per-task approach for the I/O | :heavy_check_mark: | [![](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-preempt/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-preempt) <br/> [![CI status](https://gitlab.com/emc-mongoose/mongoose-storage-driver-preempt/badges/master/pipeline.svg)](https://gitlab.com/emc-mongoose/mongoose-storage-driver-preempt/commits/master) <br/> [![](https://img.shields.io/badge/Issue-Tracker-orange.svg)](https://mongoose-issues.atlassian.net/projects/BASE) <br/> [![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose-storage-driver-preempt.svg)](https://hub.docker.com/r/emcmongoose/mongoose-storage-driver-preempt/)
| [mongoose-storage-driver-**netty**](https://github.com/emc-mongoose/mongoose-storage-driver-netty) | [Netty](https://netty.io/)-based storage driver primitive, extends the cooperative multitasking storage driver primitive | :heavy_check_mark: | [![](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-netty/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-netty) <br/> [![CI status](https://gitlab.com/emc-mongoose/mongoose-storage-driver-netty/badges/master/pipeline.svg)](https://gitlab.com/emc-mongoose/mongoose-storage-driver-netty/commits/master) <br/> [![](https://img.shields.io/badge/Issue-Tracker-orange.svg)](https://mongoose-issues.atlassian.net/projects/BASE) <br/> [![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose-storage-driver-netty.svg)](https://hub.docker.com/r/emcmongoose/mongoose-storage-driver-netty/)
| [mongoose-storage-driver-**nio**](https://github.com/emc-mongoose/mongoose-storage-driver-nio) | Non-blocking I/O storage driver primitive, extends the cooperative multitasking storage driver primitive | :heavy_check_mark: | [![](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-nio/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-nio) <br/> [![CI status](https://gitlab.com/emc-mongoose/mongoose-storage-driver-nio/badges/master/pipeline.svg)](https://gitlab.com/emc-mongoose/mongoose-storage-driver-nio/commits/master) <br/> [![](https://img.shields.io/badge/Issue-Tracker-orange.svg)](https://mongoose-issues.atlassian.net/projects/BASE) <br/> [![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose-storage-driver-nio.svg)](https://hub.docker.com/r/emcmongoose/mongoose-storage-driver-nio/)
| [mongoose-storage-driver-**http**](https://github.com/emc-mongoose/mongoose-storage-driver-http) | HTTP storage driver primitive, extends the Netty-based storage driver primitive | :heavy_check_mark: | [![](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-http/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-http) <br/> [![CI status](https://gitlab.com/emc-mongoose/mongoose-storage-driver-http/badges/master/pipeline.svg)](https://gitlab.com/emc-mongoose/mongoose-storage-driver-http/commits/master) <br/> [![](https://img.shields.io/badge/Issue-Tracker-orange.svg)](https://mongoose-issues.atlassian.net/projects/BASE) <br/> [![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose-storage-driver-http.svg)](https://hub.docker.com/r/emcmongoose/mongoose-storage-driver-http/)
| [mongoose-storage-driver-**fs**](https://github.com/emc-mongoose/mongoose-storage-driver-fs) | [VFS](https://www.oreilly.com/library/view/understanding-the-linux/0596005652/ch12s01.html) storage driver, extends the NIO storage driver primitive | :heavy_check_mark: | [![](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-fs/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-fs) <br/> [![CI status](https://gitlab.com/emc-mongoose/mongoose-storage-driver-fs/badges/master/pipeline.svg)](https://gitlab.com/emc-mongoose/mongoose-storage-driver-fs/commits/master) <br/> [![](https://img.shields.io/badge/Issue-Tracker-orange.svg)](https://mongoose-issues.atlassian.net/projects/BASE) <br/> [![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose-storage-driver-fs.svg)](https://hub.docker.com/r/emcmongoose/mongoose-storage-driver-fs/)
| [mongoose-storage-driver-**hdfs**](https://github.com/emc-mongoose/mongoose-storage-driver-hdfs) | [Apache HDFS](http://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-hdfs/HdfsDesign.html) storage driver, extends the NIO storage driver primitive | :x: | [![](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-hdfs/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-hdfs) <br/> [![CI status](https://gitlab.com/emc-mongoose/mongoose-storage-driver-hdfs/badges/master/pipeline.svg)](https://gitlab.com/emc-mongoose/mongoose-storage-driver-hdfs/commits/master) <br/> [![](https://img.shields.io/badge/Issue-Tracker-orange.svg)](https://mongoose-issues.atlassian.net/projects/HDFS) <br/> [![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose-storage-driver-hdfs.svg)](https://hub.docker.com/r/emcmongoose/mongoose-storage-driver-hdfs/)
| [mongoose-storage-driver-**atmos**](https://github.com/emc-mongoose/mongoose-storage-driver-atmos) | [Dell EMC Atmos](https://poland.emc.com/collateral/software/data-sheet/h5770-atmos-ds.pdf) storage driver, extends the HTTP storage driver primitive | :heavy_check_mark: | [![](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-atmos/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-atmos) <br/> [![CI status](https://gitlab.com/emc-mongoose/mongoose-storage-driver-atmos/badges/master/pipeline.svg)](https://gitlab.com/emc-mongoose/mongoose-storage-driver-atmos/commits/master) <br/> [![](https://img.shields.io/badge/Issue-Tracker-orange.svg)](https://mongoose-issues.atlassian.net/projects/BASE) <br/> [![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose-storage-driver-atmos.svg)](https://hub.docker.com/r/emcmongoose/mongoose-storage-driver-atmos/)
| [mongoose-storage-driver-**s3**](https://github.com/emc-mongoose/mongoose-storage-driver-s3) | [Amazon S3](https://docs.aws.amazon.com/en_us/AmazonS3/latest/API/Welcome.html) storage driver, extends the HTTP storage driver primitive | :heavy_check_mark: | [![](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-s3/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-s3) <br/> [![CI status](https://gitlab.com/emc-mongoose/mongoose-storage-driver-s3/badges/master/pipeline.svg)](https://gitlab.com/emc-mongoose/mongoose-storage-driver-s3/commits/master) <br/> [![](https://img.shields.io/badge/Issue-Tracker-orange.svg)](https://mongoose-issues.atlassian.net/projects/S3) <br/> [![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose-storage-driver-s3.svg)](https://hub.docker.com/r/emcmongoose/mongoose-storage-driver-s3/)
| [mongoose-storage-driver-**swift**](https://github.com/emc-mongoose/mongoose-storage-driver-swift) | [OpenStack Swift](https://wiki.openstack.org/wiki/Swift) storage driver, extends the HTTP storage driver primitive | :heavy_check_mark: | [![](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-swift/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-swift) <br/> [![CI status](https://gitlab.com/emc-mongoose/mongoose-storage-driver-swift/badges/master/pipeline.svg)](https://gitlab.com/emc-mongoose/mongoose-storage-driver-swift/commits/master) <br/> [![](https://img.shields.io/badge/Issue-Tracker-orange.svg)](https://mongoose-issues.atlassian.net/projects/SWIFT) <br/> [![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose-storage-driver-swift.svg)](https://hub.docker.com/r/emcmongoose/mongoose-storage-driver-swift/)
| [mongoose-storage-driver-**pravega**](https://github.com/emc-mongoose/mongoose-storage-driver-pravega) | [Pravega](http://pravega.io) storage driver, extends the cooperative multitasking storage driver primitive | :x: | [![](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-pravega/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-pravega) <br/> [![CI status](https://gitlab.com/emc-mongoose/mongoose-storage-driver-pravega/badges/master/pipeline.svg)](https://gitlab.com/emc-mongoose/mongoose-storage-driver-pravega/commits/master) <br/> [![](https://img.shields.io/badge/Issue-Tracker-orange.svg)](https://mongoose-issues.atlassian.net/projects/PRAVEGA) <br/> [![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose-storage-driver-pravega.svg)](https://hub.docker.com/r/emcmongoose/mongoose-storage-driver-pravega/)
| [mongoose-storage-driver-**kafka**](https://github.com/emc-mongoose/mongoose-storage-driver-kafka) | [Apache Kafka](https://kafka.apache.org/) storage driver, extends the cooperative multitasking storage driver primitive | :x: | [![](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-kafka/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/github/emc-mongoose/mongoose-storage-driver-kafka) <br/> [![CI status](https://gitlab.com/emc-mongoose/mongoose-storage-driver-kafka/badges/master/pipeline.svg)](https://gitlab.com/emc-mongoose/mongoose-storage-driver-kafka/commits/master) <br/> [![](https://img.shields.io/badge/Issue-Tracker-orange.svg)](https://mongoose-issues.atlassian.net/projects/KAFKA) <br/> [![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose-storage-driver-kafka.svg)](https://hub.docker.com/r/emcmongoose/mongoose-storage-driver-kafka/)
| mongoose-storage-driver-**pulsar** | [Apache Pulsar](https://pulsar.apache.org/) storage driver | :x: | TODO
| mongoose-storage-driver-**zookeeper** | [Apache Zookeeper](https://zookeeper.apache.org/) storage driver | :x: | TODO
| mongoose-storage-driver-**bookkeeper** | [Apache BookKeeper](https://bookkeeper.apache.org/) storage driver | :x: | TODO
| mongoose-storage-driver-**rocksdb** | [RocksDB](https://rocksdb.org/) storage driver | :x: | TODO
| mongoose-storage-driver-**gcs** | [Google Cloud Storage](https://cloud.google.com/storage/docs/json_api/v1/) driver | :x: | TODO
| mongoose-storage-driver-**graphql** | [GraphQL](https://graphql.org/) storage driver | :x: | TODO
| mongoose-storage-driver-**jdbc** | [JDBC](https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/) storage driver | :x: | TODO
