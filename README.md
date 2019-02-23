[![Gitter chat](https://badges.gitter.im/emc-mongoose.png)](https://gitter.im/emc-mongoose)
[![Issue Tracker](https://img.shields.io/badge/Issue-Tracker-red.svg)](https://mongoose-issues.atlassian.net/projects/GOOSE)
[![CI status](https://gitlab.com/emcmongoose/mongoose/badges/master/pipeline.svg)](https://gitlab.com/emcmongoose/mongoose/commits/master)
[![Tag](https://img.shields.io/github/tag/emc-mongoose/mongoose.svg)](https://github.com/emc-mongoose/mongoose/tags)
[![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/github/emc-mongoose/mongoose/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/github/emc-mongoose/mongoose)
[![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/http/oss.sonatype.org/com.github.emc-mongoose/mongoose.svg)](http://oss.sonatype.org/com.github.emc-mongoose/mongoose)
[![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose.svg)](https://hub.docker.com/r/emcmongoose/mongoose/)

# Contents

1. [Overview](#1-overview)
2. [Features](#2-features)<br/>
&nbsp;&nbsp;2.1. [Comparison With Similar Tools](#21-comparison-with-similar-tools)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.1.1. [General](#211-general)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.1.2. [Purpose](#212-purpose)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.1.3. [Scalability](#213-scalability)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.1.4. [Input](#214-input)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.1.5. [Output](#215-output)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.1.6. [Load Generation Patterns](#216-load-generation-patterns)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.1.7. [Storages Support](#217-storages-support)<br/>
&nbsp;&nbsp;2.2. [Scalability](#22-scalability)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.2.1. [Vertical](#221-vertical)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.2.2. [Horizontal](#222-horizontal)<br/>
&nbsp;&nbsp;2.3. [Customization](#23-customization)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.3.1. [Flexible Configuration](#231-flexible-configuration)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.3.2. [Load Generation Patterns](#232-load-generation-patterns)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.3.3. [Scenarios](#233-scenarios)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.3.4. [Metrics Reporting](#234-metrics-reporting)<br/>
&nbsp;&nbsp;2.4. [Extension](#24-extension)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.4.1. [Load Steps](#241-load-steps)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.4.2. [Storage Drivers](#242-storage-drivers)<br/>
&nbsp;&nbsp;&nbsp;2.4.3. [Scenario Engine](#243-scenario-engine)<br/>
3. Documentation<br/>
&nbsp;&nbsp;3.1. [Deployment](doc/deployment)<br/>
&nbsp;&nbsp;3.2. [Usage](doc/usage)<br/>
&nbsp;&nbsp;3.2. [Troubleshooting](doc/troubleshooting)<br/>
&nbsp;&nbsp;3.3. Storage Drivers<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.3.2. [S3](storage/driver/coop/netty/http/s3)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.3.3. [Atmos](storage/driver/coop/netty/http/atmos)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.3.4. [Swift](storage/driver/coop/netty/http/swift)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.3.5. [FS](storage/driver/coop/nio/fs)<br/>
&nbsp;&nbsp;3.4. [Dependencies](doc/dependencies)<br/>
&nbsp;&nbsp;3.5. [Interfaces](doc/interfaces#interfaces)<br/>
&nbsp;&nbsp;&nbsp;3.5.1 [Input](doc/interfaces/input)<br/>
&nbsp;&nbsp;&nbsp;3.5.2 [Output](doc/interfaces/output)<br/>
&nbsp;&nbsp;&nbsp;3.5.3 [Remote API](doc/interfaces/api/remote)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.5.3.1 [Config API](doc/interfaces/api/remote#config)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.5.3.2 [Runs API](doc/interfaces/api/remote#run)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.5.3.3 [Logs API](doc/interfaces/api/remote#logs)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.5.3.4 [Metrics API](doc/interfaces/api/remote#metrics)<br/>
&nbsp;&nbsp;&nbsp;3.5.4 Extentions <br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.5.4.1 [Load Step Types](doc/interfaces/api/extensions/load_step)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.5.4.2 [Storage Drivers](doc/interfaces/api/extensions/storage_driver)<br/>
&nbsp;&nbsp;3.6. Design<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.7.1. [Architecture](doc/design/architecture)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.7.2. [Distributed Mode](doc/design/distributed_mode)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.7.3. [Installer](src/main/java/com/emc/mongoose/base/env)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.7.4. [Recycle Mode](doc/design/recycle_mode)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.7.5. [Data Reentrancy](doc/design/data_reentrancy)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.7.6. [Byte Range Operations](doc/design/byte_range_operations)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.7.7. [Copy Mode](doc/design/copy_mode)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.7.8. [Pipeline Load](load/step/pipeline)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;3.7.9. [Weighted Load](load/step/weighted)<br/>
&nbsp;&nbsp;3.8. [Contributing](CONTRIBUTING.md)<br/>
&nbsp;&nbsp;3.9. [Changelog](doc/changelog)<br/>

# 1. Overview

Mongoose is a powerful storage performance testing tool.

It is designed to be used for:
* [Load Testing](https://en.wikipedia.org/wiki/Load_testing)
* [Stress Testing](https://en.wikipedia.org/wiki/Stress_testing)
* [Soak/Longevity/Endurance Testing](https://en.wikipedia.org/wiki/Soak_testing)
* [Volume Testing](https://en.wikipedia.org/wiki/Volume_testing)
* [Smoke](https://en.wikipedia.org/wiki/Smoke_testing_(software))/[Sanity](https://en.wikipedia.org/wiki/Sanity_check)
  Testing

# 2. Features

## 2.1. Comparison With Similar Tools

* [COSBench](https://github.com/intel-cloud/cosbench)
* [LoadRunner](https://software.microfocus.com/en-us/products/loadrunner-load-testing/overview)
* [Locust](https://locust.io/)

### 2.1.1. General
|                   | Mongoose  | COSBench | LoadRunner         | Locust |
| ---               | :---:     | :---:    | :---:              | :---:  |
|**License**        |[MIT License](../LICENSE)|[Apache 2.0](https://github.com/intel-cloud/cosbench/blob/master/LICENSE)|[Proprietary](https://en.wikipedia.org/wiki/LoadRunner)|[MIT License](https://github.com/locustio/locust/blob/master/LICENSE)|
|**Open Source**    |:heavy_check_mark:|:heavy_check_mark:    |:x:|  :heavy_check_mark:|

### 2.1.2. Purpose
|                   | Mongoose  | COSBench | LoadRunner | Locust |
| ---               | :---:     | :---:    | :---:      | :---:  |
|**[Load testing](https://en.wikipedia.org/wiki/Load_testing)** |:heavy_check_mark:|:heavy_check_mark:|:heavy_check_mark:|:heavy_check_mark:|
|**[Stress testing](https://en.wikipedia.org/wiki/Stress_testing)** |:heavy_check_mark:|:heavy_check_mark:|:heavy_check_mark:| TBD |
|**[Endurance testing](https://en.wikipedia.org/wiki/Soak_testing)**|:heavy_check_mark:| TBD |:heavy_check_mark:| TBD |

### 2.1.3. Scalability
|                                                    | Mongoose  | COSBench | LoadRunner | Locust |
| ---                                                | :---:     | :---:    | :---:      | :---:  |
|**Horizontal** (Distributed Mode)                 |:heavy_check_mark:|:heavy_check_mark:| TBD |:heavy_check_mark:|
|**Vertical** (Max sustained concurrency per instance)| 1_048_576 |[1024](http://cosbench.1094679.n5.nabble.com/how-many-connections-users-can-cosbench-create-to-test-one-swift-storage-tp325p326.html)| TBD |[1_000_000](https://locust.io/)|

### 2.1.4. Input
|                  | Mongoose  | COSBench | LoadRunner | Locust |
| ---              | :---:     | :---:    | :---:      | :---:  |
|**GUI**           |:x:|:heavy_check_mark:|:heavy_check_mark:|:heavy_check_mark:|
|**Parameterization**|:heavy_check_mark:| :heavy_check_mark: | TBD |:heavy_check_mark:(need to extend the functionality)|
|**Script language**| Any [JSR-223](https://en.wikipedia.org/wiki/Scripting_for_the_Java_Platform) compatible |[XML](https://en.wikipedia.org/wiki/XML)|[ANSI C, Java, .Net, JS](https://en.wikipedia.org/wiki/LoadRunner)|[Python](https://en.wikipedia.org/wiki/Python)|

### 2.1.5. Output
|                                        | Mongoose  | COSBench | LoadRunner | Locust |
| ---                                    | :---:     | :---:    | :---:      | :---:  |
|**Highest-resolution (per each op) metrics**|:heavy_check_mark:|:x:| TBD |:x:|
|**Saturation concurrency measurement**  |:heavy_check_mark:|:x:| TBD |:x:|

### 2.1.6. Load Generation Patterns
|                       | Mongoose  | COSBench | LoadRunner | Locust |
| ---                   | :---:     | :---:    | :---:      | :---:  |
|**[Weighted load](load/step/weighted)**|:heavy_check_mark:| :heavy_check_mark:| TBD |:x:|
|**[Pipeline load](load/step/pipeline)**|:heavy_check_mark:| :x:| TBD |:x:|
|**[Recycle mode](doc/design/recycle_mode)**|:heavy_check_mark: |:x:| TBD |:x:|

### 2.1.7. Storages Support

* **Note**: Locust and LoadRunner are not designed for the storage performance testing explicitly so they are excluded
from the table below

|                                            | Mongoose  | COSBench |
| ---                                        | :---:     | :---:    |
|**Supported storages**                      |<ul><li>Amazon S3</li><li>EMC Atmos</li><li>OpenStack Swift</li><li>Filesystem</li><li>HDFS</li><ul>|<ul><li>Amazon S3</li><li>Amplidata</li><li>OpenStack Swift</li><li>Scality</li><li>Ceph</li><li>Google Cloud Storage</li><li>Aliyun OSS</li><ul>|
|**Extensible to support custom storage API**|  :heavy_check_mark:   | :heavy_check_mark: |


## 2.2. Scalability

### 2.2.1. Vertical

Using [fibers](https://github.com/akurilov/fiber4j) allows to sustain millions of concurrent operations easily without
significant performance degradation.

### 2.2.2. Horizontal

The [distributed mode](doc/design/distributed_mode) in Mongoose was designed as P2P network. Each peer/node performs
independently as much as possible. This eliminates the excess network interaction between the nodes which may be a
bottleneck.

## 2.3. Customization

### 2.3.1. Flexible Configuration

Supports the [parameterization](doc/interfaces/input/configuration#2-parameterization) and extension but remains type-safe and
structure-safe.

### 2.3.2. Load Generation Patterns

* CRUD operations and the extensions: Noop, [Copy](doc/design/copy_mode), etc

* [Parial Operations](doc/usage/load/operations/byte_ranges)

* [Composite Operations](doc/usage/load/operations/composite)

* Complex Load Steps
    * [Pipeline Load](load/step/pipeline)
    * [Weighted Load](load/step/weighted)
* [Recycle Mode](doc/design/recycle_mode)

* [Data Reentrancy](doc/design/data_reentrancy)

  Allows to validate the data read back from the storage successfully even after the data items have been randomly
  updated multiple times before

* Custom Payload Data

### 2.3.3. [Scenarios](doc/interfaces/input/scenarios)

Scenaruis allow to organize the load steps in the required order and reuse the complex performance tests

### 2.3.4. [Metrics Reporting](doc/interfaces/output#2-metrics)

The metrics reported by Mongoose are designed to be most useful for the performance analysis. The following metrics are
available:

* Counts

  * Items
  * Bytes transferred
  * Time
    * Effective
    * Elapsed

* Rates

  * Items per second
  * Bytes per second

* Timing distributions for:

  * Operation durations
  * Network latencies

* Actual concurrency

  It's possible to limit the rate and measure the sustained actual concurrency

The *average* metrics output is being done periodically while a load step is running. The *summary* metrics output is
done once when a load step is finished. Also, it's possible to obtain the highest precision metrics (for each operation,
so called *I/O trace* records).

## 2.4. [Extension](src/main/java/com/emc/mongoose/base/env)

Mongoose is designed to be agnostic to the particular extensions implementations. This allows to support any storage,
scenario language, different load step kinds.

### 2.4.1. Load Steps

A load step controls the load operations flow. Different load step implementations may do it in the different way. There
are the available out-of-the-box:

* [Linear](load/step/linear)
  The most basic load step. All load operations have the same type.

* [Pipeline](load/step/pipeline)
  Executes a sequence of the different load operations for each item independently.

* [Weighted](load/step/weighted)
  Executes the load operations of different types sustaining the specified ratio (weights).

### 2.4.2. Storage Drivers

The actual load is being executed by the storage drivers. Mongoose supports some storage types out-of-the-box:
* [Amazon S3](storage/driver/coop/netty/http/s3)
* [EMC Atmos](storage/driver/coop/netty/http/atmos)
* [OpenStack Swift](storage/driver/coop/netty/http/swift)
* [Filesystem](storage/driver/coop/nio/fs)

The following additional storage driver implementations are available:
* [HDFS](https://github.com/emc-mongoose/mongoose-storage-driver-hdfs)

### 2.4.3. Scenario Engine

Any Mongoose scenario may be written using any JSR-223 compliant scripting language. Javascript support is available
out-of-the-box.
