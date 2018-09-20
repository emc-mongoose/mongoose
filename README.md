[![master](https://img.shields.io/travis/emc-mongoose/mongoose/master.svg)](https://travis-ci.org/emcmongoose/mongoose)
[![downloads](https://img.shields.io/github/downloads/emc-mongoose/mongoose/total.svg)](https://github.com/emc-mongoose/mongoose/releases)
[![release](https://img.shields.io/github/release/emc-mongoose/mongoose.svg)]()
[![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose.svg)](https://hub.docker.com/r/emcmongoose/mongoose/)

# Contents

1. [Overview](#1-overview)
2. [Features](#2-features)
    2.1. [Comparison With Similar Tools](#21-comparison-with-similar-tools)
        2.1.1. [General](#211-general)
        2.1.2. [Purpose](#212-purpose)
        2.1.3. [Scalability](#213-scalability)
        2.1.4. [Input](#214-input)
        2.1.5. [Output](#215-output)
        2.1.6. [Load Generation Patterns](#216-load-generation-patterns)
        2.1.7. [Storages Support](#217-storages-support)
    2.2. [Scalability](#22-scalability)<br/>
        2.2.1. [Vertical](#221-vertical)<br/>
        2.2.2. [Horizontal](#222-horizontal)<br/>
    2.3. [Customization](#23-customization)<br/>
        2.3.1. [Flexible Configuration](#231-flexible-configuration)<br/>
        2.3.2. [Load Generation Patterns](#232-load-generation-patterns)<br/>
        2.3.3. [Scenarios](#233-scenarios)<br/>
        2.3.4. [Metrics Reporting](#234-metrics-reporting)<br/>
    2.4. [Extension](#24-extension)<br/>
        2.4.1. [Load Steps](#241-load-steps)<br/>
        2.4.2. [Storage Drivers](#242-storage-drivers)<br/>
        2.4.3. [Scenario Engine](#243-scenario-engine)<br/>
3. Documentation
    3.1. [Deployment](doc/deployment/README.md)
    3.2. [User Guide](doc/user_guide/README.md)
    3.2. [Troubleshooting](doc/troubleshooting/README.md)
    3.3. Storage Drivers
        3.3.1. [General](storage/driver/README.md)
        3.3.2. [S3](storage/driver/coop/net/http/s3/README.md)
        3.3.3. [Atmos](storage/driver/coop/net/http/atmos/README.md)
        3.3.4. [Swift](storage/driver/coop/net/http/swift/README.md)
        3.3.5. [FS](storage/driver/coop/nio/fs/README.md)
    3.4. [Dependencies](doc/dependencies/README.md)
    3.5. [Input](doc/input/README.md)
    3.6. [Output](doc/output/README.md)
    3.7. Design
        3.7.1. [Architecture](doc/design/architecture/README.md)
        3.7.2. [Distributed Mode](doc/design/distributed_mode/README.md)
        3.7.3. [Installer](doc/design/installer/README.md)
        3.7.4. [Recycle Mode](doc/design/recycle_mode/README.md)
        3.7.5. [Data Reentrancy](doc/design/data_reentrancy/README.md)
        3.7.6. [Byte Range Operations](doc/design/byte_range_operations/README.md)
        3.7.7. [Copy Mode](doc/design/copy_mode/README.md)
        3.7.8. [Pipeline Load](load/step/pipeline/README.md)
        3.7.9. [Weighted Load](load/step/weighted/README.md)
    3.8. [Integrations](doc/integrations/README.md)
    3.9. [Contributing](CONTRIBUTING.md)
    3.10. [Changelog](doc/changelog/README.md)

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
|**Vertical** (Max sustained concurrency per instance)|[1_048_576](https://github.com/emc-mongoose/mongoose/blob/feature-v4-doc/doc/features.md#12-fibers)|[1024](http://cosbench.1094679.n5.nabble.com/how-many-connections-users-can-cosbench-create-to-test-one-swift-storage-tp325p326.html)| TBD |[1_000_000](https://locust.io/)|

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
|**[Weighted load](design/weighted_load.md)**|:heavy_check_mark:| :heavy_check_mark:| TBD |:x:|
|**[Pipeline load](design/pipeline_load.md)**|:heavy_check_mark:| :x:| TBD |:x:|
|**[Recycle mode](design/recycle_mode.md)**|:heavy_check_mark: |:x:| TBD |:x:|

### 2.1.7. Storages Support

* **Note**: Locust and LoadRunner are not designed for the storage performance testing explicitly so they are excluded
from the table below

|                                            | Mongoose  | COSBench |
| ---                                        | :---:     | :---:    |
|**Supported storages**                      |<ul><li>Amazon S3</li><li>EMC Atmos</li><li>OpenStack Swift</li><li>Filesystem</li><li>HDFS</li><ul>|<ul><li>Amazon S3</li><li>Amplidata</li><li>OpenStack Swift</li><li>Scality</li><li>Ceph</li><li>Google Cloud Storage</li><li>Aliyun OSS</li><ul>|
|**Extensible to support custom storage API**|  :heavy_check_mark:   | :heavy_check_mark: |


# 2.2. Scalability

### 2.2.1. Vertical

Using [fibers](https://github.com/akurilov/fiber4j) allows to sustain millions of concurrent operations easily without
significant performance degradation.

### 2.2.2. Horizontal

The [distributed mode](design/distributed_mode.md) in Mongoose was designed as P2P network. Each peer/node performs
independently as much as possible. This eliminates the excess network interaction between the nodes which may be a
bottleneck.

## 2.3. Customization

### 2.3.1. Flexible Configuration

Supports the [parameterization](input/configuration.md#2-parameterization) and [extension](api/extensions/general.md)
but remains type-safe and structure-safe

### 2.3.2. Load Generation Patterns

* CRUD operations and the extensions: Noop, [Copy](design/copy_mode.md)

* [Parial Operations](design/byte_range_operations.md)

* [Composite Operations](design/storage_side_concatenation.md)

* Complex Load Steps
    * [Pipeline Load](design/pipeline_load.md)
    * [Weighted Load](design/weighted_load.md)
* [Recycle Mode](design/recycle_mode.md)

* [Data Reentrancy](design/data_reentrancy.md)

  Allows to validate the data read back from the storage successfully even after the data items have been randomly
  updated multiple times before

* Custom Payload Data

### 2.3.3. [Scenarios](input/scenarios.md)

Scenaruis allow to organize the load steps in the required order and reuse the complex performance tests

### 2.3.4. [Metrics Reporting](output/metrics.md)

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

## 2.4. [Extension](api/extensions/general.md)

Mongoose is designed to be agnostic to the particular extensions implementations. This allows to support any storage,
scenario language, different load step kinds.

### 2.4.1. [Load Steps](api/extensions/load_steps.md)

It's possible to implement a custom load step.

### 2.4.2. Storage Drivers

Mongoose accounts and operates with abstract *items* which may be files, objects, directories, tokens, buckets, etc. The
exact behaviour is defined by the particular storage driver implementation.

Mongoose supports some storage types out-of-the-box:
* Amazon S3
* EMC Atmos
* OpenStack Swift
* Filesystem

The following additional storage driver implementations are publicly available:
* [HDFS](https://github.com/emc-mongoose/mongoose-storage-driver-hdfs)

### 2.4.3. Scenario Engine

Any Mongoose scenario may be written using any JSR-223 compliant scripting language. Javascript support is available
out-of-the-box.
